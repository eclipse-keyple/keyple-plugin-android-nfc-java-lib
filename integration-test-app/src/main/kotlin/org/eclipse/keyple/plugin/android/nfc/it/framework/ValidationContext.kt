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
package org.eclipse.keyple.plugin.android.nfc.it.framework

import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.SmartCardService
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConstants
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keypop.reader.ObservableCardReader
import org.slf4j.LoggerFactory

/**
 * Shared state passed to every scenario. Coordinates card events between the UI thread (NFC
 * callbacks) and scenario threads (blocking waits).
 */
class ValidationContext(val log: UiLog, private val activity: AppCompatActivity) {

  private val logger = LoggerFactory.getLogger(ValidationContext::class.java)

  var service: SmartCardService? = null
  var plugin: Plugin? = null
  var observableReader: ObservableCardReader? = null

  val isInitialized: Boolean
    get() = service != null && plugin != null

  private var cardInsertedLatch = CountDownLatch(1)
  private var cardRemovedLatch = CountDownLatch(1)

  /**
   * Returns the reader SPI via the plugin's reader extension, cast to [ConfigurableReaderSpi].
   * Provides access to isCardPresent(), transmitApdu(), getPowerOnData(), activateProtocol(), etc.
   */
  fun getSpi(): ConfigurableReaderSpi {
    val p = plugin ?: error("Plugin not initialized")
    return p.getReaderExtension(AndroidNfcReader::class.java, AndroidNfcConstants.READER_NAME)
        as ConfigurableReaderSpi
  }

  /** Returns the reader SPI cast to [ObservableReaderSpi] for access to deselectCard(). */
  fun getObsSpi(): ObservableReaderSpi = getSpi() as ObservableReaderSpi

  /**
   * Reactivates all supported protocols on the reader SPI, restoring a known-good state before
   * each scenario. Call this at the start of every test to guarantee protocol independence: a test
   * that called [ConfigurableReaderSpi.deactivateProtocol] (even in a finally block that was
   * skipped on cancellation) cannot silently corrupt the next test's detection flags.
   */
  fun resetProtocols() {
    if (!isInitialized) return
    val spi = getSpi()
    AndroidNfcSupportedProtocols.values().forEach { spi.activateProtocol(it.name) }
    logger.debug("Protocols reset — all protocols active")
  }

  /**
   * Logs [prompt], starts card detection in SINGLESHOT mode, and blocks until a card is detected
   * or the timeout expires. Returns true if a card was detected, false on timeout.
   */
  fun awaitTap(prompt: String = "Tap a card on the reader...", timeoutSec: Long = 30): Boolean {
    cardInsertedLatch = CountDownLatch(1)
    log.info(prompt)
    activity.runOnUiThread {
      if (observableReader == null) {
        logger.error("awaitTap: observableReader is null — startCardDetection skipped")
        log.error("observableReader is null — detection cannot start")
      } else {
        try {
          logger.info("awaitTap: calling startCardDetection(REPEATING) on {}",
              observableReader!!.javaClass.name)
          observableReader!!.startCardDetection(ObservableCardReader.DetectionMode.REPEATING)
          log.info("Detection active (REPEATING, timeout ${timeoutSec}s)")
        } catch (e: Exception) {
          logger.error("startCardDetection failed: {} — {}", e::class.java.simpleName, e.message)
          log.error("startCardDetection failed: ${e.message}")
        }
      }
    }
    val detected = cardInsertedLatch.await(timeoutSec, TimeUnit.SECONDS)
    if (!detected) {
      log.warn("Tap timeout after ${timeoutSec}s — no card detected")
    } else {
      // Called from the scenario thread (outside Keyple's event dispatch) so the state machine
      // can safely transition to WAIT_FOR_CARD_REMOVAL and start polling for tag removal.
      observableReader?.finalizeCardProcessing()
    }
    return detected
  }

  /**
   * Blocks until a card removal event is received or the timeout expires. Returns true if removal
   * was detected.
   */
  fun awaitRemoval(
      prompt: String = "Remove the card from the reader...",
      timeoutSec: Long = 30
  ): Boolean {
    cardRemovedLatch = CountDownLatch(1)
    log.info(prompt)
    val removed = cardRemovedLatch.await(timeoutSec, TimeUnit.SECONDS)
    if (!removed) log.warn("Removal timeout after ${timeoutSec}s — card still present?")
    return removed
  }

  /** Stops card detection (safe to call even if detection is not active). */
  fun stopDetection() {
    activity.runOnUiThread {
      try {
        observableReader?.stopCardDetection()
        log.info("Detection stopped")
      } catch (e: Exception) {
        // Ignored: stopCardDetection() may throw if NFC was already disabled by the OS;
        // cleanup is best-effort and no recovery is possible here.
        logger.debug("stopCardDetection() failed during cleanup — ignored: {}", e.message)
      }
    }
  }

  fun notifyCardInserted() = cardInsertedLatch.countDown()

  fun notifyCardRemoved() = cardRemovedLatch.countDown()
}
