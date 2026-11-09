package ru.mairwunnx.redbull.paper

import com.mairwunnx.worldapi.expansion.ExpansionApi
import com.mairwunnx.worldapi.expansion.Resource
import com.mairwunnx.worldapi.expansion.WorldApiExpansion
import com.mairwunnx.worldapi.expansion.WorldApiExpansions

/**
 * The `redbull` namespace in WorldAPI, through its expansion API (`com.mairwunnx:worldapi-expansion`). WorldAPI
 * brings the API's classes, so this class loads only when it runs; [WorldApiLink] is the side the rest of the plugin
 * calls. Everything is public: numbers and names, no coordinates. Handlers run on WorldAPI's threads and read only
 * [Tally], which is safe there.
 */
class StatsExpansion private constructor(private val link: WorldApiLink) : WorldApiExpansion {
  override fun id() = ID

  override fun requiresApi(): String = ExpansionApi.VERSION

  override fun name() = "Red Bull"

  override fun description() = "Red Bull: cans drunk, caffeine, overdose deaths, and flight on its wings"

  override fun resources(): List<Resource> = listOf(
    Resource.of("stats", "Everyone's numbers, all time: players who drank, cans, caffeine in mg, overdose deaths, seconds of flight on a winged can, and cans by kind") {
      link.stats()
    }.publicly(),
    Resource.of("drinkers", "The players who drank the most cans, with all their numbers and favourite kind; ?limit=1..50") { request ->
      link.drinkers(request.query("limit")?.toIntOrNull()?.coerceIn(1, 50) ?: 10)
    }.publicly(),
    Resource.of("players/{player}", "A player's numbers, by UUID or name: cans, caffeine in mg, overdose deaths, flight, favourite kind, rank by cans, and cans by kind") { request ->
      link.player(request.path("player"))
    }.publicly(),
    Resource.of("leaderboards/{board}", "Players ranked by cans, caffeine, overdoses, or flight, shaped as WorldAPI's own leaderboards; ?limit=1..500") { request ->
      link.board(request.path("board"), request.query("limit")?.toIntOrNull()?.coerceIn(1, 500) ?: 10)
    }.publicly(),
  )

  companion object {
    const val ID = "redbull"

    fun register(link: WorldApiLink) = WorldApiExpansions.register(StatsExpansion(link))

    fun unregister() = WorldApiExpansions.unregister(ID)
  }
}
