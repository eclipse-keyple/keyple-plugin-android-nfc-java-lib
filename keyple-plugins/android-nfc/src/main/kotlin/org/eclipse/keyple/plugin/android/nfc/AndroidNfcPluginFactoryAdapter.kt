package org.eclipse.keyple.plugin.android.nfc

import org.eclipse.keyple.core.common.CommonsApiProperties
import org.eclipse.keyple.core.plugin.PluginApiProperties
import org.eclipse.keyple.core.plugin.spi.PluginFactorySpi
import org.eclipse.keyple.core.plugin.spi.PluginSpi

class AndroidNfcPluginFactoryAdapter: AndroidNfcPluginFactory, PluginFactorySpi {


    override fun getPluginApiVersion(): String {
        TODO("Not yet implemented")
    }

    override fun getCommonsApiVersion(): String {
        return CommonsApiProperties.VERSION
    }

    override fun getPluginName(): String {
        return PluginApiProperties.VERSION
    }

    override fun getPlugin(): PluginSpi {
        TODO("Not yet implemented")
    }

    companion object{
        val PLUGIN_NAME = "AndroidNfcPlugin"
    }
}