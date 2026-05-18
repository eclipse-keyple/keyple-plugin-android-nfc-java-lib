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
package org.eclipse.keyple.plugin.android.nfc.it.module

import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConstants
import org.eclipse.keyple.plugin.android.nfc.it.framework.AbstractModule
import org.eclipse.keyple.plugin.android.nfc.it.framework.Scenario
import org.eclipse.keyple.plugin.android.nfc.it.framework.ScenarioResult
import org.eclipse.keyple.plugin.android.nfc.it.framework.ValidationContext

/** M01 - Plugin and reader discovery (no card required). */
class M01_PluginDiscovery : AbstractModule("M01", "Plugin Discovery") {

  override val scenarios =
      listOf<Scenario>(
          object : Scenario {
            override val id = "M01.1"
            override val title = "Plugin name"
            override val requiredEquipment = "None"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              val name = AndroidNfcConstants.PLUGIN_NAME
              ctx.log.info("Plugin name: $name")
              return if (name == "AndroidNfcPlugin") ScenarioResult.pass(id, "Plugin name: $name")
              else ScenarioResult.fail(id, "Unexpected plugin name: $name")
            }
          },
          object : Scenario {
            override val id = "M01.2"
            override val title = "Reader name"
            override val requiredEquipment = "None"

            override fun run(ctx: ValidationContext): ScenarioResult {
              val plugin = ctx.plugin ?: return ScenarioResult.fail(id, "Plugin not initialized")
              val readers = plugin.getReaders()
              ctx.log.info("Reader count: ${readers.size}")
              if (readers.isEmpty()) return ScenarioResult.fail(id, "No readers found")
              val name = readers.first().name
              ctx.log.info("Reader name: $name")
              val expected = AndroidNfcConstants.READER_NAME
              return if (name == expected) ScenarioResult.pass(id, "Reader name: $name")
              else ScenarioResult.fail(id, "Expected '$expected', got '$name'")
            }
          },
          object : Scenario {
            override val id = "M01.3"
            override val title = "Reader is contactless"
            override val requiredEquipment = "None"

            override fun run(ctx: ValidationContext): ScenarioResult {
              val plugin = ctx.plugin ?: return ScenarioResult.fail(id, "Plugin not initialized")
              val reader = plugin.getReaders().firstOrNull() ?: return ScenarioResult.fail(id, "No reader")
              val contactless = reader.isContactless
              ctx.log.info("isContactless: $contactless")
              return if (contactless) ScenarioResult.pass(id, "Reader is contactless")
              else ScenarioResult.fail(id, "Reader is not contactless")
            }
          },
      )
}
