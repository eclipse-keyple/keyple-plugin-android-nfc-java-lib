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

import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UiLog(
    private val activity: AppCompatActivity,
    private val tvLog: TextView,
    private val scrollView: ScrollView
) {

  private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

  fun info(msg: String) = append(msg)

  fun warn(msg: String) = append("! $msg")

  fun error(msg: String) = append("✗ $msg")

  fun section(title: String) = append("─── $title ───")

  fun clear() = activity.runOnUiThread { tvLog.text = "" }

  private fun append(msg: String) {
    val line = "[${fmt.format(Date())}] $msg\n"
    activity.runOnUiThread {
      tvLog.append(line)
      scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
  }
}
