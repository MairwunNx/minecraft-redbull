package ru.mairwunnx.redbull.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CansTest {
  @Test
  fun `ids are unique`() {
    assertEquals(CANS.size, CANS.map { it.id }.toSet().size)
  }

  @Test
  fun `no two recipes fill the grid the same way`() {
    val grids = CANS.mapNotNull { can -> can.recipe?.let { Triple(it.flavour, it.dye, can.sugarfree) } }
    assertEquals(grids.size, grids.toSet().size)
  }

  @Test
  fun `every can can be had`() {
    CANS.forEach { assertTrue(it.recipe != null || it.chests != null, "${it.id} has neither a recipe nor chests") }
  }

  @Test
  fun `every can gives something and loot chances are chances`() {
    CANS.forEach { can ->
      assertTrue(can.effects.isNotEmpty(), "${can.id} has no effects")
      can.chests?.let { assertTrue(it.chance in 0.0..1.0 && it.biomes.isNotEmpty(), "${can.id} chests") }
    }
  }

  @Test
  fun `chests hold an Edition one time in ten and a legend more rarely`() {
    val (legends, editions) = CANS.filter { it.chests != null }.partition { it.recipe == null }
    editions.forEach { assertEquals(EDITION_CHESTS, it.chests!!.chance, it.id) }
    assertEquals(0.10, EDITION_CHESTS)
    legends.forEach { assertTrue(it.chests!!.chance < EDITION_CHESTS, "${it.id} is as common as an Edition") }
    assertEquals(mapOf("ruby" to 0.05, "summer" to 0.035, "glacier" to 0.035, "spring" to 0.02), legends.associate { it.id to it.chests!!.chance })
  }

  @Test
  fun `sugarfree cans say so in their id`() {
    CANS.forEach { assertEquals(it.sugarfree, it.id.endsWith("sugarfree") || it.id == "zero", it.id) }
  }

  @Test
  fun `healing and Strength II stay short`() {
    val effects = CANS.flatMap { it.effects }
    effects.filter { it.id in setOf("minecraft:regeneration", "minecraft:instant_health") }
      .forEach { assertTrue(it.seconds <= HEALING, "${it.id} lasts ${it.seconds} s") }
    effects.filter { it.id == "minecraft:strength" && it.amplifier >= 1 }
      .forEach { assertTrue(it.seconds <= STRENGTH_II, "Strength II lasts ${it.seconds} s") }
  }

  @Test
  fun `wings last 16, 32, or 64 seconds by the kind of can, and Zero 42`() {
    CANS.filter { it.wings > 0 }.forEach { can ->
      val expected = when {
        can.id == "zero" -> WINGS_ZERO
        can.recipe == null -> WINGS_LEGEND
        can.chests != null -> WINGS_EDITION
        else -> WINGS_COMMON
      }
      assertEquals(expected, can.wings, can.id)
    }
  }

  @Test
  fun `a winged can outlasts its own flight`() {
    assertTrue(wingedCanExpiry(0, WINGS_LEGEND) > WINGS_LEGEND * 1000L)
  }

  @Test
  fun `a winged can wears out in flight as many times faster as its flight is shorter`() {
    assertEquals(76, wingedCanSeconds(WINGS_COMMON))
    assertEquals(76_000L, wingedCanExpiry(0, WINGS_COMMON))
    CANS.filter { it.wings > 0 }.forEach { can ->
      assertEquals(wingedCanSeconds(can.wings).toDouble(), can.wings * wingedCanFlightPace(can.wings), 1e-9, can.id)
      assertTrue(can.wings > WINGS_WARNING_FLIGHT_SECONDS, can.id)
    }
  }
}
