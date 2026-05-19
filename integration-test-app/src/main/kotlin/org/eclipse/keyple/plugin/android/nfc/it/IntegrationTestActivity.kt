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
package org.eclipse.keyple.plugin.android.nfc.it

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConfig
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcConstants
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactoryProvider
import org.eclipse.keyple.plugin.android.nfc.it.databinding.ActivityIntegrationTestBinding
import org.eclipse.keyple.plugin.android.nfc.it.framework.AbstractModule
import org.eclipse.keyple.plugin.android.nfc.it.framework.Scenario
import org.eclipse.keyple.plugin.android.nfc.it.framework.ScenarioResult
import org.eclipse.keyple.plugin.android.nfc.it.framework.UiLog
import org.eclipse.keyple.plugin.android.nfc.it.framework.ValidationContext
import org.eclipse.keyple.plugin.android.nfc.it.module.M01_PluginDiscovery
import org.eclipse.keyple.plugin.android.nfc.it.module.M02_CardPresence
import org.eclipse.keyple.plugin.android.nfc.it.module.M03_PowerOnData
import org.eclipse.keyple.plugin.android.nfc.it.module.M04_ProtocolDetection
import org.eclipse.keyple.plugin.android.nfc.it.module.M05_ApduExchange
import org.eclipse.keyple.plugin.android.nfc.it.module.M06_CardObservation
import org.eclipse.keyple.plugin.android.nfc.it.module.M07_CardDeselect
import org.eclipse.keyple.plugin.android.nfc.it.module.M08_ObservationLifecycle
import org.eclipse.keyple.plugin.android.nfc.it.module.M09_MifareUltralight
import org.eclipse.keyple.plugin.android.nfc.it.module.M10_ErrorRecovery
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcSupportedProtocols
import org.eclipse.keypop.reader.CardReaderEvent
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import org.slf4j.LoggerFactory

class IntegrationTestActivity : AppCompatActivity(), CardReaderObserverSpi {

  private val logger = LoggerFactory.getLogger(IntegrationTestActivity::class.java)

  private lateinit var binding: ActivityIntegrationTestBinding
  private lateinit var ctx: ValidationContext
  private var scenarioThread: Thread? = null

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityIntegrationTestBinding.inflate(layoutInflater)
    setContentView(binding.root)
    val log = UiLog(this, binding.tvLog, binding.scrollView)
    ctx = ValidationContext(log, this)

    binding.btnBack.setOnClickListener {
      scenarioThread?.interrupt()
      binding.viewFlipper.displayedChild = 0
    }

    initKeyple(log)
    binding.recyclerView.layoutManager = LinearLayoutManager(this)
    binding.recyclerView.adapter = buildAdapter()
  }

  override fun onDestroy() {
    super.onDestroy()
    scenarioThread?.interrupt()
    ctx.stopDetection()
    try {
      ctx.observableReader?.removeObserver(this)
      ctx.service?.unregisterPlugin(AndroidNfcConstants.PLUGIN_NAME)
    } catch (e: Exception) {
      // Ignored: unregistering is best-effort during teardown; any exception here is
      // unrecoverable and should not prevent the activity from being destroyed.
      logger.debug("Cleanup on destroy failed — ignored: {}", e.message)
    }
  }

  // ── Keyple initialization ──────────────────────────────────────────────────

  private fun initKeyple(log: UiLog) {
    try {
      val service = SmartCardServiceProvider.getService()
      ctx.service = service // assign early so onDestroy can clean up even if a later step fails
      val config = AndroidNfcConfig(activity = this, isPlatformSoundEnabled = false)
      log.info("Config: sound=${config.isPlatformSoundEnabled}, skipNdef=${config.skipNdefCheck}")
      val factory = AndroidNfcPluginFactoryProvider.provideFactory(config)
      val plugin = service.registerPlugin(factory)
      ctx.plugin = plugin
      val rawReader = plugin.getReader(AndroidNfcConstants.READER_NAME)
      logger.info("Reader type: {}", rawReader?.javaClass?.name ?: "null")
      val reader =
          rawReader as? ObservableCardReader
              ?: error("Reader '${AndroidNfcConstants.READER_NAME}' is not an ObservableCardReader (actual type: ${rawReader?.javaClass?.name})")
      reader.setReaderObservationExceptionHandler(
          CardReaderObservationExceptionHandlerSpi { pluginName, readerName, e ->
            logger.error("Reader observation error [{}/{}]: {}", pluginName, readerName, e.message)
            log.error("Reader error [$pluginName/$readerName]: ${e.message}")
          })
      reader.addObserver(this)
      ctx.observableReader = reader
      val spi = ctx.getSpi()
      AndroidNfcSupportedProtocols.values().forEach { protocol -> spi.activateProtocol(protocol.name) }
      logger.info("Init complete — observableReader={}", reader.javaClass.simpleName)
      log.info("Plugin '${AndroidNfcConstants.PLUGIN_NAME}' registered")
      log.info("Reader '${AndroidNfcConstants.READER_NAME}' ready")
    } catch (e: Exception) {
      logger.error("initKeyple failed: {}", e.message)
      log.error("Initialization failed: ${e.message}")
    }
  }

  // ── CardReaderObserverSpi ──────────────────────────────────────────────────

  override fun onReaderEvent(event: CardReaderEvent) {
    when (event.type) {
      CardReaderEvent.Type.CARD_INSERTED -> {
        ctx.log.info("← CARD_INSERTED")
        ctx.notifyCardInserted()
      }
      CardReaderEvent.Type.CARD_REMOVED -> {
        ctx.log.info("← CARD_REMOVED")
        ctx.notifyCardRemoved()
      }
      else -> ctx.log.warn("← event: ${event.type}")
    }
  }

  // ── Scenario execution ─────────────────────────────────────────────────────

  private fun runScenario(scenario: Scenario) {
    binding.viewFlipper.displayedChild = 1
    binding.tvScenarioTitle.text = "${scenario.id}: ${scenario.title}"
    if (!ctx.isInitialized) {
      binding.tvStatus.text = "✗ FAIL"
      binding.tvStatus.setTextColor(Color.parseColor("#F44336"))
      // Do NOT clear the log — keep the initialization error visible
      ctx.log.error("Cannot run scenario: plugin not initialized (see error above)")
      return
    }
    binding.tvStatus.text = "RUNNING"
    binding.tvStatus.setTextColor(Color.parseColor("#FF9800"))
    val log = ctx.log
    log.clear()
    log.section("${scenario.id} — ${scenario.title}")
    log.info("Equipment: ${scenario.requiredEquipment}")

    scenarioThread =
        Thread {
          val result =
              try {
                ctx.resetProtocols()
                scenario.run(ctx)
              } catch (_: InterruptedException) {
                ScenarioResult.skip(scenario.id, "Cancelled")
              } catch (e: Exception) {
                ScenarioResult.fail(scenario.id, e.message ?: "Unexpected error")
              } finally {
                ctx.stopDetection()
              }
          showResult(result)
        }
    scenarioThread!!.start()
  }

  private fun showResult(result: ScenarioResult) {
    val (label, color) =
        when (result.status) {
          ScenarioResult.Status.PASS -> "✓ PASS" to Color.parseColor("#4CAF50")
          ScenarioResult.Status.FAIL -> "✗ FAIL" to Color.parseColor("#F44336")
          ScenarioResult.Status.SKIP -> "⊘ SKIP" to Color.parseColor("#FF9800")
        }
    ctx.log.info("$label — ${result.message}")
    runOnUiThread {
      binding.tvStatus.text = label
      binding.tvStatus.setTextColor(color)
    }
  }

  // ── RecyclerView adapter ───────────────────────────────────────────────────

  private sealed class ListItem {
    data class Header(val moduleId: String, val moduleTitle: String) : ListItem()

    data class ScenarioItem(val scenario: Scenario) : ListItem()
  }

  private fun buildAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder> {
    val modules: List<AbstractModule> =
        listOf(
            M01_PluginDiscovery(),
            M02_CardPresence(),
            M03_PowerOnData(),
            M04_ProtocolDetection(),
            M05_ApduExchange(),
            M06_CardObservation(),
            M07_CardDeselect(),
            M08_ObservationLifecycle(),
            M09_MifareUltralight(),
            M10_ErrorRecovery(),
        )
    val items = mutableListOf<ListItem>()
    for (m in modules) {
      items.add(ListItem.Header(m.id, m.title))
      for (s in m.scenarios) items.add(ListItem.ScenarioItem(s))
    }

    return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

      override fun getItemViewType(position: Int) = if (items[position] is ListItem.Header) 0 else 1

      override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
          val v = inflater.inflate(R.layout.item_module_header, parent, false)
          object : RecyclerView.ViewHolder(v) {}
        } else {
          val v = inflater.inflate(R.layout.item_scenario, parent, false)
          object : RecyclerView.ViewHolder(v) {}
        }
      }

      override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
          is ListItem.Header -> {
            holder.itemView.findViewById<TextView>(R.id.tvModuleTitle).text =
                "${item.moduleId} — ${item.moduleTitle}"
          }
          is ListItem.ScenarioItem -> {
            val s = item.scenario
            holder.itemView.findViewById<TextView>(R.id.tvScenarioId).text = s.id
            holder.itemView.findViewById<TextView>(R.id.tvScenarioTitle).text = s.title
            holder.itemView.findViewById<TextView>(R.id.tvScenarioEquipment).text =
                "Equipment: ${s.requiredEquipment}"
            holder.itemView.setOnClickListener { runScenario(s) }
          }
        }
      }

      override fun getItemCount() = items.size
    }
  }
}
