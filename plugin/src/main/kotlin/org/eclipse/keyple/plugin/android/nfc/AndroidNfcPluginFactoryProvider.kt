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

/**
 * The following example shows how to create a [AndroidNfcPluginFactory] object with the
 * [AndroidNfcPluginFactoryProvider] and use it to register a Plugin.
 *
 * ```
 * val plugin = SmartCardServiceProvider
 *      .getService()
 *      .registerPlugin(
 *          AndroidNfcPluginFactoryProvider(this).getFactory()
 *       )
 * ```
 *
 * @property activity The the activity where the NFC is requested.
 * @constructor Builds instances of [AndroidNfcPluginFactory] from context provided in constructor.
 * @since 2.0.0
 */
class AndroidNfcPluginFactoryProvider(private val activity: Activity) : AndroidNfcPluginFactory {

  /**
   * Returns an instance of [AndroidNfcPluginFactory].
   *
   * @return A [AndroidNfcPluginFactory]
   * @since 2.0.0
   */
  fun getFactory(): AndroidNfcPluginFactory {
    return AndroidNfcPluginFactoryAdapter(activity)
  }
}
