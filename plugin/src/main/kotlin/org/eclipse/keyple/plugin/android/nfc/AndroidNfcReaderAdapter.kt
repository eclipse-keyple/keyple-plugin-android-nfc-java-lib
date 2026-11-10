/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.android.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.TagTechnology
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.eclipse.keyple.core.plugin.CardIOException
import org.eclipse.keyple.core.plugin.CardInsertionWaiterAsynchronousApi
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterAsynchronousSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterBlockingSpi
import org.eclipse.keyple.core.plugin.storagecard.internal.CommandProcessorApi
import org.eclipse.keyple.core.plugin.storagecard.internal.KeyStorageType
import org.eclipse.keyple.core.plugin.storagecard.internal.spi.ApduInterpreterFactorySpi
import org.eclipse.keyple.core.plugin.storagecard.internal.spi.ApduInterpreterSpi
import org.eclipse.keyple.core.util.HexUtil
import org.json.JSONObject
import org.slf4j.LoggerFactory

internal class AndroidNfcReaderAdapter(private val config: AndroidNfcConfig) :
    AndroidNfcReader,
    ConfigurableReaderSpi,
    ObservableReaderSpi,
    CardInsertionWaiterAsynchronousSpi,
    CardRemovalWaiterBlockingSpi,
    CommandProcessorApi,
    NfcAdapter.ReaderCallback {

  private companion object {
    private val logger = LoggerFactory.getLogger(AndroidNfcReaderAdapter::class.java)
    private const val MIFARE_KEY_A = 0x60
    private const val MIFARE_KEY_B = 0x61
  }

  // ── NFC infrastructure ────────────────────────────────────────────────────────────────────────
  private val nfcAdapter: NfcAdapter =
      NfcAdapter.getDefaultAdapter(config.activity)
          ?: throw IllegalStateException(
              "NFC is not available on this device or is disabled in system settings"
          )
  private val readerModeOptions: Bundle =
      Bundle().apply {
        if (config.cardInsertionPollingInterval > 0) {
          putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, config.cardInsertionPollingInterval)
        }
      }
  private val mainThreadHandler = Handler(Looper.getMainLooper())

  // ── Protocol configuration ────────────────────────────────────────────────────────────────────
  // NFC_A is shared by ISO_14443_4 and all MIFARE variants. Tracking active protocols as a set
  // and computing flags on demand (rather than incrementally OR/AND-NOT-ing) ensures that
  // deactivating one protocol never removes a technology bit still needed by another.
  private val baseFlags: Int = // derived from config (SKIP_NDEF, NO_PLATFORM_SOUNDS); never changes
      (if (config.skipNdefCheck) NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK else 0) or
          (if (!config.isPlatformSoundEnabled) NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS else 0)
  private val activeProtocols = mutableSetOf<AndroidNfcSupportedProtocols>()
  private val readerModeFlags: Int
    get() {
      var techFlags = 0
      for (protocol in activeProtocols) {
        techFlags =
            techFlags or
                when (protocol) {
                  AndroidNfcSupportedProtocols.ISO_14443_4 ->
                      NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_A
                  AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT,
                  AndroidNfcSupportedProtocols.MIFARE_CLASSIC_1K,
                  AndroidNfcSupportedProtocols.MIFARE_CLASSIC_4K -> NfcAdapter.FLAG_READER_NFC_A
                }
      }
      return baseFlags or techFlags
    }

  // ── Monitoring lifecycle ──────────────────────────────────────────────────────────────────────
  private var isMonitoringActive = false
  private var isWaitingForCardRemoval = false
  private val removalLock = ReentrantLock()
  private val removalCondition = removalLock.newCondition()
  private lateinit var insertionCallback: CardInsertionWaiterAsynchronousApi

  // ── Current tag state (reset at each detection cycle start) ──────────────────────────────────
  @Volatile private var tagTechnology: TagTechnology? = null
  private var currentTagTechId: String = "" // qualified class name of the active Android NFC tech
  private var tagUid: ByteArray = ByteArray(0)
  private var powerOnData: String = ""

  // ── Storage card support (optional, MIFARE) ───────────────────────────────────────────────────
  private val apduInterpreter: ApduInterpreterSpi?
  private var loadedKey: ByteArray? = null

  init {
    apduInterpreter =
        config.apduInterpreterFactory?.let {
          require(it is ApduInterpreterFactorySpi) {
            "The provided ApduInterpreterFactory is not an instance of ApduInterpreterFactorySpi"
          }
          it.createApduInterpreter()
        }
    apduInterpreter?.setCommandProcessor(this)
    if (logger.isDebugEnabled) {
      logger.debug("Reader initialized [config={}]", config)
    }
  }

  override fun getName(): String = AndroidNfcConstants.READER_NAME

  override fun isCardPresent(): Boolean {
    check(isMonitoringActive) { "Call to isCardPresent not allowed outside monitoring" }
    if (tagTechnology == null) return false
    val present = isTagPresent()
    if (!present) {
      tagTechnology = null
    }
    return present
  }

  override fun deselectCard() {
    // No-op: Android NFC deselection is managed by the NFC subsystem.
    // Closing tagTechnology here would break removal detection in waitForCardRemoval(),
    // which relies on isConnected to detect physical tag removal.
  }

  override fun getPowerOnData() = powerOnData

  override fun transmitApdu(apduIn: ByteArray): ByteArray {
    try {
      return if (apduInterpreter == null) {
        (tagTechnology as IsoDep).transceive(apduIn)
      } else {
        apduInterpreter.processApdu(apduIn)
      }
    } catch (e: Exception) {
      throw CardIOException("Failed to transmit APDU", e)
    }
  }

  override fun isContactless(): Boolean = true

  override fun onUnregister() {
    // NOP
  }

  override fun isProtocolSupported(readerProtocol: String): Boolean =
      AndroidNfcSupportedProtocols.values().any { it.name == readerProtocol }

  override fun activateProtocol(readerProtocol: String) {
    activeProtocols.add(AndroidNfcSupportedProtocols.valueOf(readerProtocol))
  }

  override fun deactivateProtocol(readerProtocol: String) {
    activeProtocols.remove(AndroidNfcSupportedProtocols.valueOf(readerProtocol))
  }

  override fun isCurrentProtocol(readerProtocol: String): Boolean {
    val protocol = AndroidNfcSupportedProtocols.valueOf(readerProtocol)

    // Check if the technology identifier matches
    if (protocol.androidNfcTechIdentifier != currentTagTechId) {
      return false
    }

    // For MIFARE Classic, check the actual card size to distinguish between 1K and 4K
    if (
        protocol == AndroidNfcSupportedProtocols.MIFARE_CLASSIC_1K ||
            protocol == AndroidNfcSupportedProtocols.MIFARE_CLASSIC_4K
    ) {
      val mifareClassic = tagTechnology as? MifareClassic ?: return false
      return when (protocol) {
        AndroidNfcSupportedProtocols.MIFARE_CLASSIC_1K ->
            mifareClassic.size == MifareClassic.SIZE_1K
        else -> mifareClassic.size == MifareClassic.SIZE_4K
      }
    }

    return true
  }

  override fun onStartDetection() {
    if (logger.isDebugEnabled) {
      logger.debug("Starting card detection [flags=0x{}]", Integer.toHexString(readerModeFlags))
    }
    // Reset per-card state: these fields belong to the connected tag and must be empty
    // before a new card is tapped so that getPowerOnData() / getUID() are never stale.
    powerOnData = ""
    tagUid = ByteArray(0)
    try {
      nfcAdapter.enableReaderMode(config.activity, this, readerModeFlags, readerModeOptions)
      isMonitoringActive = true
    } catch (e: Exception) {
      throw ReaderIOException("Failed to start card detection", e)
    }
  }

  override fun onStopDetection() {
    if (logger.isDebugEnabled) {
      logger.debug("Stopping card detection")
    }
    // Null out the tag reference *before* disableReaderMode() so that any concurrently-running
    // tagPresenceChecker on the main thread sees null and short-circuits without calling
    // isConnected() on the now-invalidated tag (which would throw SecurityException).
    tagTechnology = null
    isMonitoringActive = false
    try {
      nfcAdapter.disableReaderMode(config.activity)
    } catch (e: Exception) {
      throw ReaderIOException("Failed to stop card detection", e)
    }
  }

  override fun setCallback(callback: CardInsertionWaiterAsynchronousApi) {
    insertionCallback = callback
  }

  override fun waitForCardRemoval() {
    if (!isWaitingForCardRemoval) {
      if (logger.isDebugEnabled) {
        logger.debug("Polling for card removal (interval={}ms)", config.cardRemovalPollingInterval)
      }
      isWaitingForCardRemoval = true
      mainThreadHandler.post(tagPresenceChecker)
      removalLock.withLock { removalCondition.await() }
      isWaitingForCardRemoval = false
    }
  }

  override fun stopWaitForCardRemoval() {
    if (logger.isDebugEnabled) {
      logger.debug("Removal wait stopped")
    }
    isWaitingForCardRemoval = false
    mainThreadHandler.removeCallbacks(tagPresenceChecker)
    removalLock.withLock { removalCondition.signal() }
  }

  private val tagPresenceChecker: Runnable by lazy {
    Runnable {
      if (!isTagPresent()) {
        logger.info("Card removed")
        removalLock.withLock { removalCondition.signal() }
        return@Runnable
      }
      if (isWaitingForCardRemoval) {
        mainThreadHandler.postDelayed(
            tagPresenceChecker,
            config.cardRemovalPollingInterval.toLong(),
        )
      }
    }
  }

  private fun isTagPresent(): Boolean =
      try {
        tagTechnology?.isConnected == true
      } catch (_: SecurityException) {
        // Defensive catch: should not occur because onStopDetection() nulls tagTechnology before
        // calling disableReaderMode(), but retained as a safety net for any residual race.
        logger.warn("Unexpected SecurityException in isTagPresent — treating tag as removed")
        false
      }

  override fun transmitIsoApdu(apdu: ByteArray): ByteArray {
    return (tagTechnology as IsoDep).transceive(apdu)
  }

  override fun getUID(): ByteArray {
    return tagUid
  }

  override fun readBlock(blockAddress: Int, length: Int): ByteArray {
    return when (val tech = tagTechnology) {
      is MifareClassic -> adjustBufferLength(tech.readBlock(blockAddress), length)
      is MifareUltralight -> adjustBufferLength(tech.readPages(blockAddress), length)
      else ->
          throw UnsupportedOperationException(
              "Unsupported tag technology: ${tech?.let { it::class.java.simpleName } ?: "null"}"
          )
    }
  }

  private fun adjustBufferLength(data: ByteArray, expectedLength: Int): ByteArray {
    return if (expectedLength < data.size) {
      data.copyOf(expectedLength)
    } else {
      data
    }
  }

  override fun writeBlock(blockAddress: Int, data: ByteArray) {
    when (val tech = tagTechnology) {
      is MifareClassic -> tech.writeBlock(blockAddress, data)
      is MifareUltralight -> tech.writePage(blockAddress, data)
      else ->
          throw UnsupportedOperationException(
              "Unsupported tag technology: ${tech?.let { it::class.java.simpleName } ?: "null"}"
          )
    }
  }

  override fun loadKey(keyStorageType: KeyStorageType, keyNumber: Int, key: ByteArray) {
    loadedKey = key.copyOf()
  }

  override fun generalAuthenticate(blockAddress: Int, keyType: Int, keyNumber: Int): Boolean {
    val mifareClassic =
        tagTechnology as? MifareClassic
            ?: throw CardIOException("General Authenticate is only supported for Mifare Classic")

    val key = loadedKey
    loadedKey = null

    val usedKey =
        key
            ?: checkNotNull(config.keyProvider) { "No key loaded and no key provider available" }
                .getKey(keyNumber)

    val sectorIndex = mifareClassic.blockToSector(blockAddress)

    return when (keyType) {
      MIFARE_KEY_A -> mifareClassic.authenticateSectorWithKeyA(sectorIndex, usedKey)
      MIFARE_KEY_B -> mifareClassic.authenticateSectorWithKeyB(sectorIndex, usedKey)
      else -> throw IllegalArgumentException("Unsupported key type: ${HexUtil.toHex(keyType)}h")
    }
  }

  override fun onTagDiscovered(tag: Tag) {
    loadedKey = null
    try {
      for (technology in tag.techList) when (technology) {
        IsoDep::class.qualifiedName -> {
          currentTagTechId = IsoDep::class.qualifiedName!!
          tagTechnology = IsoDep.get(tag)
        }
        MifareUltralight::class.qualifiedName -> {
          currentTagTechId = MifareUltralight::class.qualifiedName!!
          tagTechnology = MifareUltralight.get(tag)
        }
        MifareClassic::class.qualifiedName -> {
          currentTagTechId = MifareClassic::class.qualifiedName!!
          tagTechnology = MifareClassic.get(tag)
        }
        NfcA::class.qualifiedName -> {
          val tagA = NfcA.get(tag)
          tagUid = tagA.tag.id
          @Suppress("SpellCheckingInspection")
          powerOnData =
              JSONObject()
                  .put("type", "A")
                  .put("uid", HexUtil.toHex(tagUid))
                  .put("atqa", HexUtil.toHex(tagA.atqa))
                  .put("sak", HexUtil.toHex(tagA.sak))
                  .toString()
        }
        NfcB::class.qualifiedName -> {
          val tagB = NfcB.get(tag)
          tagUid = tagB.tag.id
          powerOnData =
              JSONObject()
                  .put("type", "B")
                  .put("uid", HexUtil.toHex(tagUid))
                  .put("applicationData", HexUtil.toHex(tagB.applicationData))
                  .put("protocolInfo", HexUtil.toHex(tagB.protocolInfo))
                  .toString()
        }
        else -> {
          // Ignored: other technologies in the tag's techList (e.g. Ndef, NfcV) are not supported
        }
      }
      val selectedTech = tagTechnology?.let { it::class.java.simpleName } ?: "none"
      val uidHex = if (tagUid.isNotEmpty()) HexUtil.toHex(tagUid) else "n/a"
      logger.info("Card detected [tech={}, uid={}]", selectedTech, uidHex)
      if (logger.isDebugEnabled) {
        logger.debug("Tag techs: {}", tag.techList.joinToString { it.substringAfterLast('.') })
      }
      tagTechnology!!.connect()
      insertionCallback.onCardInserted()
    } catch (e: Exception) {
      tagTechnology = null
      logger.warn(
          "Failed to connect to tag [reason={}, type={}]",
          e.message,
          e::class.java.simpleName,
      )
    }
  }
}
