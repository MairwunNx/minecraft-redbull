package ru.mairwunnx.redbull.core

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

/** The caffeine in a player at clock [at]; read it through [now], which applies what wore off since. */
data class Caffeine(val level: Double = 0.0, val at: Long = 0) {
  fun now(clock: Long, rules: Rules) =
    Caffeine((level - (clock - at).coerceAtLeast(0) * rules.decayPerTick).coerceAtLeast(0.0), clock)

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

fun Rules.drink(before: Caffeine, clock: Long) = before.plus(perCan, clock, this).let { Drink(it, it.stage(this)) }

fun Rules.drinkCrudeTaurine(before: Caffeine, clock: Long) =
  before.plus(perCrudeTaurine, clock, this).let { Drink(it, it.stage(this)) }

fun Rules.milk(before: Caffeine, clock: Long) = before.plus(-milk, clock, this).let { it.copy(level = it.level.coerceAtLeast(0.0)) }

const val CRASH_SECONDS = 60
const val CRASH_DELAY_LIMIT = 180

/** The crash after a can drunk in the crash stage: a minute slower and weaker. */
fun crash(): List<Effect> =
  listOf(Effect("minecraft:slowness", CRASH_SECONDS, 1), Effect("minecraft:weakness", CRASH_SECONDS, 0))

/** The crash comes when the can's longest buff ends, but no later than three minutes after the drink. */
fun crashDelay(can: Can): Int = can.effects.maxOf { it.seconds }.coerceAtMost(CRASH_DELAY_LIMIT)
