package ru.mairwunnx.redbull.fabric

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Ids

/**
 * `/redbull` opens a chest of cans. A player sees the cans they have ever held and an empty can with question
 * marks for the rest. An admin sees everything, ingredients included, and takes what they click: one, or a
 * full stack with Shift. Nothing can be put in or taken out otherwise.
 */
object Menu {
  private const val ROWS = 4
  private val EXTRAS = listOf(Ids.EMPTY_CAN, Ids.TAURINE, Ids.BULL_BILE, Ids.CRUDE_TAURINE, CANS.first().crumpled)

  fun admin(player: ServerPlayer) = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)

  fun open(player: ServerPlayer) {
    val admin = admin(player)
    val collected = Collected.of(player)
    val container = SimpleContainer(9 * ROWS)
    CANS.forEachIndexed { slot, can ->
      container.setItem(slot, if (admin || can.id in collected) ItemStack(RedBullItems.cans.getValue(can.id)) else locked())
    }
    if (admin) EXTRAS.mapNotNull(RedBullItems::create).forEachIndexed { index, item -> container.setItem(27 + index, item) }
    val title = if (admin) Component.translatable("redbull.menu.admin")
    else Component.translatable("redbull.menu.collection", CANS.count { it.id in collected }, CANS.size)
    player.openMenu(SimpleMenuProvider({ id, inventory, _ -> View(id, inventory, container, admin) }, title))
  }

  private fun locked() = ItemStack(RedBullItems.emptyCan).apply {
    set(DataComponents.CUSTOM_NAME, Component.literal("???").withStyle { it.withItalic(false) })
    set(DataComponents.LORE, ItemLore(listOf(Component.translatable("redbull.menu.locked").withStyle(ChatFormatting.GRAY))))
    set(DataComponents.MAX_STACK_SIZE, 1)
  }

  class View(id: Int, inventory: Inventory, private val menu: SimpleContainer, private val admin: Boolean) :
    ChestMenu(MenuType.GENERIC_9x4, id, inventory, menu, ROWS) {

    override fun clicked(slotIndex: Int, buttonNum: Int, containerInput: ContainerInput, player: Player) {
      if (!admin || player !is ServerPlayer || slotIndex !in 0 until menu.containerSize) return
      val shown = menu.getItem(slotIndex).takeUnless { it.isEmpty } ?: return
      val taken = shown.copy()
      taken.count = if (containerInput == ContainerInput.QUICK_MOVE) taken.maxStackSize else 1
      RedBullCommand.hand(player, taken)
      player.level().playSound(null, player.x, player.y, player.z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f)
    }

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun stillValid(player: Player) = true
  }
}
