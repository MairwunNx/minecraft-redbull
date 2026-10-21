package ru.mairwunnx.redbull.fabric

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.core.Colors
import ru.mairwunnx.redbull.core.Rules
import ru.mairwunnx.redbull.core.effectLevel
import ru.mairwunnx.redbull.core.secondsLeft
import ru.mairwunnx.redbull.core.sleepless

/**
 * A player's caffeine lives in a data attachment: the level and the play-time tick it was measured at. It's saved
 * with the player. Death drops it, since the attachment isn't copied to the respawned player. The clock is ticks
 * played, so caffeine waits while they're away.
 *
 * The `redbull:caffeine` effect shows it: Caffeine I calm, II in the jitters, III in the crash, for as long as the
 * caffeine takes to wear off. It only shows; everything else reads the attachment. Milk or `/effect clear` may take
 * the effect off, and the next [refresh] puts it back.
 *
 * Any caffeine keeps a player out of bed. Fabric asks once the game has checked the bed and set the spawn point,
 * as it does when it's too early to sleep.
 */
object Caffeines {
  val rules = Rules()

  private val CODEC: Codec<Caffeine> = RecordCodecBuilder.create { instance ->
    instance.group(
      Codec.DOUBLE.fieldOf("level").forGetter(Caffeine::level),
      Codec.LONG.fieldOf("at").forGetter(Caffeine::at),
    ).apply(instance, ::Caffeine)
  }

  val CAFFEINE: AttachmentType<Caffeine> = AttachmentRegistry.create(RedBullItems.id("caffeine")) { it.persistent(CODEC) }

  val EFFECT: Holder<MobEffect> = Registry.registerForHolder(
    BuiltInRegistries.MOB_EFFECT, RedBullItems.id("caffeine"), object : MobEffect(MobEffectCategory.NEUTRAL, Colors.CAFFEINE) {},
  )

  /** What a bed answers a caffeinated player, in the action bar. */
  val SLEEPLESS = Player.BedSleepingProblem(Component.translatable("redbull.sleepless"))

  fun init() {
    EntitySleepEvents.ALLOW_SLEEPING.register { player, _ ->
      if (player is ServerPlayer && now(player).sleepless()) SLEEPLESS else null
    }
  }

  fun clock(player: ServerPlayer): Long = player.stats.getValue(Stats.CUSTOM, Stats.PLAY_TIME).toLong()

  /** The caffeine now, with what wore off since it was last written. */
  fun now(player: ServerPlayer): Caffeine =
    (player.getAttached(CAFFEINE) ?: Caffeine()).now(clock(player), rules)

  /** Writes [caffeine] as of now and shows it. */
  fun set(player: ServerPlayer, caffeine: Caffeine) {
    player.setAttached(CAFFEINE, caffeine.now(clock(player), rules))
    refresh(player)
  }

  fun reset(player: ServerPlayer) = set(player, Caffeine(0.0, clock(player)))

  /**
   * Puts the effect in line with the caffeine: its level and the time left, or no effect at all without caffeine.
   * It replaces the effect outright, since the game's own merge never lowers a level or shortens a duration.
   */
  fun refresh(player: ServerPlayer) {
    val caffeine = now(player)
    if (!caffeine.sleepless()) {
      if (player.hasEffect(EFFECT)) player.removeEffect(EFFECT)
      return
    }
    val level = caffeine.effectLevel(rules)
    val ticks = caffeine.secondsLeft(rules) * 20
    val shown = player.getEffect(EFFECT)
    // The effect counts down as caffeine wears off, so a beat changes nothing but the second it rounds to.
    if (shown != null && shown.amplifier == level && kotlin.math.abs(shown.duration - ticks) <= 20) return
    player.forceAddEffect(MobEffectInstance(EFFECT, ticks, level, false, false, true), null)
  }
}
