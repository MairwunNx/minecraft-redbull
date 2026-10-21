package ru.mairwunnx.redbull.paper

import org.bukkit.plugin.java.JavaPlugin
import ru.mairwunnx.redbull.core.CANS

class RedBullPlugin : JavaPlugin() {
  @Volatile var settings = Settings(); private set

  override fun onEnable() {
    loadSettings()
    // Every item once through the game's codecs, so a component it rejects shows now, not in a player's hands.
    Items.ids.forEach { Items.create(it)!!.serializeAsBytes() }
    logger.info("Red Bull ${pluginMeta.version} is on: ${CANS.size} cans")
  }

  private fun loadSettings() {
    val file = dataFolder.resolve("config.yml")
    if (!file.exists()) saveResource("config.yml", false)
    settings = runCatching { Settings.parse(file.readText()) }
      .onFailure { logger.severe("config.yml has an error, using the defaults: ${it.message}") }
      .getOrDefault(Settings())
  }
}
