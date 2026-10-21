@file:Suppress("UnstableApiUsage")

package ru.mairwunnx.redbull.paper

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Ids

/**
 * `/redbull` opens a chest of cans. A player sees the cans they have ever held and an empty can with question
 * marks for the rest. An admin sees everything, ingredients included, and takes what they click: one, or a
 * full stack with Shift. Nothing can be put in or taken out otherwise.
 */
class Menu(private val plugin: RedBullPlugin) : Listener {
  private class Holder(val admin: Boolean) : InventoryHolder {
    lateinit var menu: Inventory
    override fun getInventory() = menu
  }

  fun open(player: Player) {
    val admin = player.hasPermission(RedBullCommand.ADMIN)
    val holder = Holder(admin)
    val collected = plugin.collected.of(player)
    val title = if (admin) Texts.text("redbull.menu.admin")
    else Texts.text("redbull.menu.collection", Component.text(collected.size), Component.text(CANS.size))
    holder.menu = plugin.server.createInventory(holder, 36, title)
    CANS.forEachIndexed { slot, can ->
      holder.menu.setItem(slot, if (admin || can.id in collected) Items.can(can) else locked())
    }
    if (admin) {
      listOf(Ids.EMPTY_CAN, Ids.WINGED_CAN, Ids.CRUMPLED_CAN, Ids.BULL_BILE, Ids.CRUDE_TAURINE, Ids.TAURINE)
        .forEachIndexed { index, id -> holder.menu.setItem(27 + index, Items.create(id)) }
    }
    player.openInventory(holder.menu)
  }

  private fun locked() = Items.emptyCan().apply {
    setData(DataComponentTypes.CUSTOM_NAME, Component.text("???").decoration(TextDecoration.ITALIC, false))
    setData(DataComponentTypes.LORE, ItemLore.lore(listOf(Texts.line("redbull.menu.locked", NamedTextColor.GRAY))))
    setData(DataComponentTypes.MAX_STACK_SIZE, 1)
  }

  @EventHandler
  fun onClick(event: InventoryClickEvent) {
    val holder = event.view.topInventory.holder as? Holder ?: return
    event.isCancelled = true
    if (!holder.admin || event.clickedInventory != holder.menu) return
    val player = event.whoClicked as? Player ?: return
    val item = event.currentItem?.takeUnless { it.isEmpty } ?: return
    // A fresh item, not the shown one: a winged can's minute starts when it's taken.
    val taken = Items.id(item)?.let(Items::create) ?: return
    val stack = taken.asQuantity(if (event.isShiftClick) taken.maxStackSize else 1)
    player.inventory.addItem(stack).values.forEach { player.world.dropItem(player.location, it) }
    plugin.handed(player, stack)
    player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f)
  }

  @EventHandler
  fun onDrag(event: InventoryDragEvent) {
    if (event.view.topInventory.holder is Holder) event.isCancelled = true
  }
}
