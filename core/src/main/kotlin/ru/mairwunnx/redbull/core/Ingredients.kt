package ru.mairwunnx.redbull.core

/** Bull bile: the first step to taurine. Adult cows and mooshrooms killed by a player drop it. */
object Bile {
  const val CHANCE = 0.05
  const val PER_LOOTING = 0.02

  fun chance(looting: Int) = CHANCE + PER_LOOTING * looting.coerceAtLeast(0)
}

/** Drinking the bottles is a bad idea; drinking both within a minute is a worse one. */
object Bottles {
  val BILE = listOf(Effect("minecraft:nausea", 15), Effect("minecraft:hunger", 30, 1))
  val CRUDE_TAURINE = listOf(
    Effect("minecraft:nausea", 10), Effect("minecraft:poison", 4), Effect("minecraft:speed", 15),
  )
  val BULL_MODE = listOf(Effect("minecraft:strength", 10, 1), Effect("minecraft:nausea", 20))

  const val BULL_MODE_WINDOW_MILLIS = 60_000L

  /** Bull mode starts when the other bottle went down within the window before this one. */
  fun bullMode(otherAt: Long?, now: Long) = otherAt != null && now - otherAt in 0..BULL_MODE_WINDOW_MILLIS
}

/** A winged can lasts this long after the drink, flown or not; flying wears it out sooner. */
const val WINGS_GRACE_MILLIS = 60_000L
