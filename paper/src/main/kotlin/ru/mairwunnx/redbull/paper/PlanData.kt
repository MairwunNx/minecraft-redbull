package ru.mairwunnx.redbull.paper

import com.djrapitops.plan.capability.CapabilityService
import com.djrapitops.plan.extension.CallEvents
import com.djrapitops.plan.extension.Caller
import com.djrapitops.plan.extension.DataExtension
import com.djrapitops.plan.extension.ExtensionService
import com.djrapitops.plan.extension.FormatType
import com.djrapitops.plan.extension.annotation.NumberProvider
import com.djrapitops.plan.extension.annotation.PluginInfo
import com.djrapitops.plan.extension.annotation.StringProvider
import com.djrapitops.plan.extension.annotation.TableProvider
import com.djrapitops.plan.extension.icon.Color
import com.djrapitops.plan.extension.icon.Family
import com.djrapitops.plan.extension.icon.Icon
import com.djrapitops.plan.extension.table.Table
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.mairwunnx.redbull.core.CANS
import java.util.UUID

/**
 * Red Bull in Plan, when Plan is there: a player's page shows their cans, caffeine, overdose deaths, flight on
 * Red Bull wings, and favourite can, and the players table gets a cans column. The server page adds everyone's
 * numbers, the top drinkers, and cans by kind. Plan asks from its own threads, so the extension reads [Tally] alone.
 */
object PlanData {
  private val NEEDS = listOf("DATA_EXTENSION_VALUES", "DATA_EXTENSION_SHOW_IN_PLAYER_TABLE", "DATA_EXTENSION_TABLES")

  val available: Boolean get() = Bukkit.getPluginManager().isPluginEnabled("Plan")

  @Volatile private var caller: Caller? = null

  /** Registers the extension, now and again whenever Plan reloads. Call only when [available]. */
  fun register(plugin: RedBullPlugin) {
    val capabilities = CapabilityService.getInstance()
    val missing = NEEDS.filterNot(capabilities::hasCapability)
    if (missing.isNotEmpty()) return plugin.logger.warning("Plan is too old for Red Bull's section (it lacks ${missing.joinToString()}); update Plan to see it")
    val extension = Extension(plugin.stats.tally)
    // Plan finishes enabling after the plugins that load before it start, then calls the listener.
    capabilities.registerEnableListener { enabled -> if (enabled) add(plugin, extension, quiet = false) }
    add(plugin, extension, quiet = true)
  }

  private fun add(plugin: RedBullPlugin, extension: Extension, quiet: Boolean) {
    runCatching { ExtensionService.getInstance().register(extension).orElse(null) }
      .onSuccess { caller = it; if (it != null) plugin.logger.info("Plan found: cans, caffeine, overdoses, and flight show in its Plugins sections") }
      .onFailure { if (!quiet) plugin.logger.warning("Couldn't add Red Bull to Plan: ${it.message}") }
  }

  /** Tells Plan a player's values changed, after a can or an overdose. */
  fun update(player: Player) {
    caller?.let { runCatching { it.updatePlayerData(player.uniqueId, player.name) } }
  }

  @PluginInfo(name = "Red Bull", iconName = "bolt", iconFamily = Family.SOLID, color = Color.BLUE)
  class Extension(private val tally: Tally) : DataExtension {
    override fun callExtensionMethodsOn(): Array<CallEvents> =
      arrayOf(CallEvents.PLAYER_JOIN, CallEvents.PLAYER_LEAVE, CallEvents.SERVER_EXTENSION_REGISTER, CallEvents.SERVER_PERIODICAL)

    @NumberProvider(text = "Cans drunk", description = "Every Red Bull they drank", priority = 100, iconName = "bolt", iconColor = Color.BLUE, showInPlayerTable = true)
    fun cans(player: UUID): Long = tally.of(player).cans

    @NumberProvider(text = "Caffeine", description = "Milligrams of caffeine from cans and crude taurine, 80 a can", priority = 90, iconName = "mug-hot", iconColor = Color.BROWN)
    fun caffeine(player: UUID): Long = tally.of(player).caffeine

    @NumberProvider(text = "Overdose deaths", description = "Times their heart gave out from caffeine", priority = 80, iconName = "heart-broken", iconColor = Color.RED)
    fun overdoses(player: UUID): Long = tally.of(player).overdoses

    @NumberProvider(text = "Flight on wings", description = "Time they glided on a winged can", priority = 70, iconName = "feather", iconColor = Color.LIGHT_BLUE, format = FormatType.TIME_MILLISECONDS)
    fun flight(player: UUID): Long = tally.of(player).flight

    @StringProvider(text = "Favourite can", description = "The kind they drank most", priority = 60, iconName = "star", iconColor = Color.AMBER)
    fun favourite(player: UUID): String = tally.of(player).favourite?.let(::kindName) ?: "–"

    @NumberProvider(text = "Cans drunk", description = "Every Red Bull drunk here", priority = 100, iconName = "bolt", iconColor = Color.BLUE)
    fun totalCans(): Long = tally.total().cans

    @NumberProvider(text = "Caffeine", description = "Milligrams of caffeine everyone took, 80 a can", priority = 90, iconName = "mug-hot", iconColor = Color.BROWN)
    fun totalCaffeine(): Long = tally.total().caffeine

    @NumberProvider(text = "Overdose deaths", description = "Every heart that gave out from caffeine", priority = 80, iconName = "heart-broken", iconColor = Color.RED)
    fun totalOverdoses(): Long = tally.total().overdoses

    @NumberProvider(text = "Flight on wings", description = "Time everyone glided on winged cans", priority = 70, iconName = "feather", iconColor = Color.LIGHT_BLUE, format = FormatType.TIME_MILLISECONDS)
    fun totalFlight(): Long = tally.total().flight

    @TableProvider(tableColor = Color.BLUE)
    fun drinkers(): Table {
      val table = Table.builder().columnOne("Player", icon("user")).columnTwo("Cans", icon("bolt"))
        .columnThree("Caffeine, mg", icon("mug-hot")).columnFour("Overdoses", icon("heart-broken"))
      tally.board(25) { it.cans }.first.forEach { (_, it) -> table.addRow(it.name ?: "?", it.cans, it.caffeine, it.overdoses) }
      return table.build()
    }

    @TableProvider(tableColor = Color.LIGHT_BLUE)
    fun kinds(): Table {
      val table = Table.builder().columnOne("Can", icon("bolt")).columnTwo("Drunk", icon("hashtag"))
      val kinds = tally.total().kinds
      CANS.map { it.id }.sortedByDescending { kinds[it] ?: 0 }.forEach { table.addRow(kindName(it), kinds[it] ?: 0) }
      return table.build()
    }

    /** `pink_sugarfree` as “Pink Sugarfree”, close to the cans' English names. */
    private fun kindName(kind: String) = kind.split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    private fun icon(name: String) = Icon.called(name).build()
  }
}
