package ru.mairwunnx.redbull.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldTest {
  @Test
  fun `the trader never sells a legend, and asks 6 or 10 emeralds`() {
    assertTrue(Trades.forSale.none { it.recipe == null })
    assertEquals(6, Trades.price(can("original")!!))
    assertEquals(10, Trades.price(can("cherry")!!))
    repeat(50) { seed ->
      val picked = Trades.pick(Random(seed))
      assertTrue(picked.size in Trades.MIN_OFFERS..Trades.MAX_OFFERS)
      assertEquals(picked.size, picked.toSet().size)
    }
  }

  @Test
  fun `about one junk catch in ten is a can, empty or crumpled`() {
    assertTrue(Fishing.share in 0.08..0.12)
    val catches = (0 until 1000).map { Fishing.pick(Random(it)) }
    assertTrue(Ids.EMPTY_CAN in catches)
    assertTrue(catches.all { it == Ids.EMPTY_CAN || it.startsWith("crumpled_") })
  }

  @Test
  fun `the collector needs every can, and a legend is any chest-only can`() {
    assertTrue(Advancements.collector(CANS.map { it.id }.toSet()))
    assertFalse(Advancements.collector(CANS.drop(1).map { it.id }.toSet()))
    assertTrue(Advancements.legend(can("spring")!!))
    assertFalse(Advancements.legend(can("original")!!))
  }

  @Test
  fun `the collector counts every can, and its last criterion stands for them all`() {
    val json = javaClass.getResource("/data/redbull/advancement/collector.json")!!.readText()
    val criteria = Regex("\"([a-z_]+)\": \\{\\s*\"trigger\": \"minecraft:impossible\"").findAll(json).map { it.groupValues[1] }.toList()
    assertEquals(CANS.map { it.id } + Advancements.COLLECTOR_ALL, criteria)
    val requirements = Regex("\\[\"([a-z_]+)\", \"([a-z_]+)\"]").findAll(json).map { it.groupValues[1] to it.groupValues[2] }.toList()
    assertEquals(CANS.map { it.id to Advancements.COLLECTOR_ALL }, requirements)
    assertEquals(listOf("cherry", "ruby"), Advancements.collectorCriteria(setOf("ruby", "cherry", "nothing")))
    assertEquals(CANS.map { it.id } + Advancements.COLLECTOR_ALL, Advancements.collectorCriteria(CANS.map { it.id }.toSet()))
  }
}
