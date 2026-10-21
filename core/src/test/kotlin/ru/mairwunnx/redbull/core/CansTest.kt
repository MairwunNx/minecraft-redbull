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
}
