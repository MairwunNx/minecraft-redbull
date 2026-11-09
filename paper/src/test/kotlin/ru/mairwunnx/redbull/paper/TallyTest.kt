package ru.mairwunnx.redbull.paper

import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TallyTest {
  private val steve = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val alex = UUID.fromString("00000000-0000-0000-0000-000000000002")

  private fun file(): File = Files.createTempDirectory("redbull").toFile().resolve("stats.yml")

  @Test
  fun `a player's numbers add up, and the totals are everyone's`() {
    val tally = Tally(file())
    tally.drank(steve, "Steve", "original", 80)
    tally.drank(steve, "Steve", "original", 80)
    tally.drank(steve, "Steve", "red", 80)
    tally.caffeine(steve, "Steve", 27)
    tally.overdose(steve, "Steve")
    tally.glided(steve, "Steve", 1500)
    tally.drank(alex, "Alex", "red", 80)
    val mine = tally.of(steve)
    assertEquals(3, mine.cans)
    assertEquals(mapOf("original" to 2L, "red" to 1L), mine.kinds)
    assertEquals(267, mine.caffeine)
    assertEquals(1, mine.overdoses)
    assertEquals(1, mine.flightSeconds)
    assertEquals("original", mine.favourite)
    val total = tally.total()
    assertEquals(4, total.cans)
    assertEquals(mapOf("original" to 2L, "red" to 2L), total.kinds)
    assertEquals(347, total.caffeine)
    assertNull(tally.of(UUID.randomUUID()).favourite)
  }

  @Test
  fun `a board ranks those with more than nothing, best first`() {
    val tally = Tally(file())
    tally.drank(steve, "Steve", "original", 80)
    repeat(2) { tally.drank(alex, "Alex", "red", 80) }
    tally.overdose(alex, "Alex")
    val (cans, ranked) = tally.board(10) { it.cans }
    assertEquals(listOf(alex, steve), cans.map { it.first })
    assertEquals(2, ranked)
    val (overdoses, many) = tally.board(10) { it.overdoses }
    assertEquals(listOf(alex), overdoses.map { it.first })
    assertEquals(1, many)
    assertEquals(1, tally.board(1) { it.cans }.first.size)
  }

  @Test
  fun `players are found by UUID or by name in any case`() {
    val tally = Tally(file())
    tally.drank(steve, "Steve", "original", 80)
    assertEquals(steve, tally.find("steve"))
    assertEquals(alex, tally.find(alex.toString()))
    assertNull(tally.find("Herobrine"))
  }

  @Test
  fun `the file keeps everything, and only a change writes it`() {
    val file = file()
    val tally = Tally(file)
    assertFalse(tally.save())
    tally.drank(steve, "Steve", "original", 80)
    tally.glided(steve, "Steve", 2500)
    assertTrue(tally.save())
    assertFalse(tally.save())
    val again = Tally(file).apply { load() }
    assertEquals(tally.of(steve), again.of(steve))
    assertEquals("Steve", again.of(steve).name)
  }
}
