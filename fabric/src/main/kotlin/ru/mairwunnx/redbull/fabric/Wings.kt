package ru.mairwunnx.redbull.fabric

import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.gameevent.GameEvent
import ru.mairwunnx.redbull.core.Advancements
import ru.mairwunnx.redbull.core.Colors
import ru.mairwunnx.redbull.core.SOFT_LANDING_SECONDS

/**
 * Wings, the effect a can with wings gives: while it lasts, the player glides as with an elytra, wearing nothing.
 * The game asks Fabric's elytra event both whether an entity may glide, on the server and the client alike, and
 * whether the flight wears a custom glider down; the effect answers yes to the first and stands in for the second,
 * so an elytra worn with Wings keeps every point while sculk hears the glide as with any elytra.
 * If Wings run out mid-flight, Slow Falling sets the player down.
 */
object Wings {
  private const val SLOW_FALLING_TICKS = SOFT_LANDING_SECONDS * 20
  private const val FEATHER_TICKS = 2
  /** As on Paper: three bits, 0.8 blocks behind where the player looks, 0.3 above their feet. */
  private const val FEATHERS = 3
  private const val FEATHER_DISTANCE = 0.8
  private const val FEATHER_HEIGHT = 0.3
  private val FEATHER by lazy { ItemParticleOption(ParticleTypes.ITEM, Items.FEATHER) }

  val WINGS: Holder<MobEffect> = Registry.registerForHolder(
    BuiltInRegistries.MOB_EFFECT, RedBullItems.id("wings"), object : MobEffect(MobEffectCategory.BENEFICIAL, Colors.WINGS) {},
  )

  fun init() {
    // Asked every second of flight whether to wear a glider, the event skips the rest of the game's tick, the
    // vibration of the glide included: sculk still hears a player on Wings, so the vibration goes out here.
    EntityElytraEvents.CUSTOM.register { entity, wear ->
      entity.hasEffect(WINGS).also { if (it && wear) entity.gameEvent(GameEvent.ELYTRA_GLIDE) }
    }
    // The game removes an effect in the middle of ticking every effect, so a new one waits for the end of the tick.
    ServerMobEffectEvents.AFTER_REMOVE.register { instance, entity, _ ->
      if (!instance.`is`(WINGS)) return@register
      // An effect that ran out has no time left; milk or a command take one that still has some.
      val expired = !instance.isInfiniteDuration && instance.duration <= 0
      val gliding = entity.isFallFlying
      Drinks.schedule(0) { ended(entity, expired, gliding) }
    }
  }

  /**
   * Every server tick of a glide, from `GlideMixin`: on Wings, feathers trail the player every couple of ticks, and
   * the first glide is an advancement.
   */
  @JvmStatic
  fun glide(entity: LivingEntity) {
    if (entity !is ServerPlayer || entity.onGround() || !entity.hasEffect(WINGS)) return
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
    player.addEffect(MobEffectInstance(WINGS, seconds * 20))
  }

  /** Wings that ran out, or were taken mid-glide, say so; taken on the ground, they go quietly. */
  private fun ended(entity: LivingEntity, expired: Boolean, gliding: Boolean) {
    if (entity.isRemoved || !entity.isAlive || entity.hasEffect(WINGS)) return
    if (gliding) entity.addEffect(MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_TICKS))
    if ((expired || gliding) && entity is ServerPlayer) {
      entity.sendOverlayMessage(Component.translatable("redbull.wings.gone").withStyle(ChatFormatting.GRAY))
    }
  }
}
