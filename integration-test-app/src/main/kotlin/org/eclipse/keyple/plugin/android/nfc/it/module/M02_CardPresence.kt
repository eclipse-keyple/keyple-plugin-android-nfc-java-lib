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

/** M02 - Card presence detection via isCardPresent(). */
class M02_CardPresence : AbstractModule("M02", "Card Presence") {

  override val scenarios =
      listOf<Scenario>(
          object : Scenario {
            override val id = "M02.1"
            override val title = "isCardPresent() = false before any tap"
            override val requiredEquipment = "None (keep any card away from the reader)"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              val spi = ctx.getSpi()
              val present = spi.isCardPresent()
              ctx.log.info("isCardPresent(): $present")
              return if (!present) ScenarioResult.pass(id, "isCardPresent() = false — correct")
              else ScenarioResult.fail(id, "isCardPresent() = true without tap — unexpected")
            }
          },
          object : Scenario {
            override val id = "M02.2"
            override val title = "isCardPresent() = true after CARD_INSERTED"
            override val requiredEquipment = "Any NFC card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              if (!ctx.awaitTap()) return ScenarioResult.skip(id, "Timeout: no card detected")
              val spi = ctx.getSpi()
              val present = spi.isCardPresent()
              ctx.log.info("isCardPresent() after tap: $present")
              ctx.awaitRemoval()
              return if (present) ScenarioResult.pass(id, "isCardPresent() = true after tap")
              else ScenarioResult.fail(id, "isCardPresent() = false after tap — unexpected")
            }
          },
          object : Scenario {
            override val id = "M02.3"
            override val title = "isCardPresent() = false after card removal"
            override val requiredEquipment = "Any NFC card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              if (!ctx.awaitTap()) return ScenarioResult.skip(id, "Timeout: no card detected")
              val spi = ctx.getSpi()
              ctx.log.info("Card tapped, isCardPresent(): ${spi.isCardPresent()}")
              if (!ctx.awaitRemoval()) return ScenarioResult.skip(id, "Timeout: card not removed")
              val present = spi.isCardPresent()
              ctx.log.info("isCardPresent() after removal: $present")
              return if (!present) ScenarioResult.pass(id, "isCardPresent() = false after removal")
              else ScenarioResult.fail(id, "isCardPresent() = true after removal — unexpected")
            }
          },
      )
}
