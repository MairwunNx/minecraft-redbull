package ru.mairwunnx.redbull.fabric

import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity

/**
 * Wings, the effect a can with wings gives: while it lasts, the player glides as with an elytra, wearing nothing.
 * The game asks Fabric's elytra event both whether an entity may glide, on the server and the client alike, and
 * whether the flight wears a custom glider down; the effect answers yes to the first and stands in for the second.
 * If Wings run out mid-flight, Slow Falling sets the player down.
 */
object Wings {
  private const val COLOR = 0x7FD8FF
  private const val SLOW_FALLING_TICKS = 5 * 20

  val WINGS: Holder<MobEffect> = Registry.registerForHolder(
    BuiltInRegistries.MOB_EFFECT, RedBullItems.id("wings"), object : MobEffect(MobEffectCategory.BENEFICIAL, COLOR) {},
  )

  fun init() {
    EntityElytraEvents.CUSTOM.register { entity, _ -> entity.hasEffect(WINGS) }
    // The game removes an effect in the middle of ticking every effect, so a new one waits for the end of the tick.
    ServerMobEffectEvents.AFTER_REMOVE.register { instance, entity, _ ->
      if (!instance.`is`(WINGS)) return@register
      val gliding = entity.isFallFlying
      Drinks.schedule(0) { ended(entity, gliding) }
    }
  }

  /** [seconds] of Wings; a longer flight the player still has stays. */
  fun give(player: ServerPlayer, seconds: Int) {
    player.addEffect(MobEffectInstance(WINGS, seconds * 20))
  }

  private fun ended(entity: LivingEntity, gliding: Boolean) {
    if (entity.isRemoved || !entity.isAlive || entity.hasEffect(WINGS)) return
    if (gliding) entity.addEffect(MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_TICKS))
    if (entity is ServerPlayer) {
      entity.sendOverlayMessage(Component.translatable("redbull.wings.gone").withStyle(ChatFormatting.GRAY))
    }
  }
}
