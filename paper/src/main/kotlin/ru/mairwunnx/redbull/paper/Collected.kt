package ru.mairwunnx.redbull.paper

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.redbull.core.Advancements
import ru.mairwunnx.redbull.core.can

/**
 * The cans a player has ever held, in their persistent data. A can counts the moment it reaches them: picked
 * up, crafted, drunk, given, or taken from a chest, which shows when they close it. Nothing runs between those
 * events, so a full server costs no more than an empty one.
 */
class Collected : Listener {
  fun of(player: Player): Set<String> =
    player.persistentDataContainer.get(KEY, PersistentDataType.LIST.strings()).orEmpty().toSet()

  fun record(player: Player, items: Iterable<ItemStack?>) {
    val ids = items.mapNotNullTo(mutableSetOf()) { Items.canOf(it)?.id }
    if (ids.isEmpty()) return
    val known = of(player)
    if (known.containsAll(ids)) return
    val all = known + ids
    player.persistentDataContainer.set(KEY, PersistentDataType.LIST.strings(), all.sorted())
    award(player, all)
  }

  /** The advancements a collection earns; on join too, so one gathered before they existed counts. */
  private fun award(player: Player, collection: Set<String>) {
    if (collection.isEmpty()) return
    Awards.grant(player, Advancements.ROOT)
    if (collection.any { id -> can(id)?.let(Advancements::legend) == true }) Awards.grant(player, Advancements.LEGEND)
    if (Advancements.collector(collection)) Awards.grant(player, Advancements.COLLECTOR)
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onPickup(event: EntityPickupItemEvent) {
    (event.entity as? Player)?.let { record(it, listOf(event.item.itemStack)) }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onCraft(event: CraftItemEvent) {
    (event.whoClicked as? Player)?.let { record(it, listOf(event.recipe.result)) }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onDrink(event: PlayerItemConsumeEvent) = record(event.player, listOf(event.item))

  @EventHandler
  fun onClose(event: InventoryCloseEvent) {
    (event.player as? Player)?.let { record(it, it.inventory.contents.asList()) }
  }

  @EventHandler
  fun onJoin(event: PlayerJoinEvent) {
    record(event.player, event.player.inventory.contents.asList())
    award(event.player, of(event.player))
  }

  companion object {
    private val KEY = NamespacedKey("redbull", "collected")
  }
}
