package ru.mairwunnx.redbull.neoforge

import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import ru.mairwunnx.redbull.core.Stage
import java.util.UUID

/**
 * What a player feels as caffeine builds up. In the jitters they get hungry faster and hear their heart now
 * and then; in the crash stage it pounds, and the action bar warns that one more can could be the last.
 */
object Heart {
  private val stages = mutableMapOf<UUID, Stage>()
  private var ticks = 0

  fun listen(bus: IEventBus) {
    bus.on<ServerTickEvent.Post> { event ->
      if (++ticks % 40 == 0) event.server.playerList.players.forEach(::beat)
    }
    bus.on<PlayerEvent.PlayerLoggedOutEvent> { stages -= it.entity.uuid }
    // A fresh life starts calm: the next stage it reaches warns again.
    bus.on<PlayerEvent.PlayerRespawnEvent> { event -> (event.entity as? ServerPlayer)?.let { stages -= it.uuid } }
  }

  /** Every two seconds; it also keeps the Caffeine effect in line, should milk or a command have taken it. */
  private fun beat(player: ServerPlayer) {
    Caffeines.refresh(player)
    val stage = Caffeines.now(player).stage(Caffeines.rules)
    val before = stages.put(player.uuid, stage) ?: Stage.CALM
    if (stage > before) warn(player, stage)
    when (stage) {
      Stage.JITTERS -> {
        player.causeFoodExhaustion(0.2f)
        if (ticks % 160 == 0) heartbeat(player, 0.4f)
      }
      Stage.CRASH, Stage.OVERDOSE -> {
        player.causeFoodExhaustion(0.2f)
        heartbeat(player, 0.9f)
      }
      Stage.CALM -> {}
    }
  }

  private fun warn(player: ServerPlayer, stage: Stage) {
    when (stage) {
      Stage.JITTERS -> player.sendOverlayMessage(Drinks.text("redbull.jitters", ChatFormatting.YELLOW))
      Stage.CRASH, Stage.OVERDOSE -> player.sendOverlayMessage(Drinks.text("redbull.crash", ChatFormatting.RED))
      Stage.CALM -> {}
    }
  }

  /** Only the player hears their own heart. */
  private fun heartbeat(player: ServerPlayer, volume: Float) {
    val sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.WARDEN_HEARTBEAT)
    player.connection.send(
      ClientboundSoundPacket(sound, SoundSource.PLAYERS, player.x, player.y, player.z, volume, 1f, player.random.nextLong()),
    )
  }
}
