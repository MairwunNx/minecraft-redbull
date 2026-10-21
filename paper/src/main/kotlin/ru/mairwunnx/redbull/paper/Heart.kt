package ru.mairwunnx.redbull.paper

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import ru.mairwunnx.redbull.core.Stage
import java.util.UUID

/**
 * What a player feels as caffeine builds up. In the jitters they get hungry faster and hear their heart now
 * and then; in the crash stage it pounds, and the action bar warns that one more can could be the last.
 */
class Heart(private val plugin: RedBullPlugin) {
  private val stages = mutableMapOf<UUID, Stage>()
  private var ticks = 0

  /** Every two seconds. */
  fun tick() {
    ticks++
    for (player in plugin.server.onlinePlayers) {
      val stage = Caffeines.now(player).stage(Caffeines.rules)
      val before = stages.put(player.uniqueId, stage) ?: Stage.CALM
      if (stage > before) warn(player, stage)
      when (stage) {
        Stage.JITTERS -> {
          player.exhaustion += 0.2f
          if (ticks % 4 == 0) beat(player, 0.4f)
        }
        Stage.CRASH, Stage.OVERDOSE -> {
          player.exhaustion += 0.2f
          beat(player, 0.9f)
        }
        Stage.CALM -> {}
      }
    }
    stages.keys.retainAll(plugin.server.onlinePlayers.map { it.uniqueId }.toSet())
  }

  private fun warn(player: Player, stage: Stage) {
    when (stage) {
      Stage.JITTERS -> player.sendActionBar(Texts.text("redbull.jitters").color(NamedTextColor.YELLOW))
      Stage.CRASH, Stage.OVERDOSE -> player.sendActionBar(Texts.text("redbull.crash").color(NamedTextColor.RED))
      Stage.CALM -> {}
    }
  }

  /** Only the player hears their own heart. */
  private fun beat(player: Player, volume: Float) =
    player.playSound(player.location, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, volume, 1f)
}
