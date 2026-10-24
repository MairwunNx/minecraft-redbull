package ru.mairwunnx.redbull.modcore

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
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
import kotlin.math.abs

/**
 * A player's caffeine lives in a data attachment ([Loader.caffeine]): the level and the play-time tick it was measured
 * at. The clock is ticks played, so caffeine waits while they're away. Death drops it, since the attachment isn't
 * copied to the respawned player.
 *
 * The `redbull:caffeine` effect shows it: Caffeine I calm, II in the jitters, III in the crash, for as long as the
 * caffeine takes to wear off. It only shows; everything else reads the attachment. Milk or `/effect clear` may take
 * the effect off, and the next [refresh] puts it back.
 *
 * Any caffeine keeps a player out of bed ([sleepProblem]). Both loaders ask once the game has checked the bed and set
 * the spawn point, as it does when it's too early to sleep.
 */
object Caffeines {
  val rules = Rules()

  /** Saved as `{level, at}` on both loaders. */
  val CODEC: MapCodec<Caffeine> = RecordCodecBuilder.mapCodec { instance ->
    instance.group(
      Codec.DOUBLE.fieldOf("level").forGetter(Caffeine::level),
      Codec.LONG.fieldOf("at").forGetter(Caffeine::at),
    ).apply(instance, ::Caffeine)
  }

  /** Registered by [Mod.init]; the loader may read [CODEC] before that. */
  val EFFECT: Holder<MobEffect> by lazy {
    Mod.loader.effect("caffeine") { object : MobEffect(MobEffectCategory.NEUTRAL, Colors.CAFFEINE) {} }
  }

  /** What a bed answers a caffeinated player, in the action bar, yellow as on Paper. */
  val SLEEPLESS = Player.BedSleepingProblem(Component.translatable("redbull.sleepless").withStyle(ChatFormatting.YELLOW))

  fun sleepProblem(player: ServerPlayer): Player.BedSleepingProblem? = SLEEPLESS.takeIf { now(player).sleepless() }

  fun clock(player: ServerPlayer): Long = player.stats.getValue(Stats.CUSTOM, Stats.PLAY_TIME).toLong()

  /** The caffeine now, with what wore off since it was last written. */
  fun now(player: ServerPlayer): Caffeine = (Mod.loader.caffeine.get(player) ?: Caffeine()).now(clock(player), rules)

  /** Writes [caffeine] as of now and shows it. */
  fun set(player: ServerPlayer, caffeine: Caffeine) {
    Mod.loader.caffeine.set(player, caffeine.now(clock(player), rules))
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
    if (shown != null && shown.amplifier == level && abs(shown.duration - ticks) <= 20) return
    player.forceAddEffect(MobEffectInstance(EFFECT, ticks, level, false, false, true), null)
  }
}
