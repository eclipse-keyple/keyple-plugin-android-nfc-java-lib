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

import org.eclipse.keyple.plugin.android.nfc.it.framework.AbstractModule
import org.eclipse.keyple.plugin.android.nfc.it.framework.Scenario
import org.eclipse.keyple.plugin.android.nfc.it.framework.ScenarioResult
import org.eclipse.keyple.plugin.android.nfc.it.framework.ValidationContext
import org.json.JSONObject

/** M03 - Power-on data: JSON structure returned after card detection. */
class M03_PowerOnData : AbstractModule("M03", "Power-On Data") {

  override val scenarios =
      listOf<Scenario>(
          object : Scenario {
            override val id = "M03.1"
            override val title = "NFC-A card: type, uid, atqa, sak"
            override val requiredEquipment = "ISO 14443-A card (e.g. MIFARE, ISO 14443-4 A)"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              if (!ctx.awaitTap("Tap an NFC-A card (ISO 14443-A)..."))
                  return ScenarioResult.skip(id, "Timeout: no card detected")
              val spi = ctx.getSpi()
              val raw = spi.getPowerOnData()
              ctx.log.info("powerOnData: $raw")
              ctx.awaitRemoval()
              return try {
                val json = JSONObject(raw)
                val type = json.getString("type")
                ctx.log.info("  type: $type")
                ctx.log.info("  uid: ${json.optString("uid")}")
                ctx.log.info("  atqa: ${json.optString("atqa")}")
                ctx.log.info("  sak: ${json.optString("sak")}")
                if (type == "A" && json.has("uid") && json.has("atqa") && json.has("sak"))
                    ScenarioResult.pass(id, "NFC-A power-on data OK (type=$type)")
                else ScenarioResult.fail(id, "Missing fields or wrong type: $raw")
              } catch (e: Exception) {
                ScenarioResult.fail(id, "Invalid JSON: ${e.message}")
              }
            }
          },
          object : Scenario {
            override val id = "M03.2"
            override val title = "NFC-B card: type, uid, applicationData, protocolInfo"
            override val requiredEquipment = "ISO 14443-B card (e.g. ID card, ISO 14443-4 B)"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              if (!ctx.awaitTap("Tap an NFC-B card (ISO 14443-B)..."))
                  return ScenarioResult.skip(id, "Timeout: no card detected")
              val spi = ctx.getSpi()
              val raw = spi.getPowerOnData()
              ctx.log.info("powerOnData: $raw")
              ctx.awaitRemoval()
              return try {
                val json = JSONObject(raw)
                val type = json.getString("type")
                ctx.log.info("  type: $type")
                ctx.log.info("  uid: ${json.optString("uid")}")
                ctx.log.info("  applicationData: ${json.optString("applicationData")}")
                ctx.log.info("  protocolInfo: ${json.optString("protocolInfo")}")
                if (type == "B" &&
                    json.has("uid") &&
                    json.has("applicationData") &&
                    json.has("protocolInfo"))
                    ScenarioResult.pass(id, "NFC-B power-on data OK (type=$type)")
                else ScenarioResult.fail(id, "Missing fields or wrong type: $raw")
              } catch (e: Exception) {
                ScenarioResult.fail(id, "Invalid JSON: ${e.message}")
              }
            }
          },
          object : Scenario {
            override val id = "M03.3"
            override val title = "Power-on data is empty before any tap"
            override val requiredEquipment = "None"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              val spi = ctx.getSpi()
              val raw = spi.getPowerOnData()
              ctx.log.info("getPowerOnData() without tap: '$raw'")
              return if (raw.isEmpty()) ScenarioResult.pass(id, "getPowerOnData() = \"\" (no card)")
              else ScenarioResult.fail(id, "Expected empty string, got: $raw")
            }
          },
      )
}
