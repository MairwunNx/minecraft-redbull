package ru.mairwunnx.redbull.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import ru.mairwunnx.redbull.core.CANS

class RedBullPlugin : JavaPlugin(), Listener {
  @Volatile var settings = Settings(); private set

  private val recipes by lazy { Recipes(server) }
  private val tasks = mutableListOf<BukkitTask>()

  override fun onEnable() {
    loadSettings()
    // Every item once through the game's codecs, so a component it rejects shows now, not in a player's hands.
    Items.ids.forEach { Items.create(it)!!.serializeAsBytes() }
    recipes.register()

    val wings = Wings(this)
    val heart = Heart(this)
    listOf(this, Drinks(this), wings).forEach { server.pluginManager.registerEvents(it, this) }
    tasks += server.scheduler.runTaskTimer(this, Runnable { wings.tick() }, 20L, 20L)
    tasks += server.scheduler.runTaskTimer(this, Runnable { heart.tick() }, 40L, 40L)

    logger.info("Red Bull ${pluginMeta.version} is on: ${CANS.size} cans, ${recipes.keys.size} recipes")
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
