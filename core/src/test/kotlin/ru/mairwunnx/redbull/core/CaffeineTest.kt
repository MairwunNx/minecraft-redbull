package ru.mairwunnx.redbull.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CaffeineTest {
  private val rules = Rules()
  private val minute = 1200L

  private fun drinks(vararg at: Long): Drink {
    var caffeine = Caffeine()
    lateinit var last: Drink
    for (clock in at) {
      last = rules.drink(caffeine, clock)
      caffeine = last.caffeine
    }
    return last
  }

  @Test
  fun `one can is calm, two jitter, three crash`() {
    assertEquals(Stage.CALM, drinks(0).stage)
    assertEquals(Stage.JITTERS, drinks(0, 0).stage)
    assertEquals(Stage.CRASH, drinks(0, 0, 0).stage)
  }

  @Test
  fun `four cans within two minutes kill`() {
    assertTrue(drinks(0, 0, 0, 0).fatal)
    assertTrue(drinks(0, minute / 2, minute, 2 * minute - 1).fatal)
  }

  @Test
  fun `the fourth can after two minutes lands exactly on the limit and the player lives`() {
    val last = drinks(0, 0, 0, 2 * minute)
    assertEquals(100.0, last.caffeine.level, 1e-9)
    assertFalse(last.fatal)
  }

  @Test
  fun `a can wears off in three minutes`() {
    val caffeine = rules.drink(Caffeine(), 0).caffeine
    assertEquals(0.0, caffeine.now(3 * minute, rules).level, 1e-9)
    assertEquals(15.0, caffeine.now(3 * minute / 2, rules).level, 1e-9)
  }

  @Test
  fun `a clock that went back wears nothing off`() {
    val caffeine = Caffeine(40.0, 1000)
    assertEquals(40.0, caffeine.now(500, rules).level)
  }

  @Test
  fun `milk takes twenty points but never below zero`() {
    assertEquals(40.0, rules.milk(Caffeine(60.0, 0), 0).level)
    assertEquals(0.0, rules.milk(Caffeine(5.0, 0), 0).level)
  }

  @Test
  fun `crude taurine adds a third of a can`() {
    assertEquals(10.0, rules.drinkCrudeTaurine(Caffeine(), 0).caffeine.level)
  }

  @Test
  fun `the crash lasts a minute and comes when the buffs end, three minutes at most`() {
    assertEquals(listOf(60, 60), crash().map { it.seconds })
    assertEquals(listOf("minecraft:slowness", "minecraft:weakness"), crash().map { it.id })
    assertEquals(90, crashDelay(can("pink_sugarfree")!!))
    assertEquals(180, crashDelay(can("blue")!!))
  }
}
