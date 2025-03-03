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
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.TagTechnology
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import org.eclipse.keyple.core.plugin.CardIOException
import org.eclipse.keyple.core.plugin.CardInsertionWaiterAsynchronousApi
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterAsynchronousSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterBlockingSpi
import org.eclipse.keyple.core.plugin.storagecard.internal.CommandProcessorApi
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

  private val logger = LoggerFactory.getLogger(this::class.java)

  private val nfcAdapter: NfcAdapter = NfcAdapter.getDefaultAdapter(config.activity)
  private val options: Bundle
  private val handler = Handler(Looper.getMainLooper())
  private val syncWaitRemoval = Object()
  private val apduInterpreter: ApduInterpreterSpi?

  private var flags: Int
  private var tagTechnology: TagTechnology? = null
  private var isCardChannelOpen: Boolean = false
  private var isWaitingForCardRemoval = false

  private lateinit var cardInsertionWaiterAsynchronousApi: CardInsertionWaiterAsynchronousApi
  private lateinit var currentCardProtocol: String
  private lateinit var uid: ByteArray
  private lateinit var powerOnData: String

  companion object {
    const val MIFARE_ULTRALIGHT_READ_SIZE = 16
  }

  init {
    flags =
        (if (config.skipNdefCheck) NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK else 0) or
            (if (!config.isPlatformSoundEnabled) NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS else 0)
    options =
        Bundle().apply {
          if (config.cardInsertionPollingInterval > 0) {
            putInt(
                NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, config.cardInsertionPollingInterval)
          }
        }
    apduInterpreter =
        config.apduInterpreterFactory?.let {
          require(it is ApduInterpreterFactorySpi) {
            "The provided ApduInterpreterFactory is not an instance of ApduInterpreterFactorySpi"
          }
          it.createApduInterpreter()
        }
    apduInterpreter?.setCommandProcessor(this)
    logger.info("{}: config initialized: {}", name, config)
  }

  override fun getName(): String = AndroidNfcConstants.READER_NAME

  override fun openPhysicalChannel() {
    if (tagTechnology!!.isConnected) {
      logger.info("{}: card already connected", name)
      return
    }
    try {
      tagTechnology!!.connect()
      isCardChannelOpen = true
    } catch (e: Exception) {
      throw CardIOException("Error while opening physical channel", e)
    }
  }

  override fun closePhysicalChannel() {
    isCardChannelOpen = false
  }

  override fun isPhysicalChannelOpen(): Boolean {
    return isCardChannelOpen
  }

  override fun checkCardPresence(): Boolean {
    throw UnsupportedOperationException("checkCardPresence() is not supported")
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
      throw CardIOException("Error while transmitting APDU: ${e.message}", e)
    }
  }

  override fun isContactless(): Boolean = true

  override fun onUnregister() {
    // NOP
  }

  override fun isProtocolSupported(readerProtocol: String): Boolean =
      AndroidNfcSupportedProtocols.values().any { it.name == readerProtocol }

  override fun activateProtocol(readerProtocol: String) {
    flags =
        flags or
            when (AndroidNfcSupportedProtocols.valueOf(readerProtocol)) {
              AndroidNfcSupportedProtocols.ISO_14443_4 ->
                  NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_A
              AndroidNfcSupportedProtocols.NXP_MIFARE_ULTRA_LIGHT -> NfcAdapter.FLAG_READER_NFC_A
            }
  }

  override fun deactivateProtocol(readerProtocol: String) {
    flags =
        flags and
            when (AndroidNfcSupportedProtocols.valueOf(readerProtocol)) {
              AndroidNfcSupportedProtocols.ISO_14443_4 ->
                  (NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_A).inv()
              AndroidNfcSupportedProtocols.NXP_MIFARE_ULTRA_LIGHT ->
                  NfcAdapter.FLAG_READER_NFC_A.inv()
            }
  }

  override fun isCurrentProtocol(readerProtocol: String): Boolean =
      AndroidNfcSupportedProtocols.valueOf(readerProtocol).androidNfcTechIdentifier ==
          currentCardProtocol

  override fun onStartDetection() {
    logger.info("{}: start card detection", name)
    try {
      nfcAdapter.enableReaderMode(config.activity, this, flags, options)
    } catch (e: Exception) {
      throw ReaderIOException("Failed to start NFC detection", e)
    }
  }

  override fun onStopDetection() {
    logger.info("{}: stop card detection", name)
    try {
      nfcAdapter.disableReaderMode(config.activity)
    } catch (e: Exception) {
      throw ReaderIOException("Failed to stop NFC detection", e)
    }
  }

  override fun setCallback(callback: CardInsertionWaiterAsynchronousApi) {
    this.cardInsertionWaiterAsynchronousApi = callback
  }

  override fun waitForCardRemoval() {
    if (!isWaitingForCardRemoval) {
      if (logger.isDebugEnabled) {
        logger.debug("{}: waiting for card removal", name)
      }
      isWaitingForCardRemoval = true
      handler.post(tagPresenceChecker)
      synchronized(syncWaitRemoval) { syncWaitRemoval.wait() }
      isWaitingForCardRemoval = false
    }
  }

  override fun stopWaitForCardRemoval() {
    isWaitingForCardRemoval = false
    handler.removeCallbacks(tagPresenceChecker)
    synchronized(syncWaitRemoval) { syncWaitRemoval.notify() }
  }

  private val tagPresenceChecker: Runnable by lazy {
    Runnable {
      if (!isTagPresent()) {
        synchronized(syncWaitRemoval) { syncWaitRemoval.notify() }
        return@Runnable
      }
      if (isWaitingForCardRemoval) {
        handler.postDelayed(tagPresenceChecker, config.cardRemovalPollingInterval.toLong())
      }
    }
  }

  private fun isTagPresent(): Boolean {
    return try {
      tagTechnology?.isConnected == true
    } catch (_: Exception) {
      if (logger.isDebugEnabled) {
        logger.debug("{}: card removed", name)
      }
      false
    }
  }

  override fun transmitIsoApdu(apdu: ByteArray): ByteArray {
    return (tagTechnology as IsoDep).transceive(apdu)
  }

  override fun getUID(): ByteArray {
    return uid
  }

  override fun readBlock(blockNumber: Int, length: Int): ByteArray {
    require(length % MifareUltralight.PAGE_SIZE == 0) {
      "Requested length ($length) must be a multiple of PAGE_SIZE (${MifareUltralight.PAGE_SIZE})."
    }
    require(blockNumber >= 0) { "Block number must be non-negative." }
    require(length <= MIFARE_ULTRALIGHT_READ_SIZE) {
      "Requested length ($length) exceeds maximum readable size 16 in a single operation."
    }
    val ultralight = tagTechnology as MifareUltralight
    val readData = ultralight.readPages(blockNumber)
    return if (length < MIFARE_ULTRALIGHT_READ_SIZE) {
      readData.copyOf(length)
    } else {
      readData
    }
  }

  override fun writeBlock(blockNumber: Int, data: ByteArray?) {
    (tagTechnology as MifareUltralight).writePage(blockNumber, data)
  }

  override fun onTagDiscovered(tag: Tag) {
    logger.info("{}: card discovered: {}", name, tag)
    isCardChannelOpen = false
    try {
      for (technology in tag.techList) when (technology) {
        IsoDep::class.qualifiedName -> {
          currentCardProtocol = IsoDep::class.qualifiedName!!
          tagTechnology = IsoDep.get(tag)
        }
        MifareUltralight::class.qualifiedName -> {
          currentCardProtocol = MifareUltralight::class.qualifiedName!!
          tagTechnology = MifareUltralight.get(tag)
        }
        NfcA::class.qualifiedName -> {
          val tagA = NfcA.get(tag)
          uid = tagA.tag.id
          powerOnData =
              JSONObject()
                  .put("type", "A")
                  .put("uid", HexUtil.toHex(uid))
                  .put("atqa", HexUtil.toHex(tagA.atqa))
                  .put("sak", HexUtil.toHex(tagA.sak))
                  .toString()
        }
        NfcB::class.qualifiedName -> {
          val tagB = NfcB.get(tag)
          uid = tagB.tag.id
          powerOnData =
              JSONObject()
                  .put("type", "B")
                  .put("uid", HexUtil.toHex(uid))
                  .put("applicationData", HexUtil.toHex(tagB.applicationData))
                  .put("protocolInfo", HexUtil.toHex(tagB.protocolInfo))
                  .toString()
        }
        else -> logger.warn("{}: unreachable code", name)
      }
      cardInsertionWaiterAsynchronousApi.onCardInserted()
    } catch (_: NoSuchElementException) {
      tagTechnology = null
      logger.warn("{}: unsupported card technology", name)
    }
  }
}
