package ru.mairwunnx.redbull.neoforge

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.entity.player.Player
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.core.Rules
import ru.mairwunnx.redbull.core.effectLevel
import ru.mairwunnx.redbull.core.secondsLeft
import ru.mairwunnx.redbull.core.sleepless
import java.util.function.Supplier

/**
 * A player's caffeine lives in a data attachment: the level and the play-time tick it was measured at. The clock is
 * ticks played, so caffeine waits while they're away. It isn't copied on death, so death takes all of it.
 *
 * The `redbull:caffeine` effect shows it: Caffeine I calm, II in the jitters, III in the crash, for as long as the
 * caffeine takes to wear off. It only shows; everything else reads the attachment. Milk or `/effect clear` may take
 * the effect off, and the next [refresh] puts it back.
 *
 * Any caffeine keeps a player out of bed. The game has already checked the bed and set the spawn point by then,
 * as it does when it's too early to sleep.
 */
object Caffeines {
  val rules = Rules()

  /** Coffee brown. */
  private const val COLOR = 0x6F4E37

  private val ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID)
  private val EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID)

  private val CODEC = RecordCodecBuilder.mapCodec { instance ->
    instance.group(
      Codec.DOUBLE.fieldOf("level").forGetter(Caffeine::level),
      Codec.LONG.fieldOf("at").forGetter(Caffeine::at),
    ).apply(instance, ::Caffeine)
  }

  val CAFFEINE: Supplier<AttachmentType<Caffeine>> = ATTACHMENTS.register("caffeine") { ->
    AttachmentType.builder(Supplier { Caffeine() })
      .serialize(CODEC)
      .build()
  }

  val EFFECT: DeferredHolder<MobEffect, MobEffect> = EFFECTS.register("caffeine") { ->
    object : MobEffect(MobEffectCategory.NEUTRAL, COLOR) {}
  }

  fun register(bus: IEventBus) {
    ATTACHMENTS.register(bus)
    EFFECTS.register(bus)
  }

  /** What a bed answers a caffeinated player, in the action bar. */
  val SLEEPLESS = Player.BedSleepingProblem(Component.translatable("redbull.sleepless"))

  fun listen(bus: IEventBus) {
    bus.on<CanPlayerSleepEvent> { event ->
      if (event.problem == null && now(event.entity).sleepless()) event.problem = SLEEPLESS
    }
  }

  fun clock(player: ServerPlayer): Long = player.stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)).toLong()

  /** The caffeine now, with what wore off since it was last written. */
  fun now(player: ServerPlayer): Caffeine = player.getData(CAFFEINE).now(clock(player), rules)

  /** Writes [caffeine] as of now and shows it. */
  fun set(player: ServerPlayer, caffeine: Caffeine) {
    player.setData(CAFFEINE, caffeine.now(clock(player), rules))
    refresh(player)
  }

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

  fun reset(player: ServerPlayer) = set(player, Caffeine(0.0, clock(player)))
}
