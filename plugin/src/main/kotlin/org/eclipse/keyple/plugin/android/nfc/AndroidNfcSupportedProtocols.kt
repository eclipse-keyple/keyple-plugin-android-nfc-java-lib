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

import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight

/**
 * Enum representing supported NFC protocols.
 *
 * @since 2.0.0
 */
enum class AndroidNfcSupportedProtocols(private val techId: String) {

  /**
   * ISO 14443-4 protocol A and B.
   *
   * @since 2.0.0
   */
  ISO_14443_4(IsoDep::class.qualifiedName!!),

  /**
   * NXP MIFARE Ultralight protocol.
   *
   * @since 3.1.0
   */
  MIFARE_ULTRALIGHT(MifareUltralight::class.qualifiedName!!),

  /**
   * NXP MIFARE Classic protocol.
   *
   * @since 3.2.0
   */
  MIFARE_CLASSIC(MifareClassic::class.qualifiedName!!);

  internal val androidNfcTechIdentifier: String
    get() = techId
}
