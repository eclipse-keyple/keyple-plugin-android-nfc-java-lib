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

import android.app.Activity
import android.os.Build
import org.eclipse.keyple.core.plugin.spi.PluginSpi
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi

internal class AndroidNfcPluginAdapter(private val activity: Activity) :
    AndroidNfcPlugin, PluginSpi {

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  override fun getName(): String {
    return AndroidNfcPlugin.PLUGIN_NAME
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  override fun searchAvailableReaders(): MutableSet<ReaderSpi> {
    val readerSpis = HashSet<ReaderSpi>()
    readerSpis.add(
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
          AndroidNfcReaderPreNAdapter(activity)
        } else {
          AndroidNfcReaderPostNAdapter(activity)
        })
    return readerSpis
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  override fun onUnregister() {
    // Nothing to do for this plugin
  }
}
