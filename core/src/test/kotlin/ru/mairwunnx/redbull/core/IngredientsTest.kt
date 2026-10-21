package ru.mairwunnx.redbull.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IngredientsTest {
  @Test
  fun `looting raises the bile chance`() {
    assertEquals(0.05, Bile.chance(0), 1e-9)
    assertEquals(0.11, Bile.chance(3), 1e-9)
    assertEquals(0.05, Bile.chance(-1), 1e-9)
  }

  @Test
  fun `bull mode needs the other bottle within a minute`() {
    assertFalse(Bottles.bullMode(null, 1000))
    assertTrue(Bottles.bullMode(0, 60_000))
    assertFalse(Bottles.bullMode(0, 60_001))
    assertFalse(Bottles.bullMode(5000, 1000))
  }
}
