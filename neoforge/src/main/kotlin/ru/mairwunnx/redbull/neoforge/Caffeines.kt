package ru.mairwunnx.redbull.neoforge

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.core.Rules
import java.util.function.Supplier

/**
 * A player's caffeine lives in a data attachment: the level and the play-time tick it was measured at. The clock is
 * ticks played, so caffeine waits while they're away. It goes to the player's own client for the meter; it isn't
 * copied on death, so death takes all of it.
 */
object Caffeines {
  val rules = Rules()

  private val ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID)

  private val CODEC = RecordCodecBuilder.mapCodec { instance ->
    instance.group(
      Codec.DOUBLE.fieldOf("level").forGetter(Caffeine::level),
      Codec.LONG.fieldOf("at").forGetter(Caffeine::at),
    ).apply(instance, ::Caffeine)
  }

  private val STREAM_CODEC: StreamCodec<io.netty.buffer.ByteBuf, Caffeine> =
    StreamCodec.composite(ByteBufCodecs.DOUBLE, Caffeine::level, ByteBufCodecs.VAR_LONG, Caffeine::at, ::Caffeine)

  val CAFFEINE: Supplier<AttachmentType<Caffeine>> = ATTACHMENTS.register("caffeine") { ->
    AttachmentType.builder(Supplier { Caffeine() })
      .serialize(CODEC)
      .sync({ holder, to -> holder === to }, STREAM_CODEC)
      .build()
  }

  fun register(bus: IEventBus) = ATTACHMENTS.register(bus)

  fun clock(player: ServerPlayer): Long = player.stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)).toLong()

  /** The caffeine now, with what wore off since it was last written. */
  fun now(player: ServerPlayer): Caffeine = player.getData(CAFFEINE).now(clock(player), rules)

  /** Writes [caffeine] as of now, so the client reads the level its meter starts from. */
  fun set(player: ServerPlayer, caffeine: Caffeine) {
    player.setData(CAFFEINE, caffeine.now(clock(player), rules))
  }

  fun reset(player: ServerPlayer) = set(player, Caffeine(0.0, clock(player)))
}
