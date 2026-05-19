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

import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keyple.plugin.android.nfc.it.framework.AbstractModule
import org.eclipse.keyple.plugin.android.nfc.it.framework.Scenario
import org.eclipse.keyple.plugin.android.nfc.it.framework.Scenario.Companion.ERR_TIMEOUT_NO_CARD
import org.eclipse.keyple.plugin.android.nfc.it.framework.ScenarioResult
import org.eclipse.keyple.plugin.android.nfc.it.framework.ValidationContext

/** M07 - deselectCard(): no-op contract and channel behavior after deselection. */
class M07_CardDeselect : AbstractModule("M07", "Card Deselect") {

  private val GET_CHALLENGE = HexUtil.toByteArray("0084000008")

  override val scenarios =
      listOf<Scenario>(
          object : Scenario {
            override val id = "M07.1"
            override val title = "deselectCard() does not throw"
            override val requiredEquipment = "Any NFC card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              if (!ctx.awaitTap()) return ScenarioResult.skip(id, ERR_TIMEOUT_NO_CARD)
              return try {
                ctx.getObsSpi().deselectCard()
                ctx.log.info("deselectCard() completed without exception")
                ctx.awaitRemoval()
                ScenarioResult.pass(id, "deselectCard() is a no-op (no exception thrown)")
              } catch (e: Exception) {
                ScenarioResult.fail(id, "deselectCard() threw: ${e.message}")
              }
            }
          },
          object : Scenario {
            override val id = "M07.2"
            override val title = "isCardPresent() = true after deselectCard()"
            override val requiredEquipment = "Any NFC card (keep card on reader)"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              if (!ctx.awaitTap("Tap a card and KEEP it on the reader..."))
                  return ScenarioResult.skip(id, ERR_TIMEOUT_NO_CARD)
              return try {
                ctx.getObsSpi().deselectCard()
                val present = ctx.getSpi().isCardPresent()
                ctx.log.info("isCardPresent() after deselectCard(): $present")
                ctx.awaitRemoval()
                if (present)
                    ScenarioResult.pass(
                        id, "Card still present after deselectCard() — channel preserved")
                else ScenarioResult.fail(id, "isCardPresent() = false after deselectCard()")
              } catch (e: Exception) {
                ScenarioResult.fail(id, "Exception: ${e.message}")
              }
            }
          },
          object : Scenario {
            override val id = "M07.3"
            override val title = "APDU succeeds after deselectCard()"
            override val requiredEquipment = "ISO 14443-4 card (keep card on reader)"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
              if (!ctx.awaitTap("Tap an ISO 14443-4 card and KEEP it on the reader..."))
                  return ScenarioResult.skip(id, ERR_TIMEOUT_NO_CARD)
              return try {
                ctx.getObsSpi().deselectCard()
                ctx.log.info("deselectCard() called")
                val resp = spi.transmitApdu(GET_CHALLENGE)
                ctx.log.info("GET CHALLENGE after deselect: ${HexUtil.toHex(resp)}")
                ctx.awaitRemoval()
                spi.deactivateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                val sw = HexUtil.toHex(resp.copyOfRange(resp.size - 2, resp.size))
                ScenarioResult.pass(id, "APDU after deselectCard() OK, SW=$sw")
              } catch (e: Exception) {
                ScenarioResult.fail(id, "APDU after deselectCard() failed: ${e.message}")
              }
            }
          },
      )
}
