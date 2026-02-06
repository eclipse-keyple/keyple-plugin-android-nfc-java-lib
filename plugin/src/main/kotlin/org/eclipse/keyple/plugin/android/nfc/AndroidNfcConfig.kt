/* **************************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
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
import org.eclipse.keyple.core.plugin.storagecard.ApduInterpreterFactory
import org.eclipse.keyple.plugin.android.nfc.spi.KeyProvider

/**
 * Configuration class holding all the plugin options.
 *
 * @property activity The activity context in which NFC operations will occur.
 * @property apduInterpreterFactory (optional, default value: `null`) The `ApduInterpreterFactory`
 *   dedicated to the management of storage cards. The interface of this factory is provided by the
 *   `keyple-plugin-storage-card-java-api` API, its implementation should be provided.
 * @property isPlatformSoundEnabled (optional, default value: `true`) When `true`, the platform will
 *   play sounds on tag discovery (corresponds to FLAG_READER_NO_PLATFORM_SOUNDS).
 * @property skipNdefCheck (optional, default value: `true`) When true, the NFC adapter will skip
 *   the NDEF check on discovered tags (corresponds to FLAG_READER_SKIP_NDEF_CHECK).
 * @property cardInsertionPollingInterval (optional, default value: `0`) Delay (in milliseconds) for
 *   performing presence checks while waiting for card insertion. Use 0 to rely on the default
 *   behavior (corresponds to EXTRA_READER_PRESENCE_CHECK_DELAY).
 * @property cardRemovalPollingInterval (optional, default value: `100`) Delay (in milliseconds) for
 *   performing presence checks while waiting for card removal.
 * @property keyProvider (optional, default value: `null`) A provider for retrieving keys during
 *   authentication.
 * @since 3.0.0
 */
data class AndroidNfcConfig(
    val activity: Activity,
    val apduInterpreterFactory: ApduInterpreterFactory? = null,
    val isPlatformSoundEnabled: Boolean = true,
    val skipNdefCheck: Boolean = true,
    val cardInsertionPollingInterval: Int = 0,
    val cardRemovalPollingInterval: Int = 100,
    val keyProvider: KeyProvider? = null
)
