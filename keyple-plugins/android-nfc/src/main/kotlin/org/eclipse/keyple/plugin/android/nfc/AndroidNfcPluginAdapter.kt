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
import org.eclipse.keyple.core.plugin.spi.PluginSpi
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi

class AndroidNfcPluginAdapter(private val activity: Activity) : AndroidNfcPlugin, PluginSpi {

    override fun getName(): String {
        return AndroidNfcPlugin.PLUGIN_NAME
    }

    override fun searchAvailableReaders(): MutableSet<ReaderSpi> {
        TODO("Not yet implemented")
    }

    override fun unregister() {
        TODO("Not yet implemented")
    }
}
