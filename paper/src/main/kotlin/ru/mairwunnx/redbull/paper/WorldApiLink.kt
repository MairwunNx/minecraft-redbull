package ru.mairwunnx.redbull.paper

import ru.mairwunnx.redbull.core.CANS
import java.util.UUID

/**
 * Red Bull's numbers in WorldAPI, when it runs: the `redbull` namespace with the totals, players' numbers, and boards.
 * [StatsExpansion] speaks WorldAPI's expansion API; this class holds no type of it, so it loads without WorldAPI.
 * Red Bull keeps its numbers in [Tally]; WorldAPI reads them on request, from its own threads.
 */
class WorldApiLink(private val plugin: RedBullPlugin) {
  @Volatile var connected = false
    private set

  /** Registers the expansion when WorldAPI is installed; nothing happens without it. */
  fun connect() {
    if (plugin.server.pluginManager.getPlugin(WORLD_API) == null) return
    try {
      StatsExpansion.register(this)
      connected = true
      plugin.logger.info("WorldAPI found: cans, caffeine, overdoses, and flight at /extensions/${StatsExpansion.ID}")
    } catch (error: LinkageError) {
      // WorldAPI before 3.4.0 has no expansion API.
      plugin.logger.warning("WorldAPI is too old for Red Bull's numbers; it needs 3.4.0 or newer")
    } catch (error: Exception) {
      plugin.logger.warning("WorldAPI didn't take Red Bull's numbers: ${error.message}")
    }
  }

  fun disconnect() {
    if (!connected) return
    connected = false
    runCatching { StatsExpansion.unregister() }
  }

  private val tally get() = plugin.stats.tally

  /** Everyone's numbers, all time. */
  fun stats(): Map<String, Any?> = tally.total().let { total ->
    mapOf("players" to tally.all().size) + numbers(total) + mapOf("kinds" to kinds(total, everyKind = true))
  }

  /** A player's numbers, by UUID or by the name of someone who drank here. */
  fun player(raw: String): Map<String, Any?> {
    val id = tally.find(raw) ?: throw NoSuchElementException("No player named $raw has drunk Red Bull here")
    val drinker = tally.of(id)
    return mapOf("uuid" to id, "name" to drinker.name) + numbers(drinker) +
      mapOf("favourite" to drinker.favourite, "rank" to rank(id), "kinds" to kinds(drinker, everyKind = false))
  }

  /** The players who drank the most cans, with all their numbers. */
  fun drinkers(limit: Int): List<Map<String, Any?>> = tally.board(limit) { it.cans }.first.map { (id, drinker) ->
    mapOf("uuid" to id, "name" to drinker.name) + numbers(drinker) + mapOf("favourite" to drinker.favourite)
  }

  /** A board shaped as WorldAPI's own leaderboards. */
  fun board(id: String, limit: Int): Map<String, Any?> {
    val board = BOARDS[id] ?: throw NoSuchElementException("Boards: ${BOARDS.keys.joinToString()}")
    val (ranked, total) = tally.board(limit, board.score)
    return mapOf(
      "id" to id, "title" to board.title, "unit" to board.unit, "total" to total,
      "entries" to ranked.mapIndexed { i, (uuid, drinker) ->
        mapOf("place" to i + 1, "player" to mapOf("uuid" to uuid, "name" to drinker.name), "value" to board.score(drinker))
      },
    )
  }

  /** Their place by cans: 1 plus everyone with more. */
  private fun rank(id: UUID): Int {
    val cans = tally.of(id).cans
    return tally.all().count { it.key != id && it.value.cans > cans } + 1
  }

  private fun numbers(drinker: Drinker) = mapOf(
    "cans" to drinker.cans, "caffeineMg" to drinker.caffeine, "overdoseDeaths" to drinker.overdoses, "flightSeconds" to drinker.flightSeconds,
  )

  /** Cans by kind in [CANS] order; [everyKind] keeps those nobody drank, at zero. */
  private fun kinds(drinker: Drinker, everyKind: Boolean) = CANS.map { it.id to (drinker.kinds[it.id] ?: 0L) }
    .filter { everyKind || it.second > 0 }
    .map { (kind, cans) -> mapOf("kind" to kind, "cans" to cans) }

  private class Board(val title: String, val unit: String, val score: (Drinker) -> Long)

  private companion object {
    const val WORLD_API = "WorldAPI"

    val BOARDS = linkedMapOf(
      "cans" to Board("Red Bull cans drunk", "cans", Drinker::cans),
      "caffeine" to Board("Caffeine from Red Bull", "mg", Drinker::caffeine),
      "overdoses" to Board("Deaths by Red Bull overdose", "deaths", Drinker::overdoses),
      "flight" to Board("Flight on Red Bull wings", "seconds", Drinker::flightSeconds),
    )
  }
}
