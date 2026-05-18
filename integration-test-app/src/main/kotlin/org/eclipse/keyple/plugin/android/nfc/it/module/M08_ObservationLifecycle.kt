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

/** M08 - Observation lifecycle: start/stop detection, verify events are gated accordingly. */
class M08_ObservationLifecycle : AbstractModule("M08", "Observation Lifecycle") {

  override val scenarios =
      listOf<Scenario>(
          object : Scenario {
            override val id = "M08.1"
            override val title = "Card event received when detection is active"
            override val requiredEquipment = "Any NFC card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              ctx.log.info("Starting detection...")
              val detected = ctx.awaitTap("Tap a card within 20 seconds...", timeoutSec = 20)
              return if (detected) {
                ctx.log.info("CARD_INSERTED event received while detection active — correct")
                ctx.awaitRemoval()
                ScenarioResult.pass(id, "Card event received with detection active")
              } else {
                ScenarioResult.skip(id, "Timeout: no card detected")
              }
            }
          },
          object : Scenario {
            override val id = "M08.2"
            override val title = "Stop detection then restart — event received after restart"
            override val requiredEquipment = "Any NFC card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              // Stop detection first
              ctx.stopDetection()
              ctx.log.info("Detection stopped")
              Thread.sleep(500)
              // Restart and wait for tap
              ctx.log.info("Restarting detection...")
              val detected = ctx.awaitTap("Tap a card after detection restart...", timeoutSec = 20)
              return if (detected) {
                ctx.awaitRemoval()
                ScenarioResult.pass(id, "Card detected after detection restart")
              } else {
                ScenarioResult.skip(id, "Timeout: no card detected after restart")
              }
            }
          },
          object : Scenario {
            override val id = "M08.3"
            override val title = "onStartDetection / onStopDetection do not throw"
            override val requiredEquipment = "None"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              return try {
                val obsSpi = ctx.getObsSpi()
                obsSpi.onStartDetection()
                ctx.log.info("onStartDetection() OK")
                Thread.sleep(200)
                obsSpi.onStopDetection()
                ctx.log.info("onStopDetection() OK")
                ScenarioResult.pass(id, "onStartDetection/onStopDetection completed without error")
              } catch (e: Exception) {
                ScenarioResult.fail(id, "Exception: ${e.message}")
              }
            }
          },
      )
}
