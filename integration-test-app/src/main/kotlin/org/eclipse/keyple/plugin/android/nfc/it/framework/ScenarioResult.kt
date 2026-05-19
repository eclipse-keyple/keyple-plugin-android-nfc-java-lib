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

data class ScenarioResult(val id: String, val status: Status, val message: String) {

  enum class Status {
    PASS,
    FAIL,
    SKIP
  }

  companion object {
    fun pass(id: String, message: String) = ScenarioResult(id, Status.PASS, message)

    fun fail(id: String, message: String) = ScenarioResult(id, Status.FAIL, message)

    fun skip(id: String, message: String) = ScenarioResult(id, Status.SKIP, message)
  }
}
