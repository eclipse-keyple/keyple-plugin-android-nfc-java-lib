/* **************************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
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
 * Provider of [AndroidNfcPluginFactory] instances.
 *
 * @since 3.0.0
 */
object AndroidNfcPluginFactoryProvider {

  /**
   * Provides an instance of [AndroidNfcPluginFactory].
   *
   * @param config The associated [AndroidNfcConfig].
   * @since 3.0.0
   */
  fun provideFactory(config: AndroidNfcConfig): AndroidNfcPluginFactory {
    return AndroidNfcPluginFactoryAdapter(config)
  }
}
