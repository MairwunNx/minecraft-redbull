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

class RedBullPlugin : JavaPlugin(), Listener {
  @Volatile var settings = Settings(); private set
  val collected = Collected()
  val wings = Wings(this)

  private val recipes by lazy { Recipes(server) }
  private val tasks = mutableListOf<BukkitTask>()

  override fun onEnable() {
    loadSettings()
    // Every item once through the game's codecs, so a component it rejects shows now, not in a player's hands.
    Items.ids.forEach { Items.create(it)!!.serializeAsBytes() }
    recipes.register()

    val heart = Heart(this)
    val pack = Pack(this)
    val menu = Menu(this)
    listOf(this, Drinks(this), wings, collected, Loot(this), pack, menu).forEach { server.pluginManager.registerEvents(it, this) }
    registerCommand("redbull", "Open your Red Bull collection; admins also give items and check caffeine", RedBullCommand(this, menu))
    tasks += server.scheduler.runTaskTimer(this, Runnable { wings.tick() }, 20L, 20L)
    tasks += server.scheduler.runTaskTimer(this, Runnable { heart.tick() }, 40L, 40L)

    val packInfo = if (!settings.resourcePack.enabled) "pack off" else pack.url?.let { "pack from $it" } ?: "no pack URL for a dev build"
    logger.info("Red Bull ${pluginMeta.version} is on: ${CANS.size} cans, ${recipes.keys.size} recipes, $packInfo")
  }

  override fun onDisable() {
    tasks.forEach { it.cancel() }
    tasks.clear()
    recipes.unregister()
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
}
