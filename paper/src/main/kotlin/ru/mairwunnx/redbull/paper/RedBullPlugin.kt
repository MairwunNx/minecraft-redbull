package ru.mairwunnx.redbull.paper

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Ids
import ru.mairwunnx.redbull.core.RedBullInfo

class RedBullPlugin : JavaPlugin(), Listener {
  @Volatile var settings = Settings(); private set
  val collected = Collected()
  val wings = Wings(this)
  val stats by lazy { Stats(dataFolder.resolve("stats.yml")) }
  private val worldApi = WorldApiLink(this)
  private val metrics by lazy { PluginMetrics(this, stats.counts) }

  private val recipes by lazy { Recipes(server) }
  private val tasks = mutableListOf<BukkitTask>()

  override fun onEnable() {
    // For Allay, which finds Red Bull's expansion once this plugin is on.
    RedBullInfo.version = pluginMeta.version
    loadSettings()
    loadStats()
    metrics.apply(settings.metrics)
    // Every item once through the game's codecs, so a component it rejects shows now, not in a player's hands.
    Items.ids.forEach { Items.create(it)!!.serializeAsBytes() }
    recipes.register()

    val heart = Heart(this)
    val pack = Pack(this)
    val menu = Menu(this)
    listOf(this, Drinks(this), wings, collected, Symptoms(), Wanderers(), Loot(this), Upgrades(), pack, menu).forEach { server.pluginManager.registerEvents(it, this) }
    registerCommand("redbull", "Open your Red Bull collection; admins also give items and check caffeine", RedBullCommand(this, menu))
    tasks += server.scheduler.runTaskTimer(this, Runnable { wings.tick() }, 20L, 20L)
    tasks += server.scheduler.runTaskTimer(this, Runnable { wings.feathers() }, 2L, 2L)
    tasks += server.scheduler.runTaskTimer(this, Runnable { heart.tick() }, 40L, 40L)
    tasks += server.scheduler.runTaskTimerAsynchronously(this, Runnable { saveStats() }, SAVE_TICKS, SAVE_TICKS)

    worldApi.connect()

    val packInfo = if (!settings.resourcePack.enabled) "pack off" else pack.url?.let { "pack from $it" } ?: "no pack URL for a dev build"
    logger.info("Red Bull ${pluginMeta.version} is on: ${CANS.size} cans, ${recipes.keys.size} recipes, $packInfo")
  }

  override fun onDisable() {
    metrics.stop()
    worldApi.disconnect()
    tasks.forEach { it.cancel() }
    tasks.clear()
    recipes.unregister()
    saveStats()
  }

  /** An item handed to a player outside the game's own ways: it joins their collection, and a winged can is watched. */
  fun handed(player: Player, item: ItemStack) {
    collected.record(player, listOf(item))
    if (Items.id(item) == Ids.WINGED_CAN) wings.watch(player)
  }

  /** Players find every recipe in their recipe book. */
  @EventHandler
  fun onJoin(event: PlayerJoinEvent) {
    event.player.discoverRecipes(recipes.keys)
  }

  private fun loadSettings() {
    val file = dataFolder.resolve("config.yml")
    if (!file.exists()) saveResource("config.yml", false)
    settings = runCatching { Settings.parse(file.readText()) }
      .onFailure { logger.severe("config.yml has an error, using the defaults: ${it.message}") }
      .getOrDefault(Settings())
  }

  /** A `stats.yml` that doesn't parse is kept aside as `stats.yml.broken`, so the next save doesn't lose it. */
  private fun loadStats() {
    runCatching { stats.tally.load() }.onFailure {
      val file = dataFolder.resolve("stats.yml")
      logger.severe("stats.yml has an error, starting from zero and keeping it as stats.yml.broken: ${it.message}")
      file.renameTo(dataFolder.resolve("stats.yml.broken"))
    }
  }

  private fun saveStats() {
    runCatching { stats.tally.save() }.onFailure { logger.warning("Couldn't save stats.yml: ${it.message}") }
  }

  private companion object {
    /** Five minutes between saves of `stats.yml`. */
    const val SAVE_TICKS = 5 * 60 * 20L
  }
}
