package ru.mairwunnx.redbull.paper

import com.mairwunnx.worldapi.expansion.v2.Expansion
import com.mairwunnx.worldapi.expansion.v2.PlayerRef
import ru.mairwunnx.redbull.core.CANS
import java.util.UUID

/** Everyone's numbers, all time. */
@JvmRecord data class Totals(val players: Int, val cans: Long, val caffeineMg: Long, val overdoseDeaths: Long, val flightSeconds: Long, val kinds: List<KindCans>)

/** The cans of one kind. */
@JvmRecord data class KindCans(val kind: String, val cans: Long)

/** One of the players who drank the most. */
@JvmRecord data class TopDrinker(val uuid: UUID, val name: String?, val cans: Long, val caffeineMg: Long, val overdoseDeaths: Long, val flightSeconds: Long, val favourite: String?)

/** A player's numbers, their rank by cans, and the kinds they drank. */
@JvmRecord data class DrinkerNumbers(val uuid: UUID, val name: String?, val cans: Long, val caffeineMg: Long, val overdoseDeaths: Long, val flightSeconds: Long, val favourite: String?, val rank: Int, val kinds: List<KindCans>)

/** A board shaped as WorldAPI's own leaderboards. */
@JvmRecord data class Leaderboard(val id: String, val title: String, val unit: String, val total: Int, val entries: List<Placing>)

@JvmRecord data class Placing(val place: Int, val player: PlayerRef, val value: Long)

/** Red Bull's boards: their title, unit, and score. */
enum class Board(val title: String, val unit: String, val score: (Drinker) -> Long) {
  CANS("Red Bull cans drunk", "cans", Drinker::cans),
  CAFFEINE("Caffeine from Red Bull", "mg", Drinker::caffeine),
  OVERDOSES("Deaths by Red Bull overdose", "deaths", Drinker::overdoses),
  FLIGHT("Flight on Red Bull wings", "seconds", Drinker::flightSeconds),
}

/**
 * The `redbull` namespace in WorldAPI, through its expansion API 2 (`com.mairwunnx:worldapi-expansion`). WorldAPI
 * 4.5.0 and newer bring the API's classes, so this object loads only when one runs. [WorldApiLink] is the side the rest
 * of the plugin calls. Everything is public: numbers and names, no coordinates. Handlers run on WorldAPI's threads and
 * read only [Tally], which is safe there.
 */
object StatsExpansion : Expansion("redbull", "Red Bull", "Red Bull: cans drunk, caffeine, overdose deaths, and flight on its wings") {
  @Volatile private var source: (() -> Tally)? = null
  private val tally get() = source?.invoke() ?: throw IllegalStateException("Red Bull is off")

  val player = text("player", "A player's UUID, or the name of a player who drank here")
  val board = choice("board", "What players are ranked by: cans, caffeine, overdoses, or flight", Board::class.java, Board.CANS)
  val limit = integer("limit", "How many players: up to 50 drinkers, or up to 500 on a board", 1, 500, 10)

  val stats = resource("stats", "Everyone's numbers, all time: players who drank, cans, caffeine in mg, overdose deaths, seconds of flight on a winged can, and cans by kind") { totals() }.returns(Totals::class.java).publicly()
  val drinkers = resource("drinkers", "The players who drank the most cans, with all their numbers and favourite kind") { req -> drinkers(req[limit].coerceAtMost(50)) }.query(limit).returnsListOf(TopDrinker::class.java).publicly()
  val players = resource("players/{player}", "A player's numbers, by UUID or name: cans, caffeine in mg, overdose deaths, flight, favourite kind, rank by cans, and cans by kind") { req -> numbers(req[player]) }.returns(DrinkerNumbers::class.java).publicly()
  val leaderboards = resource("leaderboards/{board}", "Players ranked by cans, caffeine, overdoses, or flight, shaped as WorldAPI's own leaderboards") { req -> leaderboard(req[board], req[limit]) }.query(limit).returns(Leaderboard::class.java).publicly()

  /** Shares the namespace, reading [tally] on every request. */
  fun share(tally: () -> Tally) {
    source = tally
    register()
  }

  fun totals(): Totals = tally.let { tally ->
    val total = tally.total()
    Totals(tally.all().size, total.cans, total.caffeine, total.overdoses, total.flightSeconds, kinds(total, everyKind = true))
  }

  /** The players who drank the most cans, with all their numbers. */
  fun drinkers(limit: Int): List<TopDrinker> = tally.board(limit) { it.cans }.first.map { (id, it) ->
    TopDrinker(id, it.name, it.cans, it.caffeine, it.overdoses, it.flightSeconds, it.favourite)
  }

  /** A player's numbers, by UUID or by the name of someone who drank here. */
  fun numbers(raw: String): DrinkerNumbers = tally.let { tally ->
    val id = tally.find(raw) ?: throw NoSuchElementException("No player named $raw has drunk Red Bull here")
    val it = tally.of(id)
    DrinkerNumbers(id, it.name, it.cans, it.caffeine, it.overdoses, it.flightSeconds, it.favourite, rank(tally, id), kinds(it, everyKind = false))
  }

  fun leaderboard(board: Board, limit: Int): Leaderboard {
    val (ranked, total) = tally.board(limit, board.score)
    val entries = ranked.mapIndexed { i, (uuid, drinker) -> Placing(i + 1, PlayerRef(uuid, drinker.name), board.score(drinker)) }
    return Leaderboard(board.name.lowercase(), board.title, board.unit, total, entries)
  }

  /** Their place by cans: 1 plus everyone with more. */
  private fun rank(tally: Tally, id: UUID): Int {
    val cans = tally.of(id).cans
    return tally.all().count { it.key != id && it.value.cans > cans } + 1
  }

  /** Cans by kind in [CANS] order. [everyKind] keeps those nobody drank, at zero. */
  private fun kinds(drinker: Drinker, everyKind: Boolean) = CANS.map { KindCans(it.id, drinker.kinds[it.id] ?: 0L) }.filter { everyKind || it.cans > 0 }
}
