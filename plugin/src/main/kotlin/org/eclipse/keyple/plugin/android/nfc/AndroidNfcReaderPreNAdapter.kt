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

import android.app.Activity
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalNonBlockingSpi

/**
 * Singleton used by the plugin to run native NFC reader on Android version < 24 (Android N).
 *
 * It uses a Ping monitoring job to detect card removal
 *
 * @since 2.0
 */
internal class AndroidNfcReaderPreNAdapter(activity: Activity) : AbstractAndroidNfcReaderAdapter(activity), WaitForCardRemovalNonBlockingSpi
