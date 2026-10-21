package ru.mairwunnx.redbull.paper

import org.bukkit.NamespacedKey
import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.core.Rules

/**
 * A player's caffeine lives in their persistent data: the level and the play-time tick it was measured at.
 * The clock is ticks played (`PLAY_ONE_MINUTE` counts ticks despite its name), so caffeine waits while they're away.
 */
object Caffeines {
  val rules = Rules()

  private val LEVEL = NamespacedKey("redbull", "caffeine")
  private val AT = NamespacedKey("redbull", "caffeine_at")

  fun clock(player: Player): Long = player.getStatistic(Statistic.PLAY_ONE_MINUTE).toLong()

  /** The caffeine now, with what wore off since it was last written. */
  fun now(player: Player): Caffeine {
    val data = player.persistentDataContainer
    val stored = Caffeine(
      data.getOrDefault(LEVEL, PersistentDataType.DOUBLE, 0.0),
      data.getOrDefault(AT, PersistentDataType.LONG, 0L),
    )
    return stored.now(clock(player), rules)
  }

  fun set(player: Player, caffeine: Caffeine) {
    player.persistentDataContainer.set(LEVEL, PersistentDataType.DOUBLE, caffeine.level)
    player.persistentDataContainer.set(AT, PersistentDataType.LONG, caffeine.at)
  }

  fun reset(player: Player) = set(player, Caffeine(0.0, clock(player)))
}
