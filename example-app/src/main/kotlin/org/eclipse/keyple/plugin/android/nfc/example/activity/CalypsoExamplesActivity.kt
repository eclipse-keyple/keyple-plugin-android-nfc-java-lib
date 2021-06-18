/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.plugin.android.nfc.example.activity

import android.nfc.NfcAdapter
import android.view.MenuItem
import androidx.core.view.GravityCompat
import java.io.IOException
import kotlinx.android.synthetic.main.activity_calypso_examples.drawerLayout
import kotlinx.android.synthetic.main.activity_calypso_examples.eventRecyclerView
import kotlinx.android.synthetic.main.activity_calypso_examples.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.keyple.calypso.transaction.CalypsoPo
import org.eclipse.keyple.calypso.transaction.PoSelection
import org.eclipse.keyple.calypso.transaction.PoSelector
import org.eclipse.keyple.calypso.transaction.PoSelector.InvalidatedPo
import org.eclipse.keyple.calypso.transaction.PoTransaction
import org.eclipse.keyple.core.card.selection.CardResource
import org.eclipse.keyple.core.card.selection.CardSelectionsService
import org.eclipse.keyple.core.card.selection.CardSelector.AidSelector
import org.eclipse.keyple.core.card.selection.MultiSelectionProcessing
import org.eclipse.keyple.core.service.Reader
import org.eclipse.keyple.core.service.SmartCardService
import org.eclipse.keyple.core.service.event.AbstractDefaultSelectionsResponse
import org.eclipse.keyple.core.service.event.ObservableReader
import org.eclipse.keyple.core.service.event.ReaderEvent
import org.eclipse.keyple.core.service.exception.KeyplePluginNotFoundException
import org.eclipse.keyple.core.service.exception.KeypleReaderException
import org.eclipse.keyple.core.service.exception.KeypleReaderNotFoundException
import org.eclipse.keyple.core.service.util.ContactlessCardCommonProtocols
import org.eclipse.keyple.core.util.ByteArrayUtil
import org.eclipse.keyple.plugin.android.nfc.example.util.CalypsoClassicInfo
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcProtocolSettings
import org.eclipse.keyple.plugin.android.nfc.example.R
import timber.log.Timber

/**
 * Example of @[SmartCardService] implementation based on the @[AndroidNfcPlugin]
 *
 * By default the plugin only listens to events when your application activity is in the foreground.
 * To activate NFC events while you application is not in the foreground, add the following
 * statements to your activity definition in AndroidManifest.xml
 *
 * <intent-filter> <action android:name="android.nfc.action.TECH_DISCOVERED" /> </intent-filter>
 * <meta-data android:name="android.nfc.action.TECH_DISCOVERED" android:resource="@xml/tech_list" />
 *
 * Create a xml/tech_list.xml file in your res folder with the following content <?xml version="1.0"
 * encoding="utf-8"?> <resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"> <tech-list>
 * <tech>android.nfc.tech.IsoDep</tech> <tech>android.nfc.tech.NfcA</tech> </tech-list> </resources>
 */
class CalypsoExamplesActivity : AbstractExampleActivity() {

    private var readEnvironmentParserIndex: Int = 0

    override fun onResume() {
        super.onResume()
        try {
            checkNfcAvailability()
            if (intent.action != null && intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) run {
                configureUseCase0()

                Timber.d("Handle ACTION TECH intent")
                // notify reader that card detection has been launched
                reader.startCardDetection(ObservableReader.PollingMode.SINGLESHOT)
                initFromBackgroundTextView()
                reader.processIntent(intent)
            } else {
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                // enable detection
                reader.startCardDetection(ObservableReader.PollingMode.SINGLESHOT)
            }
        } catch (e: IOException) {
            showAlertDialog(e)
        }
    }

    override fun onPause() {
        Timber.i("on Pause Fragment - Stopping Read Write Mode")
        try {
            // notify reader that card detection has been switched off
            reader.stopCardDetection()
        } catch (e: KeyplePluginNotFoundException) {
            Timber.e(e, "NFC Plugin not found")
        }

        super.onPause()
    }

    override fun onDestroy() {
        (reader as ObservableReader).removeObserver(this)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        when (item.itemId) {
            R.id.usecase1 -> {
                clearEvents()
                configureUseCase1ExplicitSelectionAid()
            }
            R.id.usecase2 -> {
                clearEvents()
                configureUseCase2DefaultSelectionNotification()
            }
            R.id.usecase3 -> {
                clearEvents()
                configureUseCase3GroupedMultiSelection()
            }
            R.id.usecase4 -> {
                clearEvents()
                configureUseCase4SequentialMultiSelection()
            }
            R.id.start_scan -> {
                clearEvents()
                configureUseCase0()
            }
        }
        return true
    }

    override fun initContentView() {
        setContentView(R.layout.activity_calypso_examples)
        initActionBar(toolbar, "NFC Plugins", "Calypso Examples")
    }

    private fun configureUseCase4SequentialMultiSelection() {
        addHeaderEvent("UseCase Generic #4: AID based sequential explicit multiple selection")
        addHeaderEvent("Reader  NAME = ${reader.name}")

        /*Check if a card is present in the reader */
        if (reader.isCardPresent) {
            /*
              * operate card AID selection (change the AID prefix here to adapt it to the card used for
              * the test [the card should have at least two applications matching the AID prefix])
              */
            val cardAidPrefix = CalypsoClassicInfo.AID_PREFIX

            /* First selection case */
            cardSelectionsService = CardSelectionsService()

            /* AID based selection (1st selection, later indexed 0) */
            val selectionRequest1st = PoSelection(
                    PoSelector.builder()
                            .cardProtocol(AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name))
                            .aidSelector(
                                    AidSelector.builder()
                                            .aidToSelect(cardAidPrefix)
                                            .fileOccurrence(AidSelector.FileOccurrence.FIRST)
                                            .fileControlInformation(AidSelector.FileControlInformation.FCI)
                                            .build())
                            .invalidatedPo(InvalidatedPo.REJECT)
                            .build())

            cardSelectionsService.prepareSelection(selectionRequest1st)

            /* Do the selection and display the result */
            addActionEvent("FIRST MATCH Calypso PO selection for prefix: $cardAidPrefix")
            doAndAnalyseSelection(reader, cardSelectionsService, 1)

            /*
              * New selection: get the next application occurrence matching the same AID, close the
              * physical channel after
              */
            cardSelectionsService = CardSelectionsService()

            /* Close the channel after the selection */
            cardSelectionsService.prepareReleaseChannel()

            val selectionRequest2nd = PoSelection(
                    PoSelector.builder()
                            .cardProtocol(AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name))
                            .aidSelector(
                                    AidSelector.builder()
                                            .aidToSelect(cardAidPrefix)
                                            .fileOccurrence(AidSelector.FileOccurrence.NEXT)
                                            .fileControlInformation(AidSelector.FileControlInformation.FCI)
                                            .build())
                            .invalidatedPo(InvalidatedPo.REJECT)
                            .build())

            cardSelectionsService.prepareSelection(selectionRequest2nd)

            /* Do the selection and display the result */
            addActionEvent("NEXT MATCH Calypso PO selection for prefix: $cardAidPrefix")
            doAndAnalyseSelection(reader, cardSelectionsService, 2)
        } else {
            addResultEvent("No cards were detected.")
        }
        eventRecyclerView.smoothScrollToPosition(events.size - 1)
    }

    private fun doAndAnalyseSelection(reader: Reader, cardSelectionsService: CardSelectionsService, index: Int) {
        try {
            val cardSelectionsResult = cardSelectionsService.processExplicitSelections(reader)
            if (cardSelectionsResult.hasActiveSelection()) {
                val smartCard = cardSelectionsResult.activeSmartCard
                addResultEvent(getSmardCardInfos(smartCard, index))
            } else {
                addResultEvent("The selection did not match for case $index.")
            }
        } catch (e: KeypleReaderException) {
            addResultEvent("Error: ${e.message}")
        }
    }

    private fun configureUseCase3GroupedMultiSelection() {
        addHeaderEvent("UseCase Generic #3: AID based grouped explicit multiple selection")
        addHeaderEvent("Reader  NAME = ${reader.name}")

        cardSelectionsService = CardSelectionsService(MultiSelectionProcessing.PROCESS_ALL)

        /* operate card selection (change the AID here to adapt it to the card used for the test) */
        val cardAidPrefix = CalypsoClassicInfo.AID_PREFIX

        /* Close the channel after the selection to force the selection of all applications */
        cardSelectionsService.prepareReleaseChannel()

        useCase = null

        if (reader.isCardPresent) {
            /* AID based selection (1st selection, later indexed 0) */
            val selectionRequest1st = PoSelection(
                    PoSelector.builder()
                            .cardProtocol(AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name))
                            .aidSelector(
                                    AidSelector.builder()
                                            .aidToSelect(cardAidPrefix).fileOccurrence(AidSelector.FileOccurrence.FIRST)
                                            .fileControlInformation(AidSelector.FileControlInformation.FCI)
                                            .build())
                            .invalidatedPo(InvalidatedPo.REJECT)
                            .build())

            cardSelectionsService.prepareSelection(selectionRequest1st)

            /* next selection (2nd selection, later indexed 1) */
            val selectionRequest2nd = PoSelection(
                    PoSelector.builder()
                            .cardProtocol(AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name))
                            .aidSelector(
                                    AidSelector.builder()
                                            .aidToSelect(cardAidPrefix)
                                            .fileOccurrence(AidSelector.FileOccurrence.NEXT)
                                            .fileControlInformation(AidSelector.FileControlInformation.FCI)
                                            .build())
                            .invalidatedPo(InvalidatedPo.REJECT).build())

            cardSelectionsService.prepareSelection(selectionRequest2nd)

            /* next selection (3rd selection, later indexed 2) */
            val selectionRequest3rd = PoSelection(
                    PoSelector.builder()
                            .cardProtocol(AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name))
                            .aidSelector(
                                    AidSelector.builder()
                                            .aidToSelect(cardAidPrefix)
                                            .fileOccurrence(AidSelector.FileOccurrence.NEXT)
                                            .fileControlInformation(AidSelector.FileControlInformation.FCI)
                                            .build())
                            .invalidatedPo(InvalidatedPo.REJECT)
                            .build())

            cardSelectionsService.prepareSelection(selectionRequest3rd)

            addActionEvent("Calypso PO selection for prefix: $cardAidPrefix")

            /*
            * Actual card communication: operate through a single request the card selection
            */
            try {
                val selectionResult = cardSelectionsService.processExplicitSelections(reader)

                if (selectionResult.smartCards.isNotEmpty()) {
                    try {
                        selectionResult.smartCards.forEach {
                            addResultEvent(getSmardCardInfos(it.value, it.key))
                        }
                    } catch (e: IllegalStateException) {
                        showAlertDialog(e)
                    }
                    addResultEvent("End of selection")
                } else {
                    addResultEvent("No card matched the selection.")
                    addResultEvent("The card must be in the field when starting this use case")
                }
            } catch (e: KeypleReaderException) {
                addResultEvent("Error: ${e.message}")
            }
        } else {
            addResultEvent("No cards were detected.")
        }

        eventRecyclerView.smoothScrollToPosition(events.size - 1)
    }

    private fun configureUseCase2DefaultSelectionNotification() {
        addHeaderEvent("UseCase Generic #2: AID based default selection")
        addHeaderEvent("Reader  NAME = ${reader.name}")

        /*
        * Prepare a a new Calypso PO selection
        */
        cardSelectionsService = CardSelectionsService()

        val aid = CalypsoClassicInfo.AID

        /*
        * Setting of an AID based selection
        *
        * Select the first application matching the selection AID whatever the card communication
        * protocol keep the logical channel open after the selection
        */

        /*
         * Generic selection: configures a CardSelector with all the desired attributes to make the
         * selection
         */
        val cardSelectionRequest = PoSelection(
                PoSelector.builder()
                        .cardProtocol(AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name))
                        .aidSelector(
                                AidSelector.builder()
                                        .aidToSelect(aid)
                                        .build())
                        .invalidatedPo(InvalidatedPo.REJECT)
                        .build())

        /*
        * Add the selection case to the current selection (we could have added other cases here)
        */
        cardSelectionsService.prepareSelection(cardSelectionRequest)

        /*
         * Provide the Reader with the selection operation to be processed when a card is inserted.
         */
        (reader as ObservableReader).setDefaultSelectionRequest(cardSelectionsService.defaultSelectionsRequest,
                ObservableReader.NotificationMode.MATCHED_ONLY, ObservableReader.PollingMode.REPEATING)

        // (reader as ObservableReader).addObserver(this) //ALready done in onCreate

        addActionEvent("Waiting for a card... The default AID based selection to be processed as soon as the card is detected.")

        useCase = object : UseCase {
            override fun onEventUpdate(event: ReaderEvent?) {
                CoroutineScope(Dispatchers.Main).launch {
                    when (event?.eventType) {
                        ReaderEvent.EventType.CARD_MATCHED -> {
                            addResultEvent("CARD_MATCHED event: A card corresponding to request has been detected")
                            val selectedCard = cardSelectionsService.processDefaultSelectionsResponse(event.defaultSelectionsResponse).activeSmartCard
                            if (selectedCard != null) {
                                addResultEvent("Observer notification: the selection of the card has succeeded. End of the card processing.")
                                addResultEvent("Application FCI = ${ByteArrayUtil.toHex(selectedCard.fciBytes)}")
                            } else {
                                addResultEvent("The selection of the card has failed. Should not have occurred due to the MATCHED_ONLY selection mode.")
                            }
                        }

                        ReaderEvent.EventType.CARD_INSERTED -> {
                            addResultEvent("CARD_INSERTED event: should not have occurred due to the MATCHED_ONLY selection mode.")
                        }

                        ReaderEvent.EventType.CARD_REMOVED -> {
                            addResultEvent("CARD_REMOVED event: There is no PO inserted anymore. Return to the waiting state...")
                        }

                        else -> {
                        }
                    }
                    eventRecyclerView.smoothScrollToPosition(events.size - 1)
                }
                if (event?.eventType == ReaderEvent.EventType.CARD_INSERTED || event?.eventType == ReaderEvent.EventType.CARD_MATCHED) {
                    /*
                     * Informs the underlying layer of the end of the card processing, in order to manage the
                     * removal sequence. <p>If closing has already been requested, this method will do
                     * nothing.
                     */
                    try {
                        (event.reader as ObservableReader).finalizeCardProcessing()
                    } catch (e: KeypleReaderNotFoundException) {
                        Timber.e(e)
                        addResultEvent("Error: ${e.message}")
                    } catch (e: KeyplePluginNotFoundException) {
                        Timber.e(e)
                        addResultEvent("Error: ${e.message}")
                    }
                }
                eventRecyclerView.smoothScrollToPosition(events.size - 1)
            }
        }
    }

    private fun configureUseCase1ExplicitSelectionAid() {
        addHeaderEvent("UseCase Generic #1: Explicit AID selection")
        addHeaderEvent("Reader  NAME = ${reader.name}")

        /*
        * Prepare a a new Calypso PO selection
        */
        cardSelectionsService = CardSelectionsService()

        /* Close the channel after the selection */
        cardSelectionsService.prepareReleaseChannel()

        val aid = CalypsoClassicInfo.AID

        if (reader.isCardPresent) {
            /**
             * configure Protocol
             */
            val cardSelectionRequest = PoSelection(
                    PoSelector.builder()
                            .cardProtocol(AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name))
                            .aidSelector(
                                    AidSelector.builder()
                                            .aidToSelect(aid)
                                            .build())
                            .invalidatedPo(InvalidatedPo.REJECT)
                            .build())

            /**
             * Prepare Selection
             */
            cardSelectionsService.prepareSelection(cardSelectionRequest)

            /*
             * Provide the Reader with the selection operation to be processed when a card is inserted.
             */
            (reader as ObservableReader).setDefaultSelectionRequest(cardSelectionsService.defaultSelectionsRequest,
                    ObservableReader.NotificationMode.MATCHED_ONLY, ObservableReader.PollingMode.SINGLESHOT)

            /**
             * We won't be listening for event update within this use case
             */
            useCase = null

            addActionEvent("Calypso PO selection: $aid")
            try {
                val cardSelectionsResult = cardSelectionsService.processExplicitSelections(reader)

                if (cardSelectionsResult.hasActiveSelection()) {
                    val matchedCard = cardSelectionsResult.activeSmartCard
                    addResultEvent("The selection of the card has succeeded.")
                    addResultEvent("Application FCI = ${ByteArrayUtil.toHex(matchedCard.fciBytes)}")
                    addResultEvent("End of the generic card processing.")
                } else {
                    addResultEvent("The selection of the card has failed.")
                }
            } catch (e: KeypleReaderException) {
                Timber.e(e)
                addResultEvent("Error: ${e.message}")
            }
        } else {
            addResultEvent("No cards were detected.")
            addResultEvent("The card must be in the field when starting this use case")
        }
        eventRecyclerView.smoothScrollToPosition(events.size - 1)
    }

    private fun configureUseCase0() {
        // define task as an observer for ReaderEvents
        /*
         * Prepare a a new Calypso PO selection
         */
        cardSelectionsService = CardSelectionsService()

        /*
             * Setting of an AID based selection of a Calypso REV3 PO
             *
             * Select the first application matching the selection AID whatever the card communication
             * protocol keep the logical channel open after the selection
             */

        /*
             * Calypso selection: configures a PoSelector with all the desired attributes to make
             * the selection and read additional information afterwards
             */
        val poSelection = PoSelection(
                PoSelector.builder()
                        .cardProtocol(AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name))
                        .aidSelector(
                                AidSelector.builder()
                                        .aidToSelect(CalypsoClassicInfo.AID)
                                        .build())
                        .invalidatedPo(InvalidatedPo.REJECT)
                        .build())

        /*
             * Prepare the reading order and keep the associated parser for later use once the
             * selection has been made.
             */
        poSelection.prepareReadRecordFile(
                CalypsoClassicInfo.SFI_EnvironmentAndHolder,
                CalypsoClassicInfo.RECORD_NUMBER_1.toInt())

        /*
         * Add the selection case to the current selection (we could have added other cases
         * here)
         */
        cardSelectionsService.prepareSelection(poSelection)

        /*
             * Provide the Reader with the selection operation to be processed when a PO is
             * inserted.
             */
        (reader as ObservableReader).setDefaultSelectionRequest(
                cardSelectionsService.defaultSelectionsRequest, ObservableReader.NotificationMode.ALWAYS)

        // uncomment to active protocol listening for Mifare ultralight
        // reader.addSeProtocolSetting(SeCommonProtocols.PROTOCOL_MIFARE_UL, AndroidNfcProtocolSettings.getSetting(SeCommonProtocols.PROTOCOL_MIFARE_UL))
        reader.activateProtocol(ContactlessCardCommonProtocols.ISO_14443_4.name,
                AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name))

        // uncomment to active protocol listening for Mifare ultralight
        // reader.addSeProtocolSetting(SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC, AndroidNfcProtocolSettings.getSetting(SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC))
        // reader.activateProtocol(ContactlessCardCommonProtocols.ISO_14443_4.name,
        //        AndroidNfcProtocolSettings.getSetting(ContactlessCardCommonProtocols.ISO_14443_4.name))

        useCase = object : UseCase {
            override fun onEventUpdate(event: ReaderEvent?) {
                CoroutineScope(Dispatchers.Main).launch {
                    when (event?.eventType) {
                        ReaderEvent.EventType.CARD_MATCHED -> {
                            addResultEvent("Tag detected - card MATCHED")
                            executeCommands(event.defaultSelectionsResponse)
                            reader.finalizeCardProcessing()
                        }

                        ReaderEvent.EventType.CARD_INSERTED -> {
                            addResultEvent("PO detected but AID didn't match with ${CalypsoClassicInfo.AID}")
                            reader.finalizeCardProcessing()
                        }

                        ReaderEvent.EventType.CARD_REMOVED -> {
                            addResultEvent("Tag detected - card CARD_REMOVED")
                        }

                        ReaderEvent.EventType.UNREGISTERED -> {
                            addResultEvent("Unexpected error - reader is UNREGISTERED")
                        }
                    }
                }
                eventRecyclerView.smoothScrollToPosition(events.size - 1)
            }
        }
        // notify reader that card detection has been launched
        reader.startCardDetection(ObservableReader.PollingMode.REPEATING)
    }

    /**
     * Run Calypso simple read transaction
     *
     * @param defaultSelectionsResponse
     */
    private fun executeCommands(
        defaultSelectionsResponse: AbstractDefaultSelectionsResponse
    ) {

        // addHeaderEvent("Running Calypso Simple Read transaction")

        try {
            /*
             * print tag info in View
             */
            addHeaderEvent("Tag Id : ${reader.printTagId()}")
            val cardSelectionsResult = cardSelectionsService.processDefaultSelectionsResponse(defaultSelectionsResponse)
            addResultEvent("1st PO exchange: aid selection")

            if (cardSelectionsResult.hasActiveSelection()) {
                val calypsoPo = cardSelectionsResult.activeSmartCard as CalypsoPo

                addResultEvent("Calypso PO selection: ")
                addResultEvent("AID: ${ByteArrayUtil.fromHex(CalypsoClassicInfo.AID)}")

                /*
                 * Retrieve the data read from the parser updated during the selection process
                 */

                val environmentAndHolder = calypsoPo.getFileBySfi(CalypsoClassicInfo.SFI_EnvironmentAndHolder).data.content
                addResultEvent("Environment file data: ${ByteArrayUtil.toHex(environmentAndHolder)}")

                addResultEvent("2nd PO exchange: read the event log file")
                val poTransaction = PoTransaction(CardResource(reader, calypsoPo))

                /*
                 * Prepare the reading order and keep the associated parser for later use once the
                 * transaction has been processed.
                 */
                poTransaction.prepareReadRecordFile(
                        CalypsoClassicInfo.SFI_EventLog,
                        CalypsoClassicInfo.RECORD_NUMBER_1.toInt())

                /*
                 * Actual PO communication: send the prepared read order, then close the channel
                 * with the PO
                 */
                addActionEvent("processPoCommands")
                poTransaction.prepareReleasePoChannel()
                poTransaction.processPoCommands()
                addResultEvent("SUCCESS")

                /*
                 * Retrieve the data read from the parser updated during the transaction process
                 */
                val eventLog = calypsoPo.getFileBySfi(CalypsoClassicInfo.SFI_EventLog).data.content

                /* Log the result */
                addResultEvent("EventLog file: ${ByteArrayUtil.toHex(eventLog)}")
                addResultEvent("End of the Calypso PO processing.")
                addResultEvent("You can remove the card now")
            } else {
                addResultEvent("The selection of the PO has failed. Should not have occurred due to the MATCHED_ONLY selection mode.")
            }
        } catch (e: KeypleReaderException) {
            Timber.e(e)
            addResultEvent("Exception: ${e.message}")
        } catch (e: Exception) {
            Timber.e(e)
            addResultEvent("Exception: ${e.message}")
        }
    }

    override fun update(event: ReaderEvent?) {
        Timber.i("New ReaderEvent received : $event")
        useCase?.onEventUpdate(event)
    }
}
