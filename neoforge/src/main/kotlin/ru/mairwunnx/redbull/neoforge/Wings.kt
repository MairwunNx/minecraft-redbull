package ru.mairwunnx.redbull.neoforge

import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
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
import net.minecraft.world.item.Items as Vanilla

/**
 * `redbull:wings`, the effect a can with wings gives: the player glides as with an elytra, and no item wears.
 * The effect switches on NeoForge's `gliding_flight` attribute, which `LivingEntity.canGlide` checks on the
 * server and on the client; `GliderWearMixin` spares an elytra worn with it. Wings that run out mid-flight leave
 * a few seconds of Slow Falling to land with.
 */
object Wings {
  private const val LANDING_TICKS = SOFT_LANDING_SECONDS * 20
  private const val FEATHER_TICKS = 2
  /** As on Paper: three bits, 0.8 blocks behind where the player looks, 0.3 above their feet. */
  private const val FEATHERS = 3
  private const val FEATHER_DISTANCE = 0.8
  private const val FEATHER_HEIGHT = 0.3
  private val FEATHER by lazy { ItemParticleOption(ParticleTypes.ITEM, Vanilla.FEATHER) }

  private val EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID)

  val EFFECT: DeferredHolder<MobEffect, MobEffect> = EFFECTS.register("wings") { ->
    object : MobEffect(MobEffectCategory.BENEFICIAL, Colors.WINGS) {}
      .addAttributeModifier(NeoForgeMod.GLIDING_FLIGHT, id("wings"), 1.0, AttributeModifier.Operation.ADD_VALUE)
  }

  fun register(bus: IEventBus) = EFFECTS.register(bus)

  /** Every server tick of a glide, from `GlideMixin`: on Wings, feathers trail the player every couple of ticks. */
  @JvmStatic
  fun glide(entity: LivingEntity) {
    if (entity !is ServerPlayer || entity.onGround() || !entity.hasEffect(EFFECT)) return
    if (entity.fallFlyingTicks % FEATHER_TICKS == 0) feathers(entity)
  }

  /** A few bits of feather behind [player], for everyone nearby; how many players were sent them. */
  fun feathers(player: ServerPlayer): Int {
    val back = player.lookAngle.scale(-FEATHER_DISTANCE)
    return player.level().sendParticles(
      FEATHER, player.x + back.x, player.y + FEATHER_HEIGHT + back.y, player.z + back.z, FEATHERS, 0.2, 0.1, 0.2, 0.02,
    )
  }

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
