package ru.mairwunnx.redbull.fabric

import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import ru.mairwunnx.redbull.core.Stage
import java.util.UUID

/**
 * What a player feels as caffeine builds up. In the jitters they get hungry faster and hear their heart now and
 * then; in the crash stage it pounds, and the action bar warns that one more can could be the last. It also writes
 * back what wore off, so the HUD meter of each caffeinated player follows within two seconds.
 */
object Heart {
  private val stages = mutableMapOf<UUID, Stage>()
  private var ticks = 0

  /** Every server tick; works every two seconds. */
  fun tick(server: MinecraftServer) {
    if (server.tickCount % 40 != 0) return
    ticks++
    for (player in server.playerList.players) {
      val caffeine = Caffeines.now(player)
      if ((player.getAttached(Caffeines.CAFFEINE)?.level ?: 0.0) > 0.0) Caffeines.set(player, caffeine)
      val stage = caffeine.stage(Caffeines.rules)
      val before = stages.put(player.uuid, stage) ?: Stage.CALM
      if (stage > before) warn(player, stage)
      when (stage) {
        Stage.JITTERS -> {
          player.causeFoodExhaustion(0.2f)
          if (ticks % 4 == 0) beat(player, 0.4f)
        }
        Stage.CRASH, Stage.OVERDOSE -> {
          player.causeFoodExhaustion(0.2f)
          beat(player, 0.9f)
        }
        Stage.CALM -> {}
      }
    }
    stages.keys.retainAll(server.playerList.players.map { it.uuid }.toSet())
  }

  private fun warn(player: ServerPlayer, stage: Stage) {
    when (stage) {
      Stage.JITTERS -> player.sendOverlayMessage(Component.translatable("redbull.jitters").withStyle(ChatFormatting.YELLOW))
      Stage.CRASH, Stage.OVERDOSE ->
        player.sendOverlayMessage(Component.translatable("redbull.crash").withStyle(ChatFormatting.RED))
      Stage.CALM -> {}
    }
  }

  /** Only the player hears their own heart. */
  private fun beat(player: ServerPlayer, volume: Float) {
    val sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.WARDEN_HEARTBEAT)
    player.connection.send(
      ClientboundSoundPacket(sound, SoundSource.PLAYERS, player.x, player.y, player.z, volume, 1f, player.random.nextLong()),
    )
  }
}
