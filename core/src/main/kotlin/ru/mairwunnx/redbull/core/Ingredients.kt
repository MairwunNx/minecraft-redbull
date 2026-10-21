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

/** A crumpled can smelts back into this many iron nuggets: the iron of a can, not its copper. */
const val CRUMPLED_NUGGETS = 2

/** When wings run out in the air, the player floats down under Slow Falling for this long. */
const val SOFT_LANDING_SECONDS = 5

/** A winged can lasts its seconds of flight plus this long after the drink, flown or not; flying wears it out sooner. */
const val WINGS_GRACE_MILLIS = 60_000L

fun wingedCanExpiry(drunkAt: Long, wings: Int): Long = drunkAt + wings * 1000L + WINGS_GRACE_MILLIS

/** The colours every platform shares: the bottles' tint and the effects' particles and icons' frames. */
object Colors {
  const val BILE = 0x9FB230
  const val CRUDE_TAURINE = 0xEFE3B0

  /** Coffee brown. */
  const val CAFFEINE = 0x6F4E37

  /** The sky a winged can promises. */
  const val WINGS = 0x8FD8FF
}
