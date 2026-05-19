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

import org.eclipse.keyple.core.plugin.CardIOException
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keyple.plugin.android.nfc.it.framework.AbstractModule
import org.eclipse.keyple.plugin.android.nfc.it.framework.Scenario
import org.eclipse.keyple.plugin.android.nfc.it.framework.Scenario.Companion.ERR_TIMEOUT_NO_CARD
import org.eclipse.keyple.plugin.android.nfc.it.framework.ScenarioResult
import org.eclipse.keyple.plugin.android.nfc.it.framework.ValidationContext

/** M10 - Error recovery: CardIOException on removed card, channel cleanup after error. */
class M10_ErrorRecovery : AbstractModule("M10", "Error Recovery") {

  private val GET_CHALLENGE = HexUtil.toByteArray("0084000008")

  override val scenarios =
      listOf<Scenario>(
          object : Scenario {
            override val id = "M10.1"
            override val title = "CardIOException when card removed before APDU"
            override val requiredEquipment = "ISO 14443-4 card (will be removed)"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
              if (!ctx.awaitTap("Tap an ISO 14443-4 card..."))
                  return ScenarioResult.skip(id, ERR_TIMEOUT_NO_CARD)
              ctx.log.info("Card tapped. Remove the card NOW, then wait 2 seconds...")
              // Wait for the card to be physically removed before sending the APDU
              if (!ctx.awaitRemoval("Remove the card NOW...", timeoutSec = 15))
                  return ScenarioResult.skip(id, "Timeout: card not removed")
              Thread.sleep(500)
              return try {
                val resp = spi.transmitApdu(GET_CHALLENGE)
                // If we get here, no exception was thrown — unexpected
                spi.deactivateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                ScenarioResult.fail(
                    id, "Expected CardIOException but got response: ${HexUtil.toHex(resp)}")
              } catch (e: CardIOException) {
                ctx.log.info("CardIOException received: ${e.message}")
                spi.deactivateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                ScenarioResult.pass(id, "CardIOException thrown on removed card — correct")
              } catch (e: Exception) {
                ScenarioResult.fail(
                    id,
                    "Wrong exception type: ${e.javaClass.simpleName} — ${e.message}")
              }
            }
          },
          object : Scenario {
            override val id = "M10.2"
            override val title = "New insertion works after CardIOException"
            override val requiredEquipment = "ISO 14443-4 card (two taps)"

            override fun run(ctx: ValidationContext): ScenarioResult {
              requireInitialized(ctx)?.let { return it }
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
              // First tap — simulate error by removing the card
              if (!ctx.awaitTap("Tap #1: tap a card, then remove it quickly..."))
                  return ScenarioResult.skip(id, "Timeout on first tap")
              if (!ctx.awaitRemoval("Remove the card quickly...", timeoutSec = 10))
                  return ScenarioResult.skip(id, "Timeout waiting for first removal")
              // Try APDU to generate CardIOException
              try {
                spi.transmitApdu(GET_CHALLENGE)
              } catch (_: CardIOException) {
                ctx.log.info("CardIOException as expected on removed card")
              } catch (e: Exception) {
                ctx.log.warn("Unexpected exception during APDU probe: ${e.message}")
              }
              // Second tap — must work normally
              if (!ctx.awaitTap("Tap #2: tap the card again..."))
                  return ScenarioResult.skip(id, "Timeout on second tap")
              return try {
                val resp = spi.transmitApdu(GET_CHALLENGE)
                ctx.log.info("APDU after recovery: SW=${HexUtil.toHex(resp.copyOfRange(resp.size - 2, resp.size))}")
                ctx.awaitRemoval()
                spi.deactivateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                ScenarioResult.pass(id, "New insertion after CardIOException works correctly")
              } catch (e: Exception) {
                ScenarioResult.fail(id, "APDU failed after recovery: ${e.message}")
              }
            }
          },
      )
}
