package ru.mairwunnx.redbull.core

import chat.allay.expansion.AllayApi
import chat.allay.expansion.AllayExpansion
import chat.allay.expansion.AllayExpansions
import chat.allay.expansion.Asker
import chat.allay.expansion.AllayTool
import chat.allay.expansion.Schema
import chat.allay.expansion.ToolCall
import chat.allay.expansion.ToolException
import chat.allay.expansion.ToolResult
import java.util.concurrent.ConcurrentHashMap

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

  /** 1.0, Allay 6.12: [language] reads 1.3's `Asker.language` only where Allay has it. */
  override fun requiresApi(): String = "1.0.0"

  override fun name() = "Red Bull"

  override fun version() = RedBullInfo.version

  override fun tools(): List<AllayTool> = listOf(Recipes)

  override fun promptExpansion() =
    "Red Bull adds 26 energy drink cans (redbull:original and so on), each a potion with buffs and most with wings to " +
      "glide on. The way to a can: bull bile from cows a player kills, brewed into a water bottle as crude taurine, " +
      "smelted into taurine, then crafted with an empty can, a flavour, a dye, sugar or honeycomb, and a water bottle. " +
      "Four cans are legends found only in chests. Every can adds ${RULES.perCan.toInt()} caffeine, which wears off by " +
      "a point every 6 seconds of play; above ${RULES.limit.toInt()} the heart gives out, and milk takes " +
      "${RULES.milk.toInt()}. Before you explain how to make or find a can, call x_redbull_recipes: never guess a recipe. " +
      "Call Red Bull's items by the names it gives in `names`, as the game shows them to the player; never translate an id."

  /** `x_redbull_recipes`: the ingredients' way, and one can or every can. */
  object Recipes : AllayTool {
    override fun name() = "recipes"

    override fun description() =
      "How Red Bull cans and their ingredients are made and found on this server. Returns `ingredients` (bull bile, " +
        "crude taurine, taurine, the empty can: drops, brewing, smelting, crafting), `grid` (the crafting shape every " +
        "can shares), `names` (those items' names in the player's language), and `cans`: each can's id, name (English " +
        "on every can), flavour in the player's language, and its own dye, flavour item, and sweetener, " +
        "or for a chest-only legend the biomes and chance of its chests. Pass `can` for one can in full, with its " +
        "buffs, wings, chests, and trader price; leave it out for every can in short."

    override fun parameters(): Schema = Schema.arguments()
      .string("can", "One can: its id like redbull:cherry, or its name or flavour like Cherry Edition", false)

    override fun inPublic() = true

    override fun run(call: ToolCall): ToolResult {
      val asked = call.args().string("can")
      val texts = Names.of(language(call.asker()))
      val result = linkedMapOf<String, Any>("ingredients" to INGREDIENTS, "grid" to GRID, "names" to Names.items(texts))
      if (asked == null) {
        result["cans"] = CANS.map { short(it, texts) }
        result["elsewhere"] = ELSEWHERE
        return ToolResult.of(result).summary("${CANS.size} cans")
      }
      val found = find(asked, texts)
      if (found.isEmpty()) throw ToolException("no can matches \"$asked\"; the cans are ${CANS.joinToString { "redbull:${it.id}" }}")
      result["cans"] = found.map { full(it, texts) }
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

    /**
     * The language Allay answers the asker in, as a game language (`ru_ru`; API 1.3), or their game's on an older
     * Allay, which has no `Asker.language`.
     */
    fun language(asker: Asker): String =
      if (AllayApi.satisfies(AllayExpansions.apiVersion(), "1.3.0")) asker.language() else asker.locale()

    private fun short(can: Can, texts: Map<String, String>): Map<String, Any> {
      val entry = linkedMapOf<String, Any>("id" to "redbull:${can.id}", "name" to Names.name(can), "flavour" to Names.flavour(can, texts))
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

    private fun full(can: Can, texts: Map<String, String>): Map<String, Any> {
      val entry = linkedMapOf<String, Any>("id" to "redbull:${can.id}", "name" to Names.name(can), "flavour" to Names.flavour(can, texts))
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

    /**
     * Cans by id, name, or flavour, as a player might put it: “cherry”, “Red Bull Cherry Edition”, “Cherry-Sakura”, or
     * the flavour in [texts]' language.
     */
    fun find(asked: String, texts: Map<String, String> = emptyMap()): List<Can> {
      val words = fold(asked.removePrefix("redbull:"))
      CANS.firstOrNull { fold(it.id) == words }?.let { return listOf(it) }
      val named = CANS.filter { words == fold(Names.name(it)) || words == fold(Names.name(it).removePrefix("Red Bull ")) }
      if (named.isNotEmpty()) return named
      return CANS.filter { words == fold(Names.flavour(it)) || words == fold(Names.flavour(it, texts)) }
    }

    private fun fold(text: String) = text.lowercase().filter { it.isLetterOrDigit() }
  }
}

/**
 * Red Bull's texts from core's language files, which every platform's jar carries: the cans' names (English in every
 * language) and flavours, and the ingredients' names, in the asker's language or English.
 */
internal object Names {
  private const val ENGLISH = "en_us"
  private val files = ConcurrentHashMap<String, Map<String, String>>()

  private fun file(locale: String): Map<String, String> = files.getOrPut(locale) {
    val json = Names::class.java.getResourceAsStream("/assets/redbull/lang/$locale.json")?.use { it.readBytes().decodeToString() } ?: ""
    Regex("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(json)
      .associate { it.groupValues[1] to it.groupValues[2].replace("\\\"", "\"").replace("\\\\", "\\") }
  }

  private val english: Map<String, String> get() = file(ENGLISH)

  /** The texts in [locale] (`ru_ru`), else in another variant of its language (`es_mx` reads `es_es`), else English. */
  fun of(locale: String?): Map<String, String> {
    val code = locale?.trim()?.lowercase()?.replace('-', '_')?.takeIf { Regex("[a-z]{2,3}(_[a-z0-9]{2,4})?").matches(it) } ?: return english
    file(code).takeIf { it.isNotEmpty() }?.let { return it }
    val language = code.substringBefore('_')
    return LANGUAGES.firstOrNull { it.substringBefore('_') == language }?.let(::file)?.takeIf { it.isNotEmpty() } ?: english
  }

  /** The languages core ships. */
  private val LANGUAGES = listOf("en_us", "ru_ru", "de_de", "es_es", "pt_br")

  fun name(can: Can) = english[can.translation] ?: can.id

  fun flavour(can: Can, texts: Map<String, String> = english) = texts["${can.translation}.flavour"] ?: english["${can.translation}.flavour"] ?: can.id

  /** The ingredients' and the empty can's names in [texts]' language, by id: what the recipes name only by id. */
  fun items(texts: Map<String, String>): Map<String, String> =
    listOf(Ids.BULL_BILE, Ids.CRUDE_TAURINE, Ids.TAURINE, Ids.EMPTY_CAN).associate { id ->
      "redbull:$id" to (texts["item.redbull.$id"] ?: english["item.redbull.$id"] ?: id)
    }
}
