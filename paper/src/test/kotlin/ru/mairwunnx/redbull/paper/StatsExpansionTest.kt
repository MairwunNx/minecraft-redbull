package ru.mairwunnx.redbull.paper

import com.mairwunnx.worldapi.expansion.v2.PlayerRef
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StatsExpansionTest {
  private val steve = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val alex = UUID.fromString("00000000-0000-0000-0000-000000000002")

  private val tally = Tally(Files.createTempDirectory("redbull").toFile().resolve("stats.yml")).apply {
    drank(steve, "Steve", "original", 80)
    drank(steve, "Steve", "red", 80)
    overdose(steve, "Steve")
    drank(alex, "Alex", "red", 80)
  }

  private fun fields(type: Class<*>) = type.recordComponents.map { it.name }

  @Test
  fun `the namespace keeps API 1's paths, all public`() {
    StatsExpansion.share { tally }
    assertEquals("redbull", StatsExpansion.id())
    assertEquals(listOf("stats", "drinkers", "players/{player}", "leaderboards/{board}"), StatsExpansion.resources().map { it.path() })
    assertTrue(StatsExpansion.resources().all { it.isPublic })
    assertEquals(listOf("cans", "caffeine", "overdoses", "flight"), StatsExpansion.board.choices())
  }

  @Test
  fun `answers keep API 1's fields in their order`() {
    val numbers = listOf("cans", "caffeineMg", "overdoseDeaths", "flightSeconds")
    assertEquals(listOf("players") + numbers + "kinds", fields(Totals::class.java))
    assertEquals(listOf("uuid", "name") + numbers + "favourite", fields(TopDrinker::class.java))
    assertEquals(listOf("uuid", "name") + numbers + listOf("favourite", "rank", "kinds"), fields(DrinkerNumbers::class.java))
    assertEquals(listOf("kind", "cans"), fields(KindCans::class.java))
    assertEquals(listOf("id", "title", "unit", "total", "entries"), fields(Leaderboard::class.java))
    assertEquals(listOf("place", "player", "value"), fields(Placing::class.java))
  }

  @Test
  fun `answers read the tally`() {
    StatsExpansion.share { tally }
    val totals = StatsExpansion.totals()
    assertEquals(2, totals.players)
    assertEquals(3, totals.cans)
    assertEquals(KindCans("red", 2), totals.kinds.first { it.kind == "red" })
    assertEquals(listOf("Steve", "Alex"), StatsExpansion.drinkers(10).map { it.name })
    val alexNumbers = StatsExpansion.numbers("alex")
    assertEquals(2, alexNumbers.rank)
    assertEquals(listOf(KindCans("red", 1)), alexNumbers.kinds)
    assertFailsWith<NoSuchElementException> { StatsExpansion.numbers("Herobrine") }
    val board = StatsExpansion.leaderboard(Board.OVERDOSES, 10)
    assertEquals("overdoses", board.id)
    assertEquals(1, board.total)
    assertEquals(Placing(1, PlayerRef(steve, "Steve"), 1), board.entries.single())
  }
}
