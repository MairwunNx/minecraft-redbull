package ru.mairwunnx.redbull.core

import kotlin.math.roundToInt

/** How caffeine builds up and wears off. Clocks count ticks the player has played, so it doesn't wear off offline. */
data class Rules(
  val perCan: Double = 30.0,
  val perCrudeTaurine: Double = 10.0,
  /** One point every 6 seconds: 30 points, a can, wear off in 3 minutes. */
  val decayPerTick: Double = 1.0 / 120,
  val jitters: Double = 50.0,
  val crash: Double = 75.0,
  /** Above this, the heart gives out. Exactly at it, the player lives. */
  val limit: Double = 100.0,
  val milk: Double = 20.0,
)

enum class Stage { CALM, JITTERS, CRASH, OVERDOSE }

/**
 * The caffeine in a player at clock [at]; read it through [now], which applies what wore off since. [sugar] counts
 * the cans with sugar drunk since the caffeine was last gone, up to [SUGAR_PIT_DEEPEST]: the depth of the sugar pit
 * that comes when it's gone again.
 */
data class Caffeine(val level: Double = 0.0, val at: Long = 0, val sugar: Int = 0) {
  fun now(clock: Long, rules: Rules) =
    copy(level = (level - (clock - at).coerceAtLeast(0) * rules.decayPerTick).coerceAtLeast(0.0), at = clock)

  fun plus(points: Double, clock: Long, rules: Rules) = now(clock, rules).let { it.copy(level = it.level + points) }

  fun stage(rules: Rules) = when {
    level > rules.limit -> Stage.OVERDOSE
    level >= rules.crash -> Stage.CRASH
    level >= rules.jitters -> Stage.JITTERS
    else -> Stage.CALM
  }
}

/** What drinking a can did: the new caffeine and the stage it reached. */
data class Drink(val caffeine: Caffeine, val stage: Stage) {
  val fatal get() = stage == Stage.OVERDOSE
}

/** A can: its caffeine, and with [sugar] one more can of it for the sugar pit. */
fun Rules.drink(before: Caffeine, clock: Long, sugar: Boolean = false) = before.plus(perCan, clock, this)
  .let { if (sugar) it.copy(sugar = (it.sugar + 1).coerceAtMost(SUGAR_PIT_DEEPEST)) else it }
  .let { Drink(it, it.stage(this)) }

fun Rules.drinkCrudeTaurine(before: Caffeine, clock: Long) =
  before.plus(perCrudeTaurine, clock, this).let { Drink(it, it.stage(this)) }

fun Rules.milk(before: Caffeine, clock: Long) = before.plus(-milk, clock, this).let { it.copy(level = it.level.coerceAtLeast(0.0)) }

/**
 * The Caffeine effect the mods show: its level is the stage (I calm, II jitters, III crash) and its duration the
 * time until the caffeine wears off, so a player sees when the next can is safe.
 */
fun Caffeine.effectLevel(rules: Rules): Int = when (stage(rules)) {
  Stage.CALM -> 0
  Stage.JITTERS -> 1
  Stage.CRASH, Stage.OVERDOSE -> 2
}

fun Caffeine.secondsLeft(rules: Rules): Int = kotlin.math.ceil(level / rules.decayPerTick / 20).toInt()

/** Too wired to sleep: any caffeine at all keeps a player out of bed. */
fun Caffeine.sleepless(): Boolean = level > 0

const val CRASH_SECONDS = 60
const val CRASH_DELAY_LIMIT = 180

/** The crash after a can drunk in the crash stage: a minute slower and weaker. */
fun crash(): List<Effect> =
  listOf(Effect("minecraft:slowness", CRASH_SECONDS, 1), Effect("minecraft:weakness", CRASH_SECONDS, 0))

/** The crash after too much caffeine follows only a can with sugar: no sugar, no crash. */
fun crashes(can: Can, stage: Stage): Boolean = !can.sugarfree && stage == Stage.CRASH

const val SUGAR_PIT_DEEPEST = 5

/** How much longer each depth of the sugar pit lasts than the first. */
private val SUGAR_PIT_LENGTHS = listOf(1.0, 2.0, 2.4, 3.0, 3.5)

/**
 * The sugar pit: once the caffeine is gone, so is the sugar, and the player is hungry and slow. Every can with sugar
 * drunk since the caffeine was last gone makes it deeper: Hunger and Slowness a level higher each, and longer, down to
 * Hunger V and Slowness V at [SUGAR_PIT_DEEPEST] cans. A sugarfree can adds none.
 */
fun sugarPit(depth: Int): List<Effect> {
  val level = depth.coerceIn(1, SUGAR_PIT_DEEPEST)
  val length = SUGAR_PIT_LENGTHS[level - 1]
  return listOf(
    Effect("minecraft:hunger", (30 * length).roundToInt(), level - 1),
    Effect("minecraft:slowness", (15 * length).roundToInt(), level - 1),
  )
}

/** The depth of the pit due now: the cans of sugar, once the caffeine is gone; 0 while it lasts or without sugar. */
fun Caffeine.sugarPitDue(): Int = if (level > 0) 0 else sugar

/** How the sugar pit announces itself: a game's combo or how the player feels. */
enum class SugarPitTexts(val id: String) {
  /** “Double sugar pit!”, “Mega sugar pit!!!”: the default. */
  ARCADE("arcade"),

  /** “Deep sugar pit. Your legs turn to jelly.” */
  FEELINGS("feelings");

  /** The action bar's text for a pit of [depth]. */
  fun key(depth: Int) = "redbull.sugar_pit.$id.${depth.coerceIn(1, SUGAR_PIT_DEEPEST)}"

  companion object {
    fun of(id: String): SugarPitTexts? = entries.firstOrNull { it.id == id.trim().lowercase() }
  }
}

/** The crash comes when the can's longest buff ends, but no later than three minutes after the drink. */
fun crashDelay(can: Can): Int = can.effects.maxOf { it.seconds }.coerceAtMost(CRASH_DELAY_LIMIT)
