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

import org.eclipse.keyple.core.plugin.PluginIOException
import org.eclipse.keyple.core.plugin.spi.PluginSpi
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi

internal class AndroidNfcPluginAdapter(private val config: AndroidNfcConfig) :
    AndroidNfcPlugin, PluginSpi {

  override fun getName(): String {
    return AndroidNfcConstants.PLUGIN_NAME
  }

  override fun searchAvailableReaders(): MutableSet<ReaderSpi> =
      try {
        mutableSetOf(AndroidNfcReaderAdapter(config))
      } catch (e: Exception) {
        throw PluginIOException("Failed to initialize Android NFC plugin", e)
      }

  override fun onUnregister() {
    // NOP
  }
}
