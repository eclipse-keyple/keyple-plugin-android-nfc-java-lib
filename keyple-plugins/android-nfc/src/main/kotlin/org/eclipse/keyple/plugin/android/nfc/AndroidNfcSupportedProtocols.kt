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

/**
 * Enum mapping Protocols to android NFC Tech identifiers.
 *
 * @since 2.0
 */
public enum class AndroidNfcSupportedProtocols(val androidNfcTechIdentifier: String) {
    ISO_14443_4("android.nfc.tech.IsoDep"),
    MIFARE_ULTRA_LIGHT("android.nfc.tech.MifareUltralight"),
    MIFARE_CLASSIC("android.nfc.tech.MifareClassic")
}
