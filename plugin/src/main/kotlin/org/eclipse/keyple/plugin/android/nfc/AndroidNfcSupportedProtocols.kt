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
  ISO_14443_4(IsoDep::class.qualifiedName!!);

  internal val androidNfcTechIdentifier: String
    get() = techId
}
