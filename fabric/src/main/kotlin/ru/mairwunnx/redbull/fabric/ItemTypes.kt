package ru.mairwunnx.redbull.fabric

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import ru.mairwunnx.redbull.core.Can

/** A can: its components drink it, apply its buffs, and leave the crumpled can; this adds caffeine and Wings. */
class CanItem(properties: Properties, val can: Can) : Item(properties) {
  override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
    val result = super.use(level, player, hand)
    if (result.consumesAction() && player is ServerPlayer) Drinks.raise(player)
    return result
  }

  override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
    val left = super.finishUsingItem(stack, level, entity)
    if (entity is ServerPlayer) Drinks.can(entity, can)
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
