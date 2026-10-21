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

private fun chests(chance: Double, vararg biomes: String) = Chests(biomes.map { "minecraft:$it" }, chance)
private fun chests(chance: Double, biomes: List<String>) = Chests(biomes.map { "minecraft:$it" }, chance)
private fun recipe(flavour: String, dye: String) =
  Recipe(if (":" in flavour) flavour else "minecraft:$flavour", "minecraft:${dye}_dye")

/** Every can, in the order commands and the README list them. */
val CANS = listOf(
  Can("original", false, listOf(speed(30, 2), haste(30)), 8, recipe("sugar", "blue"), null),
  Can("sugarfree", true, listOf(speed(30)), 6, recipe("honeycomb", "light_blue"), null),
  Can("zero", true, listOf(speed(30, 2), haste(30)), 8, recipe(Ids.TAURINE_ITEM, "light_blue"), null),
  Can("red", false, listOf(strength(20)), 8, recipe("melon_slice", "red"), null),
  Can("red_sugarfree", true, listOf(strength(15)), 6, recipe("melon_slice", "red"), null),
  Can("pink_sugarfree", true, listOf(effect("regeneration", 10)), 6, recipe("sweet_berries", "pink"), null),
  Can(
    "green", false, listOf(effect("luck", 180), speed(20)), 8, recipe("cactus_flower", "green"),
    chests(0.15, "mangrove_swamp", "swamp", "lush_caves"),
  ),
  Can(
    "cherry", false, listOf(effect("luck", 180), speed(20, 2)), 12, recipe("pink_petals", "red"),
    chests(0.15, "cherry_grove"),
  ),
  Can(
    "ice", false, listOf(effect("resistance", 30)), 10, recipe("packed_ice", "light_blue"),
    chests(0.15, "ice_spikes", "frozen_ocean", "deep_frozen_ocean"),
  ),
  Can(
    "winter_apple", false, listOf(effect("absorption", 30, 2)), 8, recipe("apple", "orange"),
    chests(0.15, "snowy_taiga", "grove"),
  ),
  Can(
    "winter_pistachio", false, listOf(effect("regeneration", 8, 2)), 8, recipe("sweet_berries", "lime"),
    chests(0.15, "snowy_plains", "snowy_beach"),
  ),
  Can(
    "yellow", false, listOf(speed(30), effect("jump_boost", 30)), 10, recipe("sunflower", "yellow"),
    chests(0.15, "desert"),
  ),
  Can(
    "tropical", false, listOf(effect("jump_boost", 30, 2)), 12, recipe("cocoa_beans", "yellow"),
    chests(0.15, "jungle", "sparse_jungle", "bamboo_jungle"),
  ),
  Can(
    "amber", false, listOf(haste(30, 2)), 8, recipe("sweet_berries", "orange"),
    chests(0.15, "savanna", "savanna_plateau", "windswept_savanna"),
  ),
  Can(
    "amber_sugarfree", true, listOf(haste(30)), 6, recipe("sweet_berries", "orange"),
    chests(0.15, "savanna", "savanna_plateau", "windswept_savanna"),
  ),
  Can(
    "peach", false, listOf(effect("health_boost", 60)), 8, recipe("pink_tulip", "pink"),
    chests(0.15, "plains", "sunflower_plains"),
  ),
  Can(
    "lilac_sugarfree", true, listOf(effect("absorption", 30)), 6, recipe("lilac", "magenta"),
    chests(0.15, "forest", "flower_forest", "birch_forest"),
  ),
  Can(
    "purple", false, listOf(effect("invisibility", 30), speed(30)), 8, recipe("chorus_fruit", "purple"),
    chests(0.15, "dark_forest", "pale_garden"),
  ),
  Can(
    "sea_blue", false, listOf(effect("dolphins_grace", 30), effect("conduit_power", 30)), 0,
    recipe("sea_pickle", "cyan"), chests(0.15, OCEANS),
  ),
  Can(
    "coconut", false, listOf(effect("saturation", 1), speed(30)), 8, recipe("milk_bucket", "white"),
    chests(0.15, "beach"),
  ),
  Can(
    "coconut_sugarfree", true, listOf(effect("water_breathing", 60)), 6, recipe("milk_bucket", "white"),
    chests(0.15, "beach"),
  ),
  Can(
    "blue", false, listOf(effect("night_vision", 60), speed(30)), 8, recipe("blue_orchid", "blue"),
    chests(0.15, "deep_dark"),
  ),
  // The legends: no recipe, only chests in the hardest places.
  Can(
    "ruby", false, listOf(effect("fire_resistance", 90), strength(10, 2)), 0, null, chests(0.08, NETHER),
  ),
  Can(
    "summer", false, listOf(speed(15, 3)), 15, null, chests(0.05, "badlands", "eroded_badlands", "wooded_badlands"),
  ),
  Can(
    "glacier", false, listOf(effect("slow_falling", 30), effect("resistance", 15)), 15, null,
    chests(0.05, "frozen_peaks", "jagged_peaks", "snowy_slopes"),
  ),
  Can("spring", false, listOf(effect("slow_falling", 30)), 30, null, chests(0.03, "cherry_grove")),
)

fun can(id: String): Can? = CANS.firstOrNull { it.id == id }
