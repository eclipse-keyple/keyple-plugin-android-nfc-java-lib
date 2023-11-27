/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://calypsonet.org/
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

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.TagTechnology
import java.io.IOException
import org.eclipse.keyple.core.util.HexUtil
import org.slf4j.LoggerFactory

/**
 * Proxy Tag for [IsoDep], [MifareClassic], [MifareUltralight] Invoke getTagTransceiver factory
 * method to get a TagProxy object from a @ [Tag] object
 *
 * @since 0.9
 */
internal class TagProxy
private constructor(private val tagTechnology: TagTechnology, val tech: String) : TagTechnology {

  /**
   * Retrieve Answer to reset from Tag. For Isodep, getHiLayerResponse and getHiLayerResponse are
   * used to retrieve ATR. For Mifare (Classic and UL) Smartcard, a virtual ATR is returned inspired
   * by PS/SC standard 3B8F8001804F0CA000000306030001000000006A for Mifare Classic
   * 3B8F8001804F0CA0000003060300030000000068 for Mifare Ultralight
   *
   * @return a non null ByteArray
   *
   * @since 2.0.0
   */
  val atr: ByteArray
    @Throws(IOException::class, NoSuchElementException::class)
    get() =
        when (tech) {
          AndroidNfcSupportedProtocols.MIFARE_CLASSIC.androidNfcTechIdentifier ->
              HexUtil.toByteArray("3B8F8001804F0CA000000306030001000000006A")
          AndroidNfcSupportedProtocols.MIFARE_ULTRA_LIGHT.androidNfcTechIdentifier ->
              HexUtil.toByteArray("3B8F8001804F0CA0000003060300030000000068")
          AndroidNfcSupportedProtocols.ISO_14443_4.androidNfcTechIdentifier ->
              if ((tagTechnology as IsoDep).hiLayerResponse != null) tagTechnology.hiLayerResponse
              else tagTechnology.historicalBytes
          else -> throw NoSuchElementException("Protocol $tech not found in plugin's settings.")
        }

  /**
   * Transceive APDUs to correct using correct Android nfc tech identifier
   *
   * @since 2.0.0
   */
  @Throws(IOException::class, NoSuchElementException::class)
  fun transceive(data: ByteArray): ByteArray {
    return when (tech) {
      AndroidNfcSupportedProtocols.MIFARE_CLASSIC.androidNfcTechIdentifier ->
          (tagTechnology as MifareClassic).transceive(data)
      AndroidNfcSupportedProtocols.MIFARE_ULTRA_LIGHT.androidNfcTechIdentifier ->
          (tagTechnology as MifareUltralight).transceive(data)
      AndroidNfcSupportedProtocols.ISO_14443_4.androidNfcTechIdentifier ->
          (tagTechnology as IsoDep).transceive(data)
      else -> throw NoSuchElementException("Protocol $tech not found in plugin's settings.")
    }
  }

  /** {@inheritDoc} */
  override fun getTag(): Tag {
    return tagTechnology.tag
  }

  /** {@inheritDoc} */
  @Throws(IOException::class)
  override fun connect() {
    tagTechnology.connect()
  }

  /** {@inheritDoc} */
  @Throws(IOException::class)
  override fun close() {
    tagTechnology.close()
  }

  /** {@inheritDoc} */
  override fun isConnected(): Boolean {
    return tagTechnology.isConnected
  }

  companion object {

    private val logger =
        LoggerFactory.getLogger(AbstractAndroidNfcReaderAdapter.Companion::class.java)

    /**
     * Create a TagProxy based on a [Tag]
     *
     * @param tag : tag to be proxied
     * @return tagProxy
     *
     * @since 0.9
     */
    @Throws(NoSuchElementException::class)
    fun getTagProxy(tag: Tag): TagProxy {

      logger.info("Matching Tag Type : $tag")

      return when (tag.techList.first {
        it == AndroidNfcSupportedProtocols.ISO_14443_4.androidNfcTechIdentifier ||
            it == AndroidNfcSupportedProtocols.MIFARE_CLASSIC.androidNfcTechIdentifier ||
            it == AndroidNfcSupportedProtocols.MIFARE_ULTRA_LIGHT.androidNfcTechIdentifier
      }) {
        AndroidNfcSupportedProtocols.ISO_14443_4.androidNfcTechIdentifier -> {
          logger.debug("Tag embedded into IsoDep")
          TagProxy(
              IsoDep.get(tag), AndroidNfcSupportedProtocols.ISO_14443_4.androidNfcTechIdentifier)
        }
        AndroidNfcSupportedProtocols.MIFARE_CLASSIC.androidNfcTechIdentifier -> {
          logger.debug("Tag embedded into MifareClassic")
          TagProxy(
              MifareClassic.get(tag),
              AndroidNfcSupportedProtocols.MIFARE_CLASSIC.androidNfcTechIdentifier)
        }
        AndroidNfcSupportedProtocols.MIFARE_ULTRA_LIGHT.androidNfcTechIdentifier -> {
          logger.debug("Tag embedded into MifareUltralight")
          TagProxy(
              MifareUltralight.get(tag),
              AndroidNfcSupportedProtocols.MIFARE_ULTRA_LIGHT.androidNfcTechIdentifier)
        }
        else ->
            throw NoSuchElementException(
                "If received tech identifier does match a tech we can't handle.")
      }
    }
  }
}
