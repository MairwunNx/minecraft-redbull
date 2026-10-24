package ru.mairwunnx.redbull.modcore

import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import ru.mairwunnx.redbull.core.Advancements
import ru.mairwunnx.redbull.core.Colors
import ru.mairwunnx.redbull.core.SOFT_LANDING_SECONDS

/**
 * `redbull:wings`, the effect a can with wings gives: while it lasts, the player glides as with an elytra, wearing
 * nothing. How the game lets them glide is the loader's ([Loader.glider]); the rest is here. If Wings run out
 * mid-flight, Slow Falling sets the player down.
 */
object Wings {
  private const val SLOW_FALLING_TICKS = SOFT_LANDING_SECONDS * 20
  private const val FEATHER_TICKS = 2
  /** As on Paper: three bits, 0.8 blocks behind where the player looks, 0.3 above their feet. */
  private const val FEATHERS = 3
  private const val FEATHER_DISTANCE = 0.8
  private const val FEATHER_HEIGHT = 0.3
  private val FEATHER by lazy { ItemParticleOption(ParticleTypes.ITEM, Items.FEATHER) }

  /** Registered by [Mod.init]. */
  val EFFECT: Holder<MobEffect> by lazy {
    Mod.loader.effect("wings") { Mod.loader.glider(object : MobEffect(MobEffectCategory.BENEFICIAL, Colors.WINGS) {}) }
  }

  /**
   * Every server tick of a glide, from `GlideMixin`: on Wings, feathers trail the player every couple of ticks, and
   * the first glide is an advancement.
   */
  @JvmStatic
  fun glide(entity: LivingEntity) {
    if (entity !is ServerPlayer || entity.onGround() || !entity.hasEffect(EFFECT)) return
    if (entity.fallFlyingTicks % FEATHER_TICKS != 0) return
    feathers(entity)
    Awards.award(entity, Advancements.WINGS)
  }

  /** A few bits of feather behind [player], for everyone nearby; how many players were sent them. */
  fun feathers(player: ServerPlayer): Int {
    val back = player.lookAngle.scale(-FEATHER_DISTANCE)
    return player.level().sendParticles(
      FEATHER, player.x + back.x, player.y + FEATHER_HEIGHT + back.y, player.z + back.z, FEATHERS, 0.2, 0.1, 0.2, 0.02,
    )
  }

  /** [seconds] of Wings; a longer flight the player still has stays. */
  fun give(player: ServerPlayer, seconds: Int) {
    player.addEffect(MobEffectInstance(EFFECT, seconds * 20))
  }

  /**
   * Wings that ran out ([expired]), or were taken mid-glide, say so; taken on the ground, they go quietly. The rest
   * waits for the end of the tick: the game is still walking the effects when one ends, and adding one there would
   * break its loop.
   */
  fun ended(entity: LivingEntity, expired: Boolean) {
    val gliding = entity.isFallFlying
    Later.run(0) {
      if (entity.isRemoved || !entity.isAlive || entity.hasEffect(EFFECT)) return@run
      if (gliding) entity.addEffect(MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_TICKS))
      if (expired || gliding) (entity as? ServerPlayer)?.sendOverlayMessage(Drinks.text("redbull.wings.gone", ChatFormatting.GRAY))
    }
  }
}
