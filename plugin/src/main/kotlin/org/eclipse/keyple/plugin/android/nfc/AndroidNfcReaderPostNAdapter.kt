/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.android.nfc

import android.annotation.TargetApi
import android.app.Activity
import android.nfc.NfcAdapter
import android.os.Build
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterBlockingSpi
import org.slf4j.LoggerFactory

/**
 * Singleton used by the plugin to run native NFC reader on Android version >= 24 (Android N)
 *
 * It will used native features of Android NFC API to detect card removal.
 *
 * @since 1.0.0
 */
internal class AndroidNfcReaderPostNAdapter(activity: Activity) :
    AbstractAndroidNfcReaderAdapter(activity), CardRemovalWaiterBlockingSpi {

  private val logger =
      LoggerFactory.getLogger(AbstractAndroidNfcReaderAdapter.Companion::class.java)
  private var isWaitingForRemoval = false

  /** Mutex used to determine when to stop waiting for card removal */
  private val syncWaitRemoval = Object()

  /**
   * This method is called when the state machine changes to WAIT_FOR_SE_REMOVAL. Starts waiting for
   * card removal.
   *
   * Wait duration is 10 seconds max before going timeout.
   *
   * @see CardRemovalWaiterBlockingSpi.stopWaitForCardRemoval
   * @since 2.0.0
   */
  @TargetApi(Build.VERSION_CODES.N)
  override fun waitForCardRemoval() {
    logger.debug("waitForCardRemoval")
    // Check that it is not already waiting for card removal
    if (!isWaitingForRemoval) {
      isWaitingForRemoval = true

      /*
       * Listener to be called when the tag is removed from the field.
       * Note that this will only be called if the tag has been out of range for at least DEBOUNCE_MS (= 1s),
       * or if another tag came into range before DEBOUNCE_MS
       */
      val onTagRemovedListener =
          NfcAdapter.OnTagRemovedListener {
            // Card has been removed
            synchronized(syncWaitRemoval) {
              // Notify that the card has been removed -> stops waiting for card removal
              syncWaitRemoval.notify()
            }
          }

      /*
       * Signals that you are no longer interested in communicating with an NFC tag for as long as it remains in range.
       * All future attempted communication to this tag will fail with IOException
       */
      nfcAdapter?.ignore(getTagProxyTag(), DEBOUNCE_MS, onTagRemovedListener, null)

      synchronized(syncWaitRemoval) {
        /*
         * Await indefinitely for the card's removal. The wait can be shortened by calling the method `stopWaitForCardRemoval()`.
         */
        syncWaitRemoval.wait(0)
      }
    }
  }

  /**
   * Called when the state machine WAIT_FOR_SE_REMOVAL has been stopped.
   *
   * @see CardRemovalWaiterBlockingSpi.waitForCardRemoval
   * @since 2.0.0
   */
  override fun stopWaitForCardRemoval() {
    logger.debug("stopWaitForCardRemoval")
    isWaitingForRemoval = false
    synchronized(syncWaitRemoval) {
      // Notifies to stop waiting for card removal
      syncWaitRemoval.notify()
    }
  }

  companion object {
    /** Minimum amount of time the tag needs to be out of range before being dispatched again. */
    const val DEBOUNCE_MS = 1000
  }
}
