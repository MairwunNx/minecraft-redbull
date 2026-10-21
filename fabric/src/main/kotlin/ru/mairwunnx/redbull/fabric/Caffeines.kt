package ru.mairwunnx.redbull.fabric

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.core.Rules

/**
 * A player's caffeine lives in a data attachment: the level and the play-time tick it was measured at. It's saved
 * with the player and synced to them alone, for the HUD meter. Death drops it, since the attachment isn't copied to
 * the respawned player. The clock is ticks played, so caffeine waits while they're away.
 */
object Caffeines {
  val rules = Rules()

  private val CODEC: Codec<Caffeine> = RecordCodecBuilder.create { instance ->
    instance.group(
      Codec.DOUBLE.fieldOf("level").forGetter(Caffeine::level),
      Codec.LONG.fieldOf("at").forGetter(Caffeine::at),
    ).apply(instance, ::Caffeine)
  }

  private val STREAM_CODEC: StreamCodec<io.netty.buffer.ByteBuf, Caffeine> = StreamCodec.composite(
    ByteBufCodecs.DOUBLE, Caffeine::level,
    ByteBufCodecs.VAR_LONG, Caffeine::at,
    ::Caffeine,
  )

  val CAFFEINE: AttachmentType<Caffeine> = AttachmentRegistry.create(RedBullItems.id("caffeine")) {
    it.persistent(CODEC).syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
  }

  fun clock(player: ServerPlayer): Long = player.stats.getValue(Stats.CUSTOM, Stats.PLAY_TIME).toLong()

  /** The caffeine now, with what wore off since it was last written. */
  fun now(player: ServerPlayer): Caffeine =
    (player.getAttached(CAFFEINE) ?: Caffeine()).now(clock(player), rules)

  fun set(player: ServerPlayer, caffeine: Caffeine) {
    player.setAttached(CAFFEINE, caffeine)
  }

  fun reset(player: ServerPlayer) = set(player, Caffeine(0.0, clock(player)))
}
