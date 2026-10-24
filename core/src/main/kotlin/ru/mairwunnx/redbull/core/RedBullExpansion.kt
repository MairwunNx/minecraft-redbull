package ru.mairwunnx.redbull.core

import chat.allay.expansion.AllayApi
import chat.allay.expansion.AllayExpansion
import chat.allay.expansion.AllayTool
import chat.allay.expansion.Schema
import chat.allay.expansion.ToolCall
import chat.allay.expansion.ToolException
import chat.allay.expansion.ToolResult

/**
 * What every platform tells [RedBullExpansion] about itself. A class of its own, so a platform can set it without
 * loading the expansion, whose interface exists only where Allay is installed.
 */
object RedBullInfo {
  /** The plugin's or mod's version, set when it starts. */
  @Volatile var version = "dev"
}

/**
 * Red Bull for Allay, the server's AI assistant (`chat.allay:allay-expansion`): one tool, `x_redbull_recipes`, that
 * tells the model how each can is made and where it's found, and a few lines of prompt that send it there. Allay finds
 * it through `META-INF/services/chat.allay.expansion.AllayExpansion` on Paper, Fabric, and NeoForge alike; without
 * Allay nothing loads it, and Red Bull runs as before.
 */
class RedBullExpansion : AllayExpansion {
  override fun id() = "redbull"

  override fun requiresApi(): String = AllayApi.VERSION

  override fun name() = "Red Bull"

  override fun version() = RedBullInfo.version

  override fun tools(): List<AllayTool> = listOf(Recipes)

  override fun promptExpansion() =
    "Red Bull adds 26 energy drink cans (redbull:original and so on), each a potion with buffs and most with wings to " +
      "glide on. The way to a can: bull bile from cows a player kills, brewed into a water bottle as crude taurine, " +
      "smelted into taurine, then crafted with an empty can, a flavour, a dye, sugar or honeycomb, and a water bottle. " +
      "Four cans are legends found only in chests. Every can adds ${RULES.perCan.toInt()} caffeine, which wears off by " +
      "a point every 6 seconds of play; above ${RULES.limit.toInt()} the heart gives out, and milk takes " +
      "${RULES.milk.toInt()}. Before you explain how to make or find a can, call x_redbull_recipes: never guess a recipe."

  /** `x_redbull_recipes`: the ingredients' way, and one can or every can. */
  object Recipes : AllayTool {
    override fun name() = "recipes"

    override fun description() =
      "How Red Bull cans and their ingredients are made and found on this server. Returns `ingredients` (bull bile, " +
        "crude taurine, taurine, the empty can: drops, brewing, smelting, crafting), `grid` (the crafting shape every " +
        "can shares), and `cans`: each can's id, English name, flavour, and its own dye, flavour item, and sweetener, " +
        "or for a chest-only legend the biomes and chance of its chests. Pass `can` for one can in full, with its " +
        "buffs, wings, chests, and trader price; leave it out for every can in short."

    override fun parameters(): Schema = Schema.arguments()
      .string("can", "One can: its id like redbull:cherry, or its name or flavour like Cherry Edition", false)

    override fun inPublic() = true

    override fun run(call: ToolCall): ToolResult {
      val asked = call.args().string("can")
      val result = linkedMapOf<String, Any>("ingredients" to INGREDIENTS, "grid" to GRID)
      if (asked == null) {
        result["cans"] = CANS.map { short(it) }
        result["elsewhere"] = ELSEWHERE
        return ToolResult.of(result).summary("${CANS.size} cans")
      }
      val found = find(asked)
      if (found.isEmpty()) throw ToolException("no can matches \"$asked\"; the cans are ${CANS.joinToString { "redbull:${it.id}" }}")
      result["cans"] = found.map { full(it) }
      return ToolResult.of(result).summary(found.joinToString { Names.name(it) })
    }
  }

  companion object {
    private val RULES = Rules()

    /** How long a furnace takes to evaporate crude taurine, as every platform's recipe says. */
    private const val EVAPORATE_SECONDS = 10

    private val INGREDIENTS = linkedMapOf(
      "redbull:${Ids.BULL_BILE}" to linkedMapOf(
        "from" to "an adult cow or mooshroom killed by a player",
        "chance" to Bile.CHANCE, "per_looting_level" to Bile.PER_LOOTING,
      ),
      "redbull:${Ids.CRUDE_TAURINE}" to linkedMapOf("brewing" to linkedMapOf("bottle" to "water bottle", "reagent" to "redbull:${Ids.BULL_BILE}")),
      "redbull:${Ids.TAURINE}" to linkedMapOf("furnace" to "redbull:${Ids.CRUDE_TAURINE}", "seconds" to EVAPORATE_SECONDS),
      "redbull:${Ids.EMPTY_CAN}" to linkedMapOf(
        "shape" to listOf("I I", "I I", " C "), "key" to linkedMapOf("I" to "iron_nugget", "C" to "copper_nugget"),
      ),
    )

    private val GRID = linkedMapOf(
      "shape" to listOf("TDT", "FCF", "SWS"),
      "key" to linkedMapOf(
        "T" to "redbull:${Ids.TAURINE}", "D" to "the can's dye", "F" to "the can's flavour item", "C" to "redbull:${Ids.EMPTY_CAN}",
        "S" to "sugar, or honeycomb for a sugarfree can", "W" to "water bottle",
      ),
    )

    private val ELSEWHERE = listOf(
      "A wandering trader sells ${Trades.MIN_OFFERS} or ${Trades.MAX_OFFERS} crafted cans: ${Trades.CRAFTED_PRICE} emeralds, " +
        "${Trades.EDITION_PRICE} for an Edition also found in chests; never a legend",
      "Fishing junk holds empty and crumpled cans",
      "A crumpled can, what a drink leaves, smelts into $CRUMPLED_NUGGETS iron nuggets",
    )

    private fun short(can: Can): Map<String, Any> {
      val entry = linkedMapOf<String, Any>("id" to "redbull:${can.id}", "name" to Names.name(can), "flavour" to Names.flavour(can))
      val recipe = can.recipe
      if (recipe == null) {
        entry["chest_only"] = true
      } else {
        entry["dye"] = bare(recipe.dye)
        entry["flavour_item"] = bare(recipe.flavour)
        entry["sweetener"] = bare(can.sweetener)
      }
      can.chests?.let { entry["chests"] = chests(it) }
      return entry
    }

    private fun full(can: Can): Map<String, Any> {
      val entry = linkedMapOf<String, Any>("id" to "redbull:${can.id}", "name" to Names.name(can), "flavour" to Names.flavour(can))
      val recipe = can.recipe
      if (recipe == null) {
        entry["chest_only"] = true
      } else {
        entry["recipe"] = linkedMapOf(
          "shape" to GRID.getValue("shape"),
          "key" to linkedMapOf(
            "T" to "redbull:${Ids.TAURINE}", "D" to bare(recipe.dye), "F" to bare(recipe.flavour),
            "C" to "redbull:${Ids.EMPTY_CAN}", "S" to bare(can.sweetener), "W" to "water bottle",
          ),
        )
        entry["trader_price"] = Trades.price(can)
      }
      entry["effects"] = can.effects.map { linkedMapOf("effect" to bare(it.id), "level" to it.amplifier + 1, "seconds" to it.seconds) }
      entry["wings_seconds"] = can.wings
      entry["sugarfree"] = can.sugarfree
      can.chests?.let { entry["chests"] = chests(it) }
      return entry
    }

    private fun chests(chests: Chests) = linkedMapOf("biomes" to chests.biomes.map(::bare), "chance" to chests.chance)

    private fun bare(id: String) = id.removePrefix("minecraft:")

    /** Cans by id, name, or flavour, as a player might put it: “cherry”, “Red Bull Cherry Edition”, “Cherry-Sakura”. */
    fun find(asked: String): List<Can> {
      val words = fold(asked.removePrefix("redbull:"))
      CANS.firstOrNull { fold(it.id) == words }?.let { return listOf(it) }
      val named = CANS.filter { words == fold(Names.name(it)) || words == fold(Names.name(it).removePrefix("Red Bull ")) }
      if (named.isNotEmpty()) return named
      return CANS.filter { words == fold(Names.flavour(it)) }
    }

    private fun fold(text: String) = text.lowercase().filter { it.isLetterOrDigit() }
  }
}

/** The cans' English names and flavours from core's `en_us.json`, which every platform's jar carries. */
internal object Names {
  private val texts: Map<String, String> by lazy {
    val json = Names::class.java.getResourceAsStream("/assets/redbull/lang/en_us.json")?.use { it.readBytes().decodeToString() } ?: ""
    Regex("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(json)
      .associate { it.groupValues[1] to it.groupValues[2].replace("\\\"", "\"").replace("\\\\", "\\") }
  }

  fun name(can: Can) = texts[can.translation] ?: can.id

  fun flavour(can: Can) = texts["${can.translation}.flavour"] ?: can.id
}
