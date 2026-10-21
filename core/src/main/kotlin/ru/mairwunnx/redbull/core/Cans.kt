package ru.mairwunnx.redbull.core

/** A potion effect by its game id, like `minecraft:speed`; amplifier 0 is level I. */
data class Effect(val id: String, val seconds: Int, val amplifier: Int = 0)

/**
 * The crafting recipe of a can:
 *
 * ```
 * T D T     T taurine, D dye, F flavour
 * F C F     C empty can, W water bottle
 * S W S     S sugar, or nothing for a sugarfree can
 * ```
 */
data class Recipe(val flavour: String, val dye: String)

/** Loot: the chance a chest generated in one of these biomes holds the can. */
data class Chests(val biomes: List<String>, val chance: Double)

/**
 * One Red Bull. Every can carries the same caffeine, as the real ones do; they differ in buffs and wings.
 * A can without a recipe is found only in chests. Its name, slogan, and texture take their keys from [id].
 */
data class Can(
  val id: String,
  val sugarfree: Boolean,
  val effects: List<Effect>,
  val wings: Int,
  val recipe: Recipe?,
  val chests: Chests?,
) {
  val translation get() = "item.redbull.$id"
  val slogan get() = "item.redbull.$id.slogan"
}

object Ids {
  const val EMPTY_CAN = "empty_can"
  const val WINGED_CAN = "winged_can"
  const val BULL_BILE = "bull_bile"
  const val CRUDE_TAURINE = "crude_taurine"
  const val TAURINE = "taurine"

  /** Items this project adds besides the cans, as written in a [Recipe]. */
  const val TAURINE_ITEM = "redbull:$TAURINE"
}

private fun speed(seconds: Int, level: Int = 1) = Effect("minecraft:speed", seconds, level - 1)
private fun haste(seconds: Int, level: Int = 1) = Effect("minecraft:haste", seconds, level - 1)
private fun strength(seconds: Int, level: Int = 1) = Effect("minecraft:strength", seconds, level - 1)
private fun effect(name: String, seconds: Int, level: Int = 1) = Effect("minecraft:$name", seconds, level - 1)

private val NETHER = listOf("nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas")
private val OCEANS = listOf(
  "ocean", "deep_ocean", "lukewarm_ocean", "deep_lukewarm_ocean", "warm_ocean", "cold_ocean", "deep_cold_ocean",
)

/** Buff lengths by how hard a can is to get: crafted only, crafted or looted, and the chest-only legends. */
const val COMMON = 480
const val EDITION = 720
const val LEGEND = 960

/** What breaks the game stays short: anything that restores health, and Strength II. */
const val HEALING = 16
const val STRENGTH_II = 180

private fun chests(chance: Double, vararg biomes: String) = Chests(biomes.map { "minecraft:$it" }, chance)
private fun chests(chance: Double, biomes: List<String>) = Chests(biomes.map { "minecraft:$it" }, chance)
private fun recipe(flavour: String, dye: String) =
  Recipe(if (":" in flavour) flavour else "minecraft:$flavour", "minecraft:${dye}_dye")

/** Every can, in the order commands and the README list them. */
val CANS = listOf(
  Can("original", false, listOf(speed(COMMON, 2), haste(COMMON)), 8, recipe("sugar", "blue"), null),
  Can("sugarfree", true, listOf(speed(COMMON)), 6, recipe("honeycomb", "light_blue"), null),
  Can("zero", true, listOf(speed(COMMON, 2), haste(COMMON)), 8, recipe(Ids.TAURINE_ITEM, "light_blue"), null),
  Can("red", false, listOf(strength(COMMON)), 8, recipe("melon_slice", "red"), null),
  Can("red_sugarfree", true, listOf(strength(COMMON)), 6, recipe("melon_slice", "red"), null),
  Can("pink_sugarfree", true, listOf(effect("regeneration", HEALING)), 6, recipe("sweet_berries", "pink"), null),
  Can(
    "green", false, listOf(effect("luck", EDITION), speed(EDITION)), 8, recipe("cactus_flower", "green"),
    chests(0.15, "mangrove_swamp", "swamp", "lush_caves"),
  ),
  Can(
    "cherry", false, listOf(effect("luck", EDITION), speed(EDITION, 2)), 12, recipe("pink_petals", "red"),
    chests(0.15, "cherry_grove"),
  ),
  Can(
    "ice", false, listOf(effect("resistance", EDITION)), 10, recipe("packed_ice", "light_blue"),
    chests(0.15, "ice_spikes", "frozen_ocean", "deep_frozen_ocean"),
  ),
  Can(
    "winter_apple", false, listOf(effect("absorption", EDITION, 2)), 8, recipe("apple", "orange"),
    chests(0.15, "snowy_taiga", "grove"),
  ),
  Can(
    "winter_pistachio", false, listOf(effect("regeneration", HEALING, 2)), 8, recipe("sweet_berries", "lime"),
    chests(0.15, "snowy_plains", "snowy_beach"),
  ),
  Can(
    "yellow", false, listOf(speed(EDITION), effect("jump_boost", EDITION)), 10, recipe("sunflower", "yellow"),
    chests(0.15, "desert"),
  ),
  Can(
    "tropical", false, listOf(effect("jump_boost", EDITION, 2)), 12, recipe("cocoa_beans", "yellow"),
    chests(0.15, "jungle", "sparse_jungle", "bamboo_jungle"),
  ),
  Can(
    "amber", false, listOf(haste(EDITION, 2)), 8, recipe("sweet_berries", "orange"),
    chests(0.15, "savanna", "savanna_plateau", "windswept_savanna"),
  ),
  Can(
    "amber_sugarfree", true, listOf(haste(EDITION)), 6, recipe("sweet_berries", "orange"),
    chests(0.15, "savanna", "savanna_plateau", "windswept_savanna"),
  ),
  Can(
    "peach", false, listOf(effect("health_boost", EDITION)), 8, recipe("pink_tulip", "pink"),
    chests(0.15, "plains", "sunflower_plains"),
  ),
  Can(
    "lilac_sugarfree", true, listOf(effect("absorption", EDITION)), 6, recipe("lilac", "magenta"),
    chests(0.15, "forest", "flower_forest", "birch_forest"),
  ),
  Can(
    "purple", false, listOf(effect("invisibility", EDITION), speed(EDITION)), 8, recipe("chorus_fruit", "purple"),
    chests(0.15, "dark_forest", "pale_garden"),
  ),
  Can(
    "sea_blue", false, listOf(effect("dolphins_grace", EDITION), effect("conduit_power", EDITION)), 0,
    recipe("sea_pickle", "cyan"), chests(0.15, OCEANS),
  ),
  Can(
    "coconut", false, listOf(effect("saturation", 1), speed(EDITION)), 8, recipe("milk_bucket", "white"),
    chests(0.15, "beach"),
  ),
  Can(
    "coconut_sugarfree", true, listOf(effect("water_breathing", EDITION)), 6, recipe("milk_bucket", "white"),
    chests(0.15, "beach"),
  ),
  Can(
    "blue", false, listOf(effect("night_vision", EDITION), speed(EDITION)), 8, recipe("blue_orchid", "blue"),
    chests(0.15, "deep_dark"),
  ),
  // The legends: no recipe, only chests in the hardest places.
  Can(
    "ruby", false, listOf(effect("fire_resistance", LEGEND), strength(STRENGTH_II, 2)), 0, null, chests(0.08, NETHER),
  ),
  Can(
    "summer", false, listOf(speed(LEGEND, 3)), 15, null, chests(0.05, "badlands", "eroded_badlands", "wooded_badlands"),
  ),
  Can(
    "glacier", false, listOf(effect("slow_falling", LEGEND), effect("resistance", LEGEND)), 15, null,
    chests(0.05, "frozen_peaks", "jagged_peaks", "snowy_slopes"),
  ),
  Can("spring", false, listOf(effect("slow_falling", LEGEND)), 30, null, chests(0.03, "cherry_grove")),
)

fun can(id: String): Can? = CANS.firstOrNull { it.id == id }
