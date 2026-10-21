@file:Suppress("UnstableApiUsage")

package ru.mairwunnx.redbull.paper

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.redbull.core.Ids

/**
 * A winged can is spent once its flight wore it down to the last point, or once its grace ran out.
 * A spent can turns back into an empty one, but never under a player still in the air on it.
 */
class Wings(private val plugin: RedBullPlugin) : Listener {
  @EventHandler
  fun onLand(event: EntityToggleGlideEvent) {
    if (event.isGliding) return
    val player = event.entity as? Player ?: return
    // The game stops the glide before it writes the item; look a tick later.
    plugin.server.scheduler.runTask(plugin, Runnable { if (player.isOnline) sweep(player) })
  }

  /** Every second: turn spent winged cans back into empty ones. */
  fun tick() {
    plugin.server.onlinePlayers.forEach(::sweep)
  }

  private fun sweep(player: Player) {
    if (player.isGliding) return
    val inventory = player.inventory
    val now = System.currentTimeMillis()
    for (slot in 0 until inventory.size) {
      val item = inventory.getItem(slot) ?: continue
      if (Items.id(item) != Ids.WINGED_CAN || !spent(item, now)) continue
      inventory.setItem(slot, Items.emptyCan())
      player.sendActionBar(Texts.text("redbull.wings.gone").color(NamedTextColor.GRAY))
    }
  }

  private fun spent(item: ItemStack, now: Long): Boolean {
    val expires = item.persistentDataContainer.getOrDefault(Items.EXPIRES, PersistentDataType.LONG, 0L)
    val max = item.getData(DataComponentTypes.MAX_DAMAGE) ?: return true
    val damage = item.getData(DataComponentTypes.DAMAGE) ?: 0
    return damage >= max - 1 || now >= expires
  }
}
