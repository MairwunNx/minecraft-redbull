package ru.mairwunnx.redbull.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.redbull.core.CANS

class RedBullPlugin : JavaPlugin(), Listener {
  @Volatile var settings = Settings(); private set
  val collected = Collected(this)

  private val recipes by lazy { Recipes(server) }
  private val tasks = mutableListOf<BukkitTask>()

  override fun onEnable() {
    loadSettings()
    // Every item once through the game's codecs, so a component it rejects shows now, not in a player's hands.
    Items.ids.forEach { Items.create(it)!!.serializeAsBytes() }
    recipes.register()

    val wings = Wings(this)
    val heart = Heart(this)
    val pack = Pack(this)
    listOf(this, Drinks(this), wings, Loot(this), pack).forEach { server.pluginManager.registerEvents(it, this) }
    registerCommand("redbull", "Give Red Bull items, or check a player’s caffeine", RedBullCommand())
    tasks += server.scheduler.runTaskTimer(this, Runnable { wings.tick() }, 20L, 20L)
    tasks += server.scheduler.runTaskTimer(this, Runnable { heart.tick() }, 40L, 40L)
    tasks += server.scheduler.runTaskTimer(this, Runnable { collected.tick() }, 40L, 40L)

    val packInfo = if (!settings.resourcePack.enabled) "pack off" else pack.url?.let { "pack from $it" } ?: "no pack URL for a dev build"
    logger.info("Red Bull ${pluginMeta.version} is on: ${CANS.size} cans, ${recipes.keys.size} recipes, $packInfo")
  }

  override fun onDisable() {
    tasks.forEach { it.cancel() }
    tasks.clear()
    recipes.unregister()
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
}
