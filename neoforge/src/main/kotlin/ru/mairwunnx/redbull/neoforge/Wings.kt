package ru.mairwunnx.redbull.neoforge

import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.NeoForgeMod
import net.neoforged.neoforge.event.entity.living.MobEffectEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import ru.mairwunnx.redbull.core.Colors
import ru.mairwunnx.redbull.core.SOFT_LANDING_SECONDS

/**
 * `redbull:wings`, the effect a can with wings gives: the player glides as with an elytra, and no item wears.
 * The effect switches on NeoForge's `gliding_flight` attribute, which `LivingEntity.canGlide` checks on the
 * server and on the client; `GliderWearMixin` spares an elytra worn with it. Wings that run out mid-flight leave
 * a few seconds of Slow Falling to land with.
 */
object Wings {
  private const val LANDING_TICKS = SOFT_LANDING_SECONDS * 20

  private val EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID)

  val EFFECT: DeferredHolder<MobEffect, MobEffect> = EFFECTS.register("wings") { ->
    object : MobEffect(MobEffectCategory.BENEFICIAL, Colors.WINGS) {}
      .addAttributeModifier(NeoForgeMod.GLIDING_FLIGHT, id("wings"), 1.0, AttributeModifier.Operation.ADD_VALUE)
  }

  fun register(bus: IEventBus) = EFFECTS.register(bus)

  /** Wings for [seconds]; a longer flight already under way stays. */
  fun give(player: ServerPlayer, seconds: Int) {
    player.addEffect(MobEffectInstance(EFFECT, seconds * 20))
  }

  fun listen(bus: IEventBus) {
    bus.on<MobEffectEvent.Expired> { event ->
      if (wings(event.effectInstance?.effect)) ended(event.entity, expired = true)
    }
    // Milk, `/effect clear`, and the like.
    bus.on<MobEffectEvent.Remove> { event ->
      if (wings(event.effect) && event.effectInstance != null) ended(event.entity, expired = false)
    }
  }

  private fun wings(effect: Holder<MobEffect>?) = effect != null && effect.value() == EFFECT.get()

  /**
   * Wings that ran out, or were taken mid-glide, say so; taken on the ground, they go quietly. A glide they leave
   * ends under Slow Falling, a tick later: the game is still walking the effects when one ends, and adding one there
   * would break its loop.
   */
  private fun ended(entity: LivingEntity, expired: Boolean) {
    val gliding = entity.isFallFlying
    if (expired || gliding) (entity as? ServerPlayer)?.sendOverlayMessage(Drinks.text("redbull.wings.gone", ChatFormatting.GRAY))
    if (!gliding) return
    Later.run(1) {
      if (!entity.isRemoved && entity.isAlive) entity.addEffect(MobEffectInstance(MobEffects.SLOW_FALLING, LANDING_TICKS))
    }
  }
}
