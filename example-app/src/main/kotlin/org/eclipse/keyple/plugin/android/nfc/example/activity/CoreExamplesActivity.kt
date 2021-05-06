/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.android.nfc.example.activity

import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.GravityCompat
import java.io.IOException
import kotlinx.android.synthetic.main.activity_core_examples.drawerLayout
import kotlinx.android.synthetic.main.activity_core_examples.eventRecyclerView
import kotlinx.android.synthetic.main.activity_core_examples.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.keyple.card.generic.GenericExtensionServiceProvider
import org.eclipse.keyple.core.service.CardSelectionServiceFactory
import org.eclipse.keyple.core.service.KeypleCardCommunicationException
import org.eclipse.keyple.core.service.KeypleReaderCommunicationException
import org.eclipse.keyple.core.service.ObservableReader
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.ReaderEvent
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.core.service.selection.CardSelectionService
import org.eclipse.keyple.core.service.selection.CardSelector
import org.eclipse.keyple.core.service.selection.spi.SmartCard
import org.eclipse.keyple.core.util.ByteArrayUtil
import org.eclipse.keyple.core.util.protocol.ContactlessCardCommonProtocol
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryAdapter
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keyple.plugin.android.nfc.example.R
import org.eclipse.keyple.plugin.android.nfc.example.util.CalypsoClassicInfo
import timber.log.Timber

/**
 * Examples of Keyple API usage relying on keyple-java-plugin-android-nfc
 */
class CoreExamplesActivity : AbstractExampleActivity() {

    protected lateinit var reader: Reader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * Register AndroidNfc plugin Factory
         */
        val plugin = SmartCardServiceProvider.getService().registerPlugin(AndroidNfcPluginFactoryAdapter(this))

        /**
         * Configure Nfc Reader
         */
        with(plugin.getReader(AndroidNfcReader.READER_NAME) as ObservableReader) {
            setReaderObservationExceptionHandler(this@CoreExamplesActivity)
            addObserver(this@CoreExamplesActivity)

            // with this protocol settings we activate the nfc for ISO1443_4 protocol
            activateProtocol(ContactlessCardCommonProtocol.ISO_14443_4.name, ContactlessCardCommonProtocol.ISO_14443_4.name)
            reader = this
        }
    }

    override fun onDestroy() {
        SmartCardServiceProvider.getService().unregisterPlugin(AndroidNfcPlugin.PLUGIN_NAME)
        super.onDestroy()
    }

    override fun initContentView() {
        setContentView(R.layout.activity_core_examples)
        initActionBar(toolbar, "NFC Plugins", "Core Examples")
    }

    override fun onResume() {
        super.onResume()
        try {
            checkNfcAvailability()
            if (intent.action != null && intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) run {

                Timber.d("Handle ACTION TECH intent")
                // notify reader that card detection has been launched
                (reader as ObservableReader).startCardDetection(ObservableReader.PollingMode.SINGLESHOT)
                initFromBackgroundTextView()
                (reader as AndroidNfcReader).processIntent(intent)
                configureUseCase1ExplicitSelectionAid()
            } else {
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                // enable detection
                (reader as ObservableReader).startCardDetection(ObservableReader.PollingMode.SINGLESHOT)
            }
        } catch (e: IOException) {
            showAlertDialog(e)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        when (item.itemId) {
            R.id.usecase2 -> {
                clearEvents()
                (reader as ObservableReader).startCardDetection(ObservableReader.PollingMode.REPEATING)
                configureUseCase2DefaultSelectionNotification()
            }
        }
        return true
    }

    private fun doAndAnalyseSelection(reader: Reader?, cardSelectionsService: CardSelectionService, index: Int) {
        try {
            val cardSelectionsResult = cardSelectionsService.processCardSelectionScenario(reader)
            if (cardSelectionsResult.hasActiveSelection()) {
                val smartCard = cardSelectionsResult.activeSmartCard
                addResultEvent(getSmardCardInfos(smartCard, index))
            } else {
                addResultEvent("The selection did not match for case $index.")
            }
        } catch (e: KeypleCardCommunicationException) {
            addResultEvent("Error: ${e.message}")
        } catch (e: KeypleReaderCommunicationException) {
            addResultEvent("Error: ${e.message}")
        }
    }

    private fun configureUseCase2DefaultSelectionNotification() {
        addHeaderEvent("UseCase Generic #2: AID based default selection")

        with(reader as ObservableReader) {

            addHeaderEvent("Reader  NAME = $name")

            /**
             * Prepare a card selection
             */
            cardSelectionsService = CardSelectionServiceFactory.getService()

            /**
             * Setting of an AID based selection
             *
             * Select the first application matching the selection AID whatever the card communication
             * protocol keep the logical channel open after the selection
             */
            val aid = CalypsoClassicInfo.AID_CD_LIGHT_GTML

            /**
             * Generic selection: configures a CardSelector with all the desired attributes to make the
             * selection
             */
            val cardSelector = CardSelector
                    .builder()
                    .filterByDfName(aid)
                    .filterByCardProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                    .build()

            /**
             * Add the selection case to the current selection (we could have added other cases here)
             */
            cardSelectionsService.prepareSelection(GenericExtensionServiceProvider.getService().createCardSelection(cardSelector))

            cardSelectionsService.scheduleCardSelectionScenario(reader as ObservableReader, ObservableReader.NotificationMode.MATCHED_ONLY)

            useCase = object : UseCase {
                override fun onEventUpdate(event: ReaderEvent?) {
                    CoroutineScope(Dispatchers.Main).launch {
                        when (event?.eventType) {
                            ReaderEvent.EventType.CARD_MATCHED -> {
                                addResultEvent("CARD_MATCHED event: A card corresponding to request has been detected")
                                val selectedCard = cardSelectionsService.parseScheduledCardSelectionsResponse(event.scheduledCardSelectionsResponse).activeSmartCard
                                if (selectedCard != null) {
                                    addResultEvent("Observer notification: the selection of the card has succeeded. End of the card processing.")
                                    addResultEvent("Application FCI = ${ByteArrayUtil.toHex(selectedCard.fciBytes)}")
                                } else {
                                    addResultEvent("The selection of the card has failed. Should not have occurred due to the MATCHED_ONLY selection mode.")
                                }
                                (reader as ObservableReader).finalizeCardProcessing()
                            }

                            ReaderEvent.EventType.CARD_INSERTED -> {
                                addResultEvent("CARD_INSERTED event: should not have occurred due to the MATCHED_ONLY selection mode.")
                                (reader as ObservableReader).finalizeCardProcessing()
                            }

                            ReaderEvent.EventType.CARD_REMOVED -> {
                                addResultEvent("CARD_REMOVED event: There is no PO inserted anymore. Return to the waiting state...")
                            }

                            else -> {
                            }
                        }
                        eventRecyclerView.smoothScrollToPosition(events.size - 1)
                    }
                    eventRecyclerView.smoothScrollToPosition(events.size - 1)
                }
            }
            addActionEvent("Waiting for a card... The default AID based selection to be processed as soon as the card is detected.")
        }
    }

    private fun configureUseCase1ExplicitSelectionAid() {
        addHeaderEvent("UseCase Generic #1: Explicit AID selection")

        with(reader as ObservableReader) {
            addHeaderEvent("Reader  NAME = $name")

            if (isCardPresent) {

                val smartCardService = SmartCardServiceProvider.getService()

                /**
                 * Get the generic card extension service
                 */
                val cardExtension = GenericExtensionServiceProvider.getService()

                /**
                 * Verify that the extension's API level is consistent with the current service.
                 */
                smartCardService.checkCardExtension(cardExtension)

                /**
                 * Prepare the card selection
                 */
                cardSelectionsService = CardSelectionServiceFactory.getService()

                /**
                 * Setting of an AID based selection (in this example a Calypso REV3 PO)
                 *
                 * Select the first application matching the selection AID whatever the card communication
                 * protocol keep the logical channel open after the selection
                 */
                val aid = CalypsoClassicInfo.AID_CD_LIGHT_GTML

                /**
                 * Generic selection: configures a CardSelector with all the desired attributes to make
                 * the selection and read additional information afterwards
                 */
                val cardSelector = CardSelector
                        .builder()
                        .filterByDfName(aid)
                        .filterByCardProtocol(AndroidNfcSupportedProtocols.ISO_14443_4.name)
                        .build()

                /**
                 * Create a card selection using the generic card extension.
                 */
                val cardSelection = cardExtension.createCardSelection(cardSelector)

                /**
                 * Prepare Selection
                 */
                cardSelectionsService.prepareSelection(cardSelection)
                /**
                 * Provide the Reader with the selection operation to be processed when a card is inserted.
                 */
                cardSelectionsService.scheduleCardSelectionScenario(reader as ObservableReader, ObservableReader.NotificationMode.MATCHED_ONLY, ObservableReader.PollingMode.SINGLESHOT)

                /**
                 * We won't be listening for event update within this use case
                 */
                useCase = null

                addActionEvent("Calypso PO selection: $aid")
                try {
                    val cardSelectionsResult = cardSelectionsService.processCardSelectionScenario(this)

                    if (cardSelectionsResult.hasActiveSelection()) {
                        val matchedCard = cardSelectionsResult.activeSmartCard
                        addResultEvent("The selection of the card has succeeded.")
                        addResultEvent("Application FCI = ${ByteArrayUtil.toHex(matchedCard.fciBytes)}")
                        addResultEvent("End of the generic card processing.")
                    } else {
                        addResultEvent("The selection of the card has failed.")
                    }
                    (reader as ObservableReader).finalizeCardProcessing()
                } catch (e: KeypleCardCommunicationException) {
                    addResultEvent("Error: ${e.message}")
                } catch (e: KeypleReaderCommunicationException) {
                    addResultEvent("Error: ${e.message}")
                }
            } else {
                addResultEvent("No cards were detected.")
                addResultEvent("The card must be in the field when starting this use case")
            }
            eventRecyclerView.smoothScrollToPosition(events.size - 1)
        }
    }

    private fun getSmardCardInfos(smartCard: SmartCard, index: Int): String {
        val atr = try {
            ByteArrayUtil.toHex(smartCard.atrBytes)
        } catch (e: IllegalStateException) {
            Timber.w(e)
            e.message
        }
        val fci = try {
            ByteArrayUtil.toHex(smartCard.fciBytes)
        } catch (e: IllegalStateException) {
            Timber.w(e)
            e.message
        }

        return "Selection status for selection " +
                "(indexed $index): \n\t\t" +
                "ATR: ${atr}\n\t\t" +
                "FCI: $fci"
    }

    override fun onReaderEvent(readerEvent: ReaderEvent) {
        Timber.i("New ReaderEvent received : ${readerEvent.eventType}")
        useCase?.onEventUpdate(readerEvent)
    }
}
