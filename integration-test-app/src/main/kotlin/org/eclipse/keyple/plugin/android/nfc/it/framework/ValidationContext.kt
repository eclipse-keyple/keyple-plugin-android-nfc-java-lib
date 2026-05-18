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
import org.eclipse.keypop.reader.ObservableCardReader

/**
 * Shared state passed to every scenario. Coordinates card events between the UI thread (NFC
 * callbacks) and scenario threads (blocking waits).
 */
class ValidationContext(val log: UiLog, private val activity: AppCompatActivity) {

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
   * Logs [prompt], starts card detection in SINGLESHOT mode, and blocks until a card is detected
   * or the timeout expires. Returns true if a card was detected, false on timeout.
   */
  fun awaitTap(prompt: String = "Tap a card on the reader...", timeoutSec: Long = 30): Boolean {
    cardInsertedLatch = CountDownLatch(1)
    log.info(prompt)
    activity.runOnUiThread {
      observableReader?.startCardDetection(ObservableCardReader.DetectionMode.SINGLESHOT)
    }
    return cardInsertedLatch.await(timeoutSec, TimeUnit.SECONDS)
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
    return cardRemovedLatch.await(timeoutSec, TimeUnit.SECONDS)
  }

  /** Stops card detection (safe to call even if detection is not active). */
  fun stopDetection() {
    activity.runOnUiThread {
      try {
        observableReader?.stopCardDetection()
      } catch (_: Exception) {}
    }
  }

  fun notifyCardInserted() = cardInsertedLatch.countDown()

  fun notifyCardRemoved() = cardRemovedLatch.countDown()
}
