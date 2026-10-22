package ru.mairwunnx.redbull.core

import kotlin.random.Random

/**
 * A wandering trader brings a can or two: crafted ones for 6 emeralds, the Editions also found in chests for 10.
 * The legends stay something you find, never buy.
 */
object Trades {
  const val CRAFTED_PRICE = 6
  const val EDITION_PRICE = 10
  const val MAX_USES = 3
  const val MIN_OFFERS = 1
  const val MAX_OFFERS = 2

  val forSale get() = CANS.filter { it.recipe != null }

  fun price(can: Can) = if (can.chests == null) CRAFTED_PRICE else EDITION_PRICE

  /** The cans one trader brings, different ones. */
  fun pick(random: Random): List<Can> = forSale.shuffled(random).take(random.nextInt(MIN_OFFERS, MAX_OFFERS + 1))
}

/**
 * Cans in a fishing rod's junk, beside the game's own: an empty can, or a crumpled one of any flavour. Their weights
 * sit next to the game's junk, which weighs 110 in all, so about one junk catch in ten is a can.
 */
object Fishing {
  const val VANILLA_JUNK_WEIGHT = 110
  const val EMPTY_WEIGHT = 4
  const val CRUMPLED_WEIGHT = 8

  /** On a platform that can only swap a junk catch afterwards: the chance it becomes a can. */
  val share get() = (EMPTY_WEIGHT + CRUMPLED_WEIGHT).toDouble() / (VANILLA_JUNK_WEIGHT + EMPTY_WEIGHT + CRUMPLED_WEIGHT)

  /** Which can a catch becomes, once it is one: empty, or crumpled of a random flavour. */
  fun pick(random: Random): String =
    if (random.nextInt(EMPTY_WEIGHT + CRUMPLED_WEIGHT) < EMPTY_WEIGHT) Ids.EMPTY_CAN else CANS.random(random).crumpled
}

/** Red Bull's advancements, each granted by code on every platform; their JSON sits in core's data. */
object Advancements {
  const val ROOT = "root"
  const val WINGS = "wings"
  const val COLLECTOR = "collector"
  const val EDGE = "edge"
  const val SUGAR_PIT = "sugar_pit"
  const val LEGEND = "legend"

  /** “On the Edge”: this much caffeine, or more, and alive. */
  const val EDGE_LEVEL = 90.0

  fun collector(collected: Set<String>) = CANS.all { it.id in collected }

  fun legend(can: Can) = can.recipe == null
}
