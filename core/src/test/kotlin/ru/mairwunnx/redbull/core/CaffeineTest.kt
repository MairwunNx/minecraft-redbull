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
    assertEquals(16, crashDelay(can("pink_sugarfree")!!))
    assertEquals(180, crashDelay(can("blue")!!))
  }

  @Test
  fun `the effect shows the stage and the time until the caffeine is gone`() {
    assertEquals(0, Caffeine(30.0, 0).effectLevel(rules))
    assertEquals(1, Caffeine(60.0, 0).effectLevel(rules))
    assertEquals(2, Caffeine(80.0, 0).effectLevel(rules))
    assertEquals(180, Caffeine(30.0, 0).secondsLeft(rules))
    assertEquals(0, Caffeine().secondsLeft(rules))
  }

  @Test
  fun `any caffeine keeps a player awake`() {
    assertTrue(Caffeine(0.5, 0).sleepless())
    assertFalse(Caffeine().sleepless())
  }

  @Test
  fun `only a can with sugar crashes`() {
    assertTrue(crashes(can("original")!!, Stage.CRASH))
    assertFalse(crashes(can("original")!!, Stage.JITTERS))
    assertFalse(crashes(can("red_sugarfree")!!, Stage.CRASH))
  }

  @Test
  fun `the sugar pit comes once the caffeine is gone, as deep as the cans with sugar since`() {
    var caffeine = rules.drink(Caffeine(), 0, sugar = true).caffeine
    caffeine = rules.drink(caffeine, 0, sugar = false).caffeine
    caffeine = rules.drink(caffeine, 0, sugar = true).caffeine
    assertEquals(2, caffeine.sugar)
    assertEquals(0, caffeine.sugarPitDue())
    assertEquals(0, caffeine.now(caffeine.secondsLeft(rules) * 20L - 20, rules).sugarPitDue())
    assertEquals(2, caffeine.now(caffeine.secondsLeft(rules) * 20L, rules).sugarPitDue())
    assertEquals(0, rules.drink(Caffeine(), 0).caffeine.sugar)
    assertEquals(SUGAR_PIT_DEEPEST, (1..7).fold(Caffeine()) { it, _ -> rules.drink(it, 0, sugar = true).caffeine }.sugar)
  }

  @Test
  fun `a deeper sugar pit is stronger and longer`() {
    assertEquals(listOf(Effect("minecraft:hunger", 30), Effect("minecraft:slowness", 15)), sugarPit(1))
    assertEquals(listOf(60, 72, 90, 105), (2..5).map { sugarPit(it)[0].seconds })
    assertEquals(listOf(30, 36, 45, 53), (2..5).map { sugarPit(it)[1].seconds })
    assertEquals(listOf(1, 2, 3, 4), (2..5).map { sugarPit(it)[1].amplifier })
    assertEquals(sugarPit(5), sugarPit(9))
    assertEquals("redbull.sugar_pit.arcade.3", SugarPitTexts.ARCADE.key(3))
    assertEquals(SugarPitTexts.FEELINGS, SugarPitTexts.of(" Feelings"))
    assertEquals(null, SugarPitTexts.of("loud"))
  }

  @Test
  fun `a sugarfree can is sweetened with honeycomb`() {
    assertEquals("minecraft:sugar", can("red")!!.sweetener)
    assertEquals("minecraft:honeycomb", can("red_sugarfree")!!.sweetener)
  }
}
