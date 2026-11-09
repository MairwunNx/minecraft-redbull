package ru.mairwunnx.redbull.paper

import ru.mairwunnx.redbull.core.Rules
import kotlin.test.Test
import kotlin.test.assertEquals

class MetricsCountsTest {
  @Test
  fun `a drain empties the counts for the next period`() {
    val counts = MetricsCounts()
    counts.drank("original", 80)
    counts.drank("original", 80)
    counts.drank("summer", 80)
    counts.caffeine(27)
    counts.overdose()
    assertEquals(3, counts.drainCans())
    assertEquals(mapOf("original" to 2, "summer" to 1), counts.drainKinds())
    assertEquals(267, counts.drainCaffeine())
    assertEquals(1, counts.drainOverdoses())
    assertEquals(0, counts.drainCans())
    assertEquals(emptyMap(), counts.drainKinds())
    assertEquals(0, counts.drainCaffeine())
    assertEquals(0, counts.drainOverdoses())
  }

  @Test
  fun `only the cans' own ids reach the pie`() {
    val counts = MetricsCounts()
    counts.drank("someone's text", 80)
    assertEquals(emptyMap(), counts.drainKinds())
    assertEquals(1, counts.drainCans())
  }

  @Test
  fun `flight sends whole seconds and carries the rest`() {
    val counts = MetricsCounts()
    counts.glided(1500)
    counts.glided(1200)
    assertEquals(2, counts.drainFlightSeconds())
    counts.glided(300)
    assertEquals(1, counts.drainFlightSeconds())
    assertEquals(0, counts.drainFlightSeconds())
    counts.glided(-5)
    assertEquals(0, counts.drainFlightSeconds())
  }

  @Test
  fun `a can is 80 mg of caffeine, crude taurine a third of it`() {
    val rules = Rules()
    assertEquals(80, Stats.milligrams(rules.perCan, rules))
    assertEquals(27, Stats.milligrams(rules.perCrudeTaurine, rules))
  }
}
