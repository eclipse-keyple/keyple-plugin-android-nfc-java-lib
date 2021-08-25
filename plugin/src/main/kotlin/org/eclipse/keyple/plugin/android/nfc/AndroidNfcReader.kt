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

import android.content.Intent
import org.eclipse.keyple.core.common.KeypleReaderExtension

/**
 * Generic type for a Keyple Android NFC reader extension.
 *
 * @since 2.0.0
 */
interface AndroidNfcReader : KeypleReaderExtension {

    companion object {
        val READER_NAME = "AndroidNfcReader"
    }

    /**
     * Gets a string describing the low level description of the current tag.
     *
     * Used for logging purpose
     * @return string
     *
     * @since 2.0.0
     */
    fun printTagId(): String

    /**
     * Process data from NFC Intent. Can be use to handle NFC Tag received when app is started
     * by nfc detection
     *
     * @param intent : Intent received and filterByProtocol by xml tech_list
     *
     * @since 2.0.0
     */
    fun processIntent(intent: Intent)

    /**
     * Allows the calling application to specify the delay that the platform will use for performing presence checks on any discovered tag.
     * see @NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY
     *
     * @since 2.0.0
     */
    var presenceCheckDelay: Int?

    /**
     * Allows the invoker to prevent the platform from playing sounds when it discovers a tag.
     *
     * @since 2.0.0
     */
    var noPlateformSound: Boolean?

    /**
     * Prevent the platform from performing any NDEF checks in reader mode.
     *
     * @since 2.0.0
     */
    var skipNdefCheck: Boolean?
}
