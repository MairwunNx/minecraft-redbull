package ru.mairwunnx.redbull.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory

/**
 * Swaps the items of older versions for today's where a player meets them: in their inventory and ender chest on
 * join, in any container they open, and on the ground as they pick it up. Up to 1.4.1 bull bile was a potion, which
 * no brewing stand takes.
 */
class Upgrades : Listener {
  @EventHandler
  fun onJoin(event: PlayerJoinEvent) {
    upgrade(event.player.inventory)
    upgrade(event.player.enderChest)
  }

  @EventHandler(priority = EventPriority.LOWEST)
  fun onOpen(event: InventoryOpenEvent) {
    upgrade(event.view.topInventory)
    upgrade(event.view.bottomInventory)
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  fun onPickup(event: EntityPickupItemEvent) {
    val stack = event.item.itemStack
    if (Items.isOldBile(stack)) event.item.itemStack = Items.bile().asQuantity(stack.amount)
  }

  private fun upgrade(inventory: Inventory) {
    inventory.contents.forEachIndexed { slot, stack ->
      if (Items.isOldBile(stack)) inventory.setItem(slot, Items.bile().asQuantity(stack!!.amount))
    }
  }
}
