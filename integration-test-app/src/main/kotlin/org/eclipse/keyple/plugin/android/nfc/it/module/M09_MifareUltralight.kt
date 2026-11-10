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
import org.json.JSONObject

/**
 * M09 - MIFARE Ultralight: protocol detection and UID/power-on data verification.
 *
 * Note: direct block read/write requires an ApduInterpreterFactory configured in AndroidNfcConfig.
 * These scenarios validate the tag is correctly identified and produces valid power-on data.
 */
class M09_MifareUltralight : AbstractModule("M09", "MIFARE Ultralight") {

  override val scenarios =
      listOf<Scenario>(
          object : Scenario {
            override val id = "M09.1"
            override val title = "MIFARE Ultralight detected and isCurrentProtocol() = true"
            override val requiredEquipment = "MIFARE Ultralight tag"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name)
              if (!ctx.awaitTap("Tap a MIFARE Ultralight tag..."))
                  return ScenarioResult.skip(id, "Timeout: no tag detected")
              val current =
                  spi.isCurrentProtocol(AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name)
              ctx.log.info("isCurrentProtocol(MIFARE_ULTRALIGHT): $current")
              ctx.awaitRemoval()
              spi.deactivateProtocol(AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name)
              return if (current) ScenarioResult.pass(id, "MIFARE Ultralight protocol identified")
              else ScenarioResult.fail(id, "MIFARE_ULTRALIGHT not identified")
            }
          },
          object : Scenario {
            override val id = "M09.2"
            override val title = "MIFARE Ultralight UID in power-on data"
            override val requiredEquipment = "MIFARE Ultralight tag"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name)
              if (!ctx.awaitTap("Tap a MIFARE Ultralight tag..."))
                  return ScenarioResult.skip(id, "Timeout: no tag detected")
              val raw = spi.getPowerOnData()
              ctx.log.info("powerOnData: $raw")
              ctx.awaitRemoval()
              spi.deactivateProtocol(AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name)
              return try {
                val json = JSONObject(raw)
                val uid = json.getString("uid")
                ctx.log.info("UID: $uid (${uid.length / 2} bytes)")
                ScenarioResult.pass(id, "UID present in power-on data: $uid")
              } catch (e: Exception) {
                ScenarioResult.fail(id, "Invalid power-on data: ${e.message}")
              }
            }
          },
          object : Scenario {
            override val id = "M09.3"
            override val title = "isProtocolSupported(MIFARE_ULTRALIGHT)"
            override val requiredEquipment = "None"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              val spi = ctx.getSpi()
              val supported =
                  spi.isProtocolSupported(AndroidNfcSupportedProtocols.MIFARE_ULTRALIGHT.name)
              ctx.log.info("isProtocolSupported(MIFARE_ULTRALIGHT): $supported")
              return if (supported) ScenarioResult.pass(id, "MIFARE_ULTRALIGHT is supported")
              else ScenarioResult.fail(id, "MIFARE_ULTRALIGHT is not reported as supported")
            }
          },
      )
}
