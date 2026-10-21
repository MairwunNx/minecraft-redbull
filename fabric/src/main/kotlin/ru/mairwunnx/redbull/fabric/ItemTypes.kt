package ru.mairwunnx.redbull.fabric

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import ru.mairwunnx.redbull.core.Can

/** A can: its components drink it, apply its buffs, and leave the crumpled can; this adds caffeine and wings. */
class CanItem(properties: Properties, val can: Can) : Item(properties) {
  override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
    val result = super.use(level, player, hand)
    if (result.consumesAction() && player is ServerPlayer) Drinks.raise(player)
    return result
  }

  override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
    val left = super.finishUsingItem(stack, level, entity)
    if (entity !is ServerPlayer) return left
    // A winged can takes the drunk can's place; where the can stays, as in creative, it joins the inventory.
    val winged = Drinks.can(entity, can) ?: return left
    if (left.isEmpty) return winged
    entity.inventory.add(winged)
    return left
  }
}

/** Bull bile or crude taurine: a tinted potion bottle that moos, and crude taurine adds a little caffeine. */
class BottleItem(properties: Properties) : Item(properties) {
  override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
    val left = super.finishUsingItem(stack, level, entity)
    if (entity is ServerPlayer) {
      if (this === RedBullItems.crudeTaurine) Drinks.crudeTaurine(entity) else Drinks.bile(entity)
    }
    return left
  }
}

/**
 * A winged can is spent once its flight wore it down to the last point, or once its grace ran out. A spent can
 * crumples into its flavour's crumpled can, but never under a player still in the air on it. The game ticks every
 * item in an inventory, so only players who hold one pay for the check, once a second.
 */
class WingedCanItem(properties: Properties) : Item(properties) {
  override fun inventoryTick(stack: ItemStack, level: ServerLevel, entity: Entity, slot: EquipmentSlot?) {
    if (entity !is ServerPlayer || level.gameTime % 20 != 0L) return
    if (entity.isFallFlying || !spent(stack)) return
    val inventory = entity.inventory
    for (index in 0 until inventory.containerSize) {
      if (inventory.getItem(index) !== stack) continue
      inventory.setItem(index, RedBullItems.crumpledOf(stack))
      entity.sendOverlayMessage(Component.translatable("redbull.wings.gone").withStyle(ChatFormatting.GRAY))
      return
    }
  }

  private fun spent(stack: ItemStack): Boolean {
    val expires = stack.get(RedBullItems.EXPIRES) ?: 0L
    val max = stack.get(DataComponents.MAX_DAMAGE) ?: return true
    return stack.damageValue >= max - 1 || System.currentTimeMillis() >= expires
  }
}
