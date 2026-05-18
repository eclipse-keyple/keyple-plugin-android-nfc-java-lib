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
import org.eclipse.keyple.plugin.android.nfc.it.framework.ScenarioResult
import org.eclipse.keyple.plugin.android.nfc.it.framework.ValidationContext

/** M05 - APDU exchange on ISO 14443-4 cards (transmitApdu). */
class M05_ApduExchange : AbstractModule("M05", "APDU Exchange") {

  private val GET_CHALLENGE = HexUtil.toByteArray("0084000008")
  private val SELECT_MF = HexUtil.toByteArray("00A4000000")

  private fun sw(resp: ByteArray): String =
      if (resp.size >= 2) HexUtil.toHex(resp.copyOfRange(resp.size - 2, resp.size)) else "??"

  private fun isOk(resp: ByteArray): Boolean =
      resp.size >= 2 && resp[resp.size - 2] == 0x90.toByte() && resp[resp.size - 1] == 0x00.toByte()

  override val scenarios =
      listOf<Scenario>(
          object : Scenario {
            override val id = "M05.1"
            override val title = "GET CHALLENGE (SW=9000)"
            override val requiredEquipment = "ISO 14443-4 card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
              if (!ctx.awaitTap("Tap an ISO 14443-4 card..."))
                  return ScenarioResult.skip(id, "Timeout: no card detected")
              return try {
                val cmd = GET_CHALLENGE
                ctx.log.info("CMD: ${HexUtil.toHex(cmd)}")
                val resp = spi.transmitApdu(cmd)
                ctx.log.info("RSP: ${HexUtil.toHex(resp)}  SW=${sw(resp)}")
                ctx.getObsSpi().deselectCard()
                ctx.awaitRemoval()
                spi.deactivateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                if (isOk(resp)) ScenarioResult.pass(id, "GET CHALLENGE SW=${sw(resp)}")
                else ScenarioResult.fail(id, "Unexpected SW: ${sw(resp)}")
              } catch (e: Exception) {
                ScenarioResult.fail(id, e.message ?: "APDU failed")
              }
            }
          },
          object : Scenario {
            override val id = "M05.2"
            override val title = "SELECT Master File (00 A4 00 00)"
            override val requiredEquipment = "ISO 14443-4 card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
              if (!ctx.awaitTap("Tap an ISO 14443-4 card..."))
                  return ScenarioResult.skip(id, "Timeout: no card detected")
              return try {
                val cmd = SELECT_MF
                ctx.log.info("CMD: ${HexUtil.toHex(cmd)}")
                val resp = spi.transmitApdu(cmd)
                ctx.log.info("RSP: ${HexUtil.toHex(resp)}  SW=${sw(resp)}")
                ctx.getObsSpi().deselectCard()
                ctx.awaitRemoval()
                spi.deactivateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                ScenarioResult.pass(id, "SELECT MF SW=${sw(resp)}")
              } catch (e: Exception) {
                ScenarioResult.fail(id, e.message ?: "APDU failed")
              }
            }
          },
          object : Scenario {
            override val id = "M05.3"
            override val title = "Sequential APDUs: 3x GET CHALLENGE"
            override val requiredEquipment = "ISO 14443-4 card"

            override fun run(ctx: ValidationContext): ScenarioResult {
              if (!ctx.isInitialized) return ScenarioResult.fail(id, "Plugin not initialized")
              val spi = ctx.getSpi()
              spi.activateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
              if (!ctx.awaitTap("Tap an ISO 14443-4 card..."))
                  return ScenarioResult.skip(id, "Timeout: no card detected")
              return try {
                var ok = 0
                for (i in 1..3) {
                  val resp = spi.transmitApdu(GET_CHALLENGE)
                  ctx.log.info("APDU $i: SW=${sw(resp)}")
                  if (isOk(resp)) ok++
                }
                ctx.getObsSpi().deselectCard()
                ctx.awaitRemoval()
                spi.deactivateProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                if (ok == 3) ScenarioResult.pass(id, "3/3 sequential APDUs succeeded")
                else ScenarioResult.fail(id, "$ok/3 APDUs succeeded")
              } catch (e: Exception) {
                ScenarioResult.fail(id, e.message ?: "APDU failed")
              }
            }
          },
      )
}
