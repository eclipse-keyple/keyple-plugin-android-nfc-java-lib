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
import org.eclipse.keyple.plugin.android.nfc.it.framework.Scenario.Companion.ERR_TIMEOUT_NO_CARD
import org.eclipse.keyple.plugin.android.nfc.it.framework.ScenarioResult
import org.eclipse.keyple.plugin.android.nfc.it.framework.ValidationContext

/** M06 - Card observation: CARD_INSERTED and CARD_REMOVED events via CardReaderObserverSpi. */
class M06_CardObservation : AbstractModule("M06", "Card Observation") {

  override val scenarios =
      listOf<Scenario>(
          object : Scenario {
            override val id = "M06.1"
            override val title = "CARD_INSERTED event received on tap"
            override val requiredEquipment = "Any NFC card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              val detected = ctx.awaitTap("Tap a card to trigger CARD_INSERTED event...")
              return if (detected) {
                ctx.log.info("CARD_INSERTED event received")
                ctx.awaitRemoval()
                ScenarioResult.pass(id, "CARD_INSERTED event received correctly")
              } else {
                ScenarioResult.skip(id, "Timeout: CARD_INSERTED event not received")
              }
            }
          },
          object : Scenario {
            override val id = "M06.2"
            override val title = "CARD_REMOVED event received after card removal"
            override val requiredEquipment = "Any NFC card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              if (!ctx.awaitTap()) return ScenarioResult.skip(id, ERR_TIMEOUT_NO_CARD)
              ctx.log.info("CARD_INSERTED received — waiting for card removal...")
              val removed = ctx.awaitRemoval("Remove the card to trigger CARD_REMOVED event...")
              return if (removed) ScenarioResult.pass(id, "CARD_REMOVED event received correctly")
              else ScenarioResult.skip(id, "Timeout: CARD_REMOVED event not received")
            }
          },
          object : Scenario {
            override val id = "M06.3"
            override val title = "Two successive insertions in REPEATING mode"
            override val requiredEquipment = "Any NFC card (tap twice)"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              // First tap
              if (!ctx.awaitTap("Tap #1: tap a card..."))
                  return ScenarioResult.skip(id, "Timeout on first tap")
              ctx.log.info("First CARD_INSERTED received")
              if (!ctx.awaitRemoval("Remove the card after first tap..."))
                  return ScenarioResult.skip(id, "Timeout waiting for first removal")
              ctx.log.info("First CARD_REMOVED received")
              // Second tap
              if (!ctx.awaitTap("Tap #2: tap the card again..."))
                  return ScenarioResult.skip(id, "Timeout on second tap")
              ctx.log.info("Second CARD_INSERTED received")
              ctx.awaitRemoval("Remove the card...")
              return ScenarioResult.pass(id, "Two successive insertions detected correctly")
            }
          },
      )
}
