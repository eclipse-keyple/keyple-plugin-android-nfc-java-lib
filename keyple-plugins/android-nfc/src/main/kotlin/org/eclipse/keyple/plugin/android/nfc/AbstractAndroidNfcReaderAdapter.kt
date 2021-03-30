/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
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

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import java.io.IOException
import java.lang.ref.WeakReference
import org.eclipse.keyple.core.plugin.CardIOException
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.plugin.WaitForCardInsertionAutonomousReaderApi
import org.eclipse.keyple.core.plugin.WaitForCardRemovalAutonomousReaderApi
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.DontWaitForCardRemovalDuringProcessingSpi
import org.eclipse.keyple.core.util.ByteArrayUtil
import timber.log.Timber

abstract class AbstractAndroidNfcReaderAdapter(activity: Activity) : AndroidNfcReader, ObservableReaderSpi, WaitForCardInsertionAutonomousReaderApi, WaitForCardRemovalAutonomousReaderApi, DontWaitForCardRemovalDuringProcessingSpi, NfcAdapter.ReaderCallback {

    private var activityWeakRef = WeakReference(activity)
    private val activatedProtocols = ArrayList<AndroidNfcReader.AndroidNfcSupportedProtocols>()
    private var tagProxy: TagProxy? = null
    protected var nfcAdapter: NfcAdapter? = null

    companion object {
        private const val NO_TAG = "no-tag"
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun getName(): String {
        return AndroidNfcReader.READER_NAME
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Throws(ReaderIOException::class)
    override fun openPhysicalChannel() {
        if (tagProxy?.isConnected != true) {
            try {
                Timber.d("Connect to tag..")
                tagProxy?.connect()
                Timber.i("Tag connected successfully : ${printTagId()}")
            } catch (e: IOException) {
                Timber.e(e, "Error while connecting to Tag ")
                throw ReaderIOException("Error while opening physical channel", e)
            }
        } else {
            Timber.i("Tag is already connected to : ${printTagId()}")
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Throws(ReaderIOException::class)
    override fun closePhysicalChannel() {
        try {
            tagProxy?.close()
            Timber.i("Disconnected tag : ${printTagId()}")
        } catch (e: IOException) {
            Timber.e(e, "Disconnecting error")
            throw ReaderIOException("Error while closing physical channel", e)
        } finally {
            tagProxy = null
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun isPhysicalChannelOpen(): Boolean {
        return tagProxy?.isConnected == true
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Throws(ReaderIOException::class)
    override fun checkCardPresence(): Boolean {
        return tagProxy != null
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun getATR(): ByteArray? {
        val atr = tagProxy?.atr
        Timber.d("ATR : ${ByteArrayUtil.toHex(atr)}")
        return if (atr?.isNotEmpty() == true) atr else null
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Throws(IllegalArgumentException::class, ReaderIOException::class, CardIOException::class)
    override fun transmitApdu(apduIn: ByteArray): ByteArray {
        Timber.d("Send data to card : ${apduIn.size} bytes")
        return with(tagProxy) {
            if (this == null) {
                throw ReaderIOException(
                        "Error while transmitting APDU, invalid out data buffer"
                )
            } else {
                try {
                    val bytes = transceive(apduIn)
                    if (bytes.size < 2) {
                        throw ReaderIOException(
                                "Error while transmitting APDU, invalid out data buffer"
                        )
                    } else {
                        Timber.d("Receive data from card : ${ByteArrayUtil.toHex(bytes)}")
                        bytes
                    }
                } catch (e: IOException) {
                    throw ReaderIOException(
                            "Error while transmitting APDU, invalid out data buffer", e
                    )
                } catch (e: NoSuchElementException) {
                    throw CardIOException(
                            "Error while transmitting APDU, no such Element",
                            e
                    )
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun isContactless(): Boolean {
        return true
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun unregister() {
        clearContext()
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun onStartDetection() {
        TODO("Not yet implemented")
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun onStopDetection() {
        TODO("Not yet implemented")
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Throws(IllegalStateException::class)
    override fun isProtocolSupported(readerProtocol: String): Boolean {
        return try {
                AndroidNfcReader.AndroidNfcSupportedProtocols.values().first { it.name == readerProtocol }
                true
            } catch (e: NoSuchElementException) {
                throw IllegalStateException("Unsupported protocol $readerProtocol")
            }
    }

    /**
     * {@inheritDoc}
     *
     * List of available readerProtocol is ISO_14443_4, MIFARE_ULTRA_LIGHT, MIFARE_CLASSIC.
     *
     * @since 2.0
     */
    override fun activateProtocol(readerProtocol: String) {
        if (activatedProtocols.firstOrNull { it.name == readerProtocol } == null) {
            activatedProtocols.add(AndroidNfcReader.AndroidNfcSupportedProtocols.valueOf(readerProtocol))
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun deactivateProtocol(readerProtocol: String) {
        AndroidNfcReader.AndroidNfcSupportedProtocols
                .values().firstOrNull { it.name == readerProtocol }?.let { activatedProtocols.remove(it) }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun isCurrentProtocol(readerProtocol: String): Boolean {
        val protocol = activatedProtocols.firstOrNull { it.name == readerProtocol } ?: return false
        return protocol.androidNfcTechIdentifier == tagProxy?.tech
    }

    private fun clearContext() {
        activityWeakRef.clear()
        activityWeakRef = WeakReference(null)
        nfcAdapter = null
    }

    /**
     *
     */
    override fun onTagDiscovered(tag: Tag?) {
        Timber.i("Received Tag Discovered event $tag")
        tag?.let {
            try {
                Timber.i("Getting tag proxy")
                tagProxy = TagProxy.getTagProxy(tag)
                onCardInserted()
            } catch (e: NoSuchElementException) {
                Timber.e(e)
            }
        }
    }

    private fun printTagId(): String {
        return with(tagProxy) {
            if (this == null) {
                NO_TAG
            } else {
                // build a user friendly TechList
                val techList = tag.techList.joinToString(separator = ", ") {
                    it.replace(
                            "android.nfc.tech.",
                            ""
                    )
                }
                // build a hexa TechId
                val tagId = tag.id.joinToString(separator = " ") { String.format("%02X", it) }
                "$tagId - $techList"
            }
        }
    }
}
