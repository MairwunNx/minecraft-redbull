package ru.mairwunnx.redbull.paper

import ru.mairwunnx.redbull.core.AllTime
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.core.RedBullPlayers
import java.util.UUID

/** Red Bull's Allay tools' view of a player on Paper: caffeine and the collection from their data, numbers from [Tally]. */
class AllayPlayers(private val plugin: RedBullPlugin) : RedBullPlayers {
  override fun caffeine(player: UUID): Caffeine? = plugin.server.getPlayer(player)?.let(Caffeines::now)

  override fun collection(player: UUID): Set<String>? = plugin.server.getPlayer(player)?.let(plugin.collected::of)

  override fun allTime(player: UUID): AllTime? {
    if (plugin.server.getPlayer(player) == null) return null
    val drinker = plugin.stats.tally.of(player)
    return AllTime(drinker.cans, drinker.caffeine, drinker.overdoses, drinker.flightSeconds, drinker.favourite?.let { "redbull:$it" })
  }
}
