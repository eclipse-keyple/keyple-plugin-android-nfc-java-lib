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

import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keyple.plugin.android.nfc.it.framework.AbstractModule
import org.eclipse.keyple.plugin.android.nfc.it.framework.Scenario
import org.eclipse.keyple.plugin.android.nfc.it.framework.ScenarioResult
import org.eclipse.keyple.plugin.android.nfc.it.framework.ValidationContext

/** M04 - Protocol detection: isCurrentProtocol() for each supported protocol. */
class M04_ProtocolDetection : AbstractModule("M04", "Protocol Detection") {

  override val scenarios =
      listOf<Scenario>(
          object : Scenario {
            override val id = "M04.1"
            override val title = "ISO 14443-4 detected"
            override val requiredEquipment = "ISO 14443-4 card (A or B)"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
              if (!ctx.awaitTap("Tap an ISO 14443-4 card..."))
                  return ScenarioResult.skip(id, "Timeout: no card detected")
              val current = spi.isCurrentProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
              ctx.log.info("isCurrentProtocol(ISO_14443_4): $current")
              ctx.awaitRemoval()
              spi.deactivateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
              return if (current) ScenarioResult.pass(id, "ISO_14443_4 detected correctly")
              else ScenarioResult.fail(id, "ISO_14443_4 not detected")
            }
          },
          object : Scenario {
            override val id = "M04.2"
            override val title = "MIFARE Ultralight detected"
            override val requiredEquipment = "MIFARE Ultralight tag"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name)
              if (!ctx.awaitTap("Tap a MIFARE Ultralight tag..."))
                  return ScenarioResult.skip(id, "Timeout: no card detected")
              val current = spi.isCurrentProtocol(AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name)
              ctx.log.info("isCurrentProtocol(MIFARE_ULTRALIGHT): $current")
              ctx.awaitRemoval()
              spi.deactivateProtocol(AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name)
              return if (current) ScenarioResult.pass(id, "MIFARE_ULTRALIGHT detected correctly")
              else ScenarioResult.fail(id, "MIFARE_ULTRALIGHT not detected")
            }
          },
          object : Scenario {
            override val id = "M04.3"
            override val title = "MIFARE Classic 1K detected"
            override val requiredEquipment = "MIFARE Classic 1K card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.MIFARE_CLASSIC_1K.name)
              if (!ctx.awaitTap("Tap a MIFARE Classic 1K card..."))
                  return ScenarioResult.skip(id, "Timeout: no card detected")
              val current = spi.isCurrentProtocol(AndroidNfcSupportedProtocols.MIFARE_CLASSIC_1K.name)
              ctx.log.info("isCurrentProtocol(MIFARE_CLASSIC_1K): $current")
              ctx.awaitRemoval()
              spi.deactivateProtocol(AndroidNfcSupportedProtocols.MIFARE_CLASSIC_1K.name)
              return if (current) ScenarioResult.pass(id, "MIFARE_CLASSIC_1K detected correctly")
              else ScenarioResult.fail(id, "MIFARE_CLASSIC_1K not detected")
            }
          },
          object : Scenario {
            override val id = "M04.4"
            override val title = "MIFARE Classic 4K detected"
            override val requiredEquipment = "MIFARE Classic 4K card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.MIFARE_CLASSIC_4K.name)
              if (!ctx.awaitTap("Tap a MIFARE Classic 4K card..."))
                  return ScenarioResult.skip(id, "Timeout: no card detected")
              val current = spi.isCurrentProtocol(AndroidNfcSupportedProtocols.MIFARE_CLASSIC_4K.name)
              ctx.log.info("isCurrentProtocol(MIFARE_CLASSIC_4K): $current")
              ctx.awaitRemoval()
              spi.deactivateProtocol(AndroidNfcSupportedProtocols.MIFARE_CLASSIC_4K.name)
              return if (current) ScenarioResult.pass(id, "MIFARE_CLASSIC_4K detected correctly")
              else ScenarioResult.fail(id, "MIFARE_CLASSIC_4K not detected")
            }
          },
      )
}
