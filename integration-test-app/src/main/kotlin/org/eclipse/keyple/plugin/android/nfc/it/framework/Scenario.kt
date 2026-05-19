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

interface Scenario {

  companion object {
    const val ERR_NOT_INITIALIZED = "Plugin not initialized"
    const val ERR_TIMEOUT_NO_CARD = "Timeout: no card detected"
  }

  val id: String
  val title: String
  val requiredEquipment: String

  fun run(ctx: ValidationContext): ScenarioResult

  /**
   * Returns a FAIL [ScenarioResult] if the plugin is not initialized, null otherwise.
   * Call at the top of every [run] implementation to guard against missing initialization.
   */
  fun requireInitialized(ctx: ValidationContext): ScenarioResult? =
      if (!ctx.isInitialized) ScenarioResult.fail(id, ERR_NOT_INITIALIZED) else null
}
