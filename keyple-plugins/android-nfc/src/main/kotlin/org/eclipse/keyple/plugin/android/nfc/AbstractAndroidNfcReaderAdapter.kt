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
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import java.io.IOException
import java.lang.ref.WeakReference
import org.eclipse.keyple.core.plugin.CardIOException
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.plugin.WaitForCardInsertionAutonomousReaderApi
import org.eclipse.keyple.core.plugin.WaitForCardRemovalAutonomousReaderApi
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionAutonomousSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.DontWaitForCardRemovalDuringProcessingSpi
import org.eclipse.keyple.core.util.ByteArrayUtil
import timber.log.Timber

internal abstract class AbstractAndroidNfcReaderAdapter(activity: Activity) : AndroidNfcReader,
        ObservableReaderSpi,
        WaitForCardInsertionAutonomousSpi,
        DontWaitForCardRemovalDuringProcessingSpi,
        NfcAdapter.ReaderCallback {

    private var activityWeakRef = WeakReference(activity)
    private val activatedProtocols = ArrayList<AndroidNfcSupportedProtocols>()
    private var tagProxy: TagProxy? = null
    protected var nfcAdapter: NfcAdapter? = null

    private var mPresenceCheckDelay: Int? = 100
    private var mNoPlateformSound: Boolean? = false
    private var mSkipNdefCheck: Boolean? = false

    lateinit var waitForCardInsertionAutonomousReaderApi: WaitForCardInsertionAutonomousReaderApi
    lateinit var waitForCardRemovalAutonomousReaderApi: WaitForCardRemovalAutonomousReaderApi

    override var presenceCheckDelay: Int?
        get() = mPresenceCheckDelay
        set(value) {
            mPresenceCheckDelay = value
        }

    override var noPlateformSound: Boolean?
        get() = mNoPlateformSound
        set(value) {
            mNoPlateformSound = value
        }

    override var skipNdefCheck: Boolean?
        get() = mSkipNdefCheck
        set(value) {
            mSkipNdefCheck = value
        }

    /**
     * Build Reader Mode flags Integer from parameters
     *
     * @return flags Integer
     */
    // Build flags list for reader mode
    val flags: Int
        get() {
            var flags = 0

            if (skipNdefCheck != null && skipNdefCheck == true) {
                flags = flags or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            }

            if (noPlateformSound != null && noPlateformSound == true) {
                flags = flags or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
            }
            for (cardProtocol in this.activatedProtocols) {
                if (AndroidNfcSupportedProtocols.ISO_14443_4 == cardProtocol) {
                    flags = flags or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_A
                } else if (AndroidNfcSupportedProtocols.MIFARE_ULTRA_LIGHT == cardProtocol || AndroidNfcSupportedProtocols.MIFARE_CLASSIC == cardProtocol) {
                    flags = flags or NfcAdapter.FLAG_READER_NFC_A
                }
            }

            return flags
        }

    /**
     * Build Reader Mode options Bundle from parameters
     *
     * @return options
     */
    val options: Bundle
        get() {
            val options = Bundle(1)
            presenceCheckDelay?.let { delay ->
                options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, delay)
            }
            return options
        }

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
    override fun getAtr(): ByteArray {
        val atr = tagProxy?.atr
        Timber.d("ATR : ${ByteArrayUtil.toHex(atr)}")
        return atr ?: byteArrayOf()
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Throws(IllegalArgumentException::class, CardIOException::class)
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
                    throw CardIOException(
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
        Timber.d("onStartDetection")
        if (activityWeakRef.get() == null) {
            throw IllegalStateException("onStartDetection() failed : no context available")
        }

        if (nfcAdapter == null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(activityWeakRef.get()!!)
        }

        val flags = flags

        val options = options

        Timber.i("Enabling Read Write Mode with flags : $flags and options : $options")

        // Reader mode for NFC reader allows to listen to NFC events without the Intent mechanism.
        // It is active only when the activity thus the fragment is active.
        nfcAdapter?.enableReaderMode(activityWeakRef.get(), this, flags, options)
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun onStopDetection() {
        Timber.d("onStopDetection")
        nfcAdapter?.let {
            if (activityWeakRef.get() != null) {
                it.disableReaderMode(activityWeakRef.get())
            } else {
                throw IllegalStateException("onStopDetection failed : no context available")
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun isProtocolSupported(readerProtocol: String): Boolean {
        return try {
                AndroidNfcSupportedProtocols.values().first { it.name == readerProtocol }
                true
            } catch (e: NoSuchElementException) {
                false
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
            activatedProtocols.add(AndroidNfcSupportedProtocols.valueOf(readerProtocol))
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun deactivateProtocol(readerProtocol: String) {
        AndroidNfcSupportedProtocols
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

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    override fun connect(waitForCardInsertionAutonomousReaderApi: WaitForCardInsertionAutonomousReaderApi) {
        this.waitForCardInsertionAutonomousReaderApi = waitForCardInsertionAutonomousReaderApi
    }

    private fun clearContext() {
        activityWeakRef.clear()
        activityWeakRef = WeakReference(null)
        nfcAdapter = null
    }

    /**
     * When a card is presented within the NFC field, the event
     * is transmitted from Android OS component to keyple
     *
     * @since 2.0
     */
    override fun onTagDiscovered(tag: Tag?) {
        Timber.i("Received Tag Discovered event $tag")
        tag?.let {
            try {
                Timber.i("Getting tag proxy")
                tagProxy = TagProxy.getTagProxy(tag)
                waitForCardInsertionAutonomousReaderApi.onCardInserted()
            } catch (e: NoSuchElementException) {
                Timber.e(e)
            }
        }
    }

    /**
     * Process data from NFC Intent
     *
     * @param intent : Intent received and filterByProtocol by xml tech_list
     *
     * @since 2.0
     */
    override fun processIntent(intent: Intent) {
        // Extract Tag from Intent
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        this.onTagDiscovered(tag)
    }

    /**
     * Only for logging purpose
     */
    override fun printTagId(): String {
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

    /**
     * Allow to retrieve tag reguardless of Android version
     *
     * @since 2.0
     */
    protected fun getTagProxyTag(): Tag? {
        return tagProxy?.tag
    }
}
