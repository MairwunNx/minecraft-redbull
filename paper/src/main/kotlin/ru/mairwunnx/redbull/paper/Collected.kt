package ru.mairwunnx.redbull.paper

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

/**
 * The cans a player has ever held, in their persistent data. A can counts however it reached the inventory:
 * crafted, looted, picked up, or given. Every two seconds is soon enough, and it catches all of these at once.
 */
class Collected(private val plugin: RedBullPlugin) {
  fun of(player: Player): Set<String> =
    player.persistentDataContainer.get(KEY, PersistentDataType.LIST.strings()).orEmpty().toSet()

  fun tick() {
    for (player in plugin.server.onlinePlayers) {
      val held = player.inventory.contents.mapNotNullTo(mutableSetOf()) { Items.canOf(it)?.id }
      val known = of(player)
      if (!known.containsAll(held)) {
        player.persistentDataContainer.set(KEY, PersistentDataType.LIST.strings(), (known + held).sorted())
      }
    }
  }

  companion object {
    private val KEY = NamespacedKey("redbull", "collected")
  }
}
