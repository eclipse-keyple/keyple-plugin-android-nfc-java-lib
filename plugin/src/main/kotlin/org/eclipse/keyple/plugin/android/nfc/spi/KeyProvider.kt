/* **************************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.android.nfc.spi

/**
 * Interface allowing the application to provide authentication keys dynamically.
 *
 * @since 3.2.0
 */
interface KeyProvider {

  /**
   * Retrieves the key associated with the given key index.
   *
   * @param keyIndex The index of the key requested.
   * @return The key as a byte array, or null if not found.
   * @since 3.2.0
   */
  fun getKey(keyIndex: Int): ByteArray?
}
