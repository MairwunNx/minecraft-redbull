package ru.mairwunnx.redbull.paper

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import ru.mairwunnx.redbull.core.Advancements
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.core.SUGAR_PIT_DEEPEST
import ru.mairwunnx.redbull.core.Stage
import ru.mairwunnx.redbull.core.sugarPit
import ru.mairwunnx.redbull.core.sugarPitDue
import java.util.UUID

/**
 * What a player feels as caffeine builds up. In the jitters they get hungry faster and hear their heart now
 * and then; in the crash stage it pounds, and the action bar warns that one more can could be the last. Once the
 * caffeine is gone, the sugar of every can since sinks the player into one sugar pit, as deep as the cans were many.
 */
class Heart(private val plugin: RedBullPlugin) {
  private val stages = mutableMapOf<UUID, Stage>()
  private var ticks = 0

  /** Every two seconds. */
  fun tick() {
    ticks++
    for (player in plugin.server.onlinePlayers) {
      val caffeine = Caffeines.now(player)
      if (caffeine.sugarPitDue() > 0) sink(player, caffeine)
      val stage = caffeine.stage(Caffeines.rules)
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

  /** Hungry and slow, by how deep the pit is, and told so; the deepest two in red. */
  private fun sink(player: Player, caffeine: Caffeine) {
    val depth = caffeine.sugarPitDue()
    Caffeines.set(player, caffeine.copy(sugar = 0))
    sugarPit(depth).mapNotNull(Items::potionEffect).forEach { player.addPotionEffect(it) }
    val color = if (depth >= SUGAR_PIT_DEEPEST - 1) NamedTextColor.RED else NamedTextColor.GOLD
    player.sendActionBar(Texts.text(plugin.settings.sugarPitTexts.key(depth)).color(color))
    Awards.grant(player, Advancements.SUGAR_PIT)
  }

  /** Only the player hears their own heart. */
  private fun beat(player: Player, volume: Float) =
    player.playSound(player.location, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, volume, 1f)
}
