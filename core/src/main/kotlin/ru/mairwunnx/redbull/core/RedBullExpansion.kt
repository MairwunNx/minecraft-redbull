package ru.mairwunnx.redbull.core

import chat.allay.expansion.ItemInfo
import chat.allay.expansion.ToolException
import chat.allay.expansion.v2.Call
import chat.allay.expansion.v2.Expansion
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.floor

/**
 * What every platform tells [RedBullExpansion] about itself. A class of its own, so a platform can set it without
 * loading the expansion, whose class exists only where Allay is installed.
 */
object RedBullInfo {
  /** The plugin's or mod's version, set when it starts. */
  @Volatile var version = "dev"

  /**
   * True on Paper, set when it starts: every item is a potion underneath there, and crude taurine brews by a mix that
   * matches in code, which Allay can't read, so [RedBullExpansion.items] names the items' base and describes that brew.
   */
  @Volatile var paper = false

  /** What the running platform tells Allay's tools about a player, or null while no server runs. */
  @Volatile var players: RedBullPlayers? = null

  /**
   * One line for the log when Allay runs but is older than 6.33, which brought the expansion API 2 [RedBullExpansion]
   * speaks. Null without Allay or with a new enough one. An older Allay never reads API 2's services file, so Red Bull
   * runs as before.
   */
  fun allayTooOld(): String? {
    val loader = RedBullInfo::class.java.classLoader
    if (!present("chat.allay.expansion.AllayExpansions", loader)) return null
    return if (present("chat.allay.expansion.v2.Expansion", loader)) null else "Allay is too old for Red Bull's tools and items: it needs Allay 6.33 or newer"
  }

  private fun present(name: String, loader: ClassLoader?) = try {
    Class.forName(name, false, loader)
    true
  } catch (_: ClassNotFoundException) {
    false
  } catch (_: LinkageError) {
    false
  }
}

/**
 * A player as Red Bull's Allay tools read them, from the platform that keeps them. Allay calls the tools on the server
 * thread, so every method runs there and answers null for a player who isn't online.
 */
interface RedBullPlayers {
  /** Their caffeine now, with what wore off since it was last written. */
  fun caffeine(player: UUID): Caffeine?

  /** The ids of the cans they ever held (`cherry`). */
  fun collection(player: UUID): Set<String>?

  /** Their numbers for all time where the platform counts them (Paper's `stats.yml`), else null. */
  fun allTime(player: UUID): AllTime? = null
}

/** One player's numbers for all time: cans drunk, caffeine in milligrams, overdose deaths, flight, and favourite can. */
@JvmRecord data class AllTime(val cans: Long, val caffeineMg: Long, val overdoses: Long, val flightSeconds: Long, val favourite: String?)

/**
 * Red Bull for Allay, the server's AI assistant (`chat.allay:allay-expansion`, API 2): `x_redbull_recipes` tells the
 * model how each can is made and where it's found, `x_redbull_caffeine` the asker's caffeine and what it brings, and
 * `x_redbull_collection` the cans they hold and miss. Its items describe what Allay can't read from the game, and a few
 * lines of prompt send the model to the tools. Allay
 * finds it through `META-INF/services/chat.allay.expansion.v2.Expansion` on Paper, Fabric, and NeoForge alike. Without
 * Allay nothing loads it, and Red Bull runs as before.
 */
object RedBullExpansion : Expansion("redbull", "Red Bull") {
  val can = string("can", "One can: its id like redbull:cherry, or its name or flavour like Cherry Edition")

  /** `x_redbull_recipes`: the ingredients' way, and one can or every can. */
  val recipes = tool("recipes", RECIPES, ::recipes).args(can).inPublic().summary { it.summary() }

  /** `x_redbull_caffeine`: the asker's caffeine, what more cans would do, and what's coming. About the asker alone. */
  val caffeine = tool("caffeine", CAFFEINE, ::caffeine).summary { "caffeine ${it.level}" }

  /** `x_redbull_collection`: the cans the asker holds and those they miss, with where to get them. */
  val collection = tool("collection", COLLECTION, ::collection).summary { "${it.held} of ${it.total} cans" }

  override fun version(): String = RedBullInfo.version

  override fun items(): List<ItemInfo> = RedBullItemInfo.all(RedBullInfo.paper)

  override fun prompt() =
    "Red Bull adds 26 energy drink cans (redbull:original and so on), each a potion with buffs and most with wings to " +
      "glide on. The way to a can: bull bile from cows a player kills, brewed into a water bottle as crude taurine, " +
      "smelted into taurine, then crafted with an empty can, a flavour, a dye, sugar or honeycomb, and a water bottle. " +
      "Four cans are legends found only in chests. Every can adds ${RULES.perCan.toInt()} caffeine, which wears off by " +
      "a point every 16 seconds of play, so not while the player is offline. Above ${RULES.limit.toInt()} the heart " +
      "gives out, from ${RULES.sleepless.toInt()} no bed lets the player sleep, and milk takes ${RULES.milk.toInt()}. " +
      "A can with sugar that brings caffeine to ${RULES.crash.toInt()} or more ends in a crash once its buffs do: " +
      "Slowness and Weakness for a minute. Once the caffeine is gone and the buffs of the last can with sugar have ended, " +
      "the sugar pit comes: Hunger and Slowness, deeper for every can with sugar since the caffeine was last gone. " +
      "Sugarfree cans bring neither. Wings let a player glide as on an elytra and wear out, faster in the air. Wings that " +
      "end mid-glide leave $SOFT_LANDING_SECONDS seconds of Slow Falling. Bull bile and crude taurine drunk within a " +
      "minute of each other start bull mode: Strength II and Nausea. /redbull opens the player's collection, every can " +
      "they ever held and question marks for the rest. Before you explain how to make or find a can, call " +
      "x_redbull_recipes: never guess a recipe. Call x_redbull_caffeine for the asker's caffeine, how many more cans are " +
      "safe, and when a crash or the sugar pit comes, and x_redbull_collection for the cans they still miss. Call Red " +
      "Bull's items by the names it gives in `names`, as the game shows them to the player, and never translate an id."

  /** What `x_redbull_recipes` answers: every can in short, or the cans asked for in full. */
  sealed interface Recipes {
    /** The line under the tool in the asker's chat. */
    fun summary(): String
  }

  /** Every can in short, and the other ways to get one. */
  @JvmRecord data class EveryCan(val ingredients: Map<String, Any>, val grid: Map<String, Any>, val names: Map<String, String>, val cans: List<Map<String, Any>>, val elsewhere: List<String>) : Recipes {
    override fun summary() = "${cans.size} cans"
  }

  /** The cans asked for, in full. */
  @JvmRecord data class Found(val ingredients: Map<String, Any>, val grid: Map<String, Any>, val names: Map<String, String>, val cans: List<Map<String, Any>>) : Recipes {
    override fun summary() = cans.joinToString { it["name"].toString() }
  }

  /** One effect of a can. */
  @JvmRecord data class CanEffect(val effect: String, val level: Int, val seconds: Int)

  /** The biomes whose chests may hold a can, and the chance of each chest. */
  @JvmRecord data class ChestOdds(val biomes: List<String>, val chance: Double)

  private fun recipes(call: Call): Recipes {
    val asked = call[can]
    val texts = Names.of(call.asker().language())
    val names = Names.items(texts)
    if (asked == null) return EveryCan(INGREDIENTS, GRID, names, CANS.map { short(it, texts) }, ELSEWHERE)
    val found = find(asked, texts)
    if (found.isEmpty()) throw ToolException("no can matches \"$asked\"; the cans are ${CANS.joinToString { "redbull:${it.id}" }}")
    return Found(INGREDIENTS, GRID, names, found.map { full(it, texts) })
  }

  /** The asker's caffeine now and what it brings. */
  @JvmRecord data class CaffeineNow(val level: Int, val limit: Int, val stage: Stage, val roomLeft: Int, val safeCans: Int, val secondsLeft: Int, val sleepless: Boolean, val nextCanCrashes: Boolean, val crashInSeconds: Int?, val sugarPit: SugarPitDue?)

  /** The sugar pit the asker has coming: how deep, when, and how long its Hunger and Slowness last. */
  @JvmRecord data class SugarPitDue(val depth: Int, val inSeconds: Int, val hungerSeconds: Int, val slownessSeconds: Int)

  /** The asker's collection: the cans they ever held, those they miss, and their numbers where the platform counts them. */
  @JvmRecord data class CollectionNow(val held: Int, val total: Int, val cans: List<String>, val missing: List<MissingCan>, val allTime: AllTime?)

  /** A can the asker never held, and where to get it: crafted, from chests, or both. */
  @JvmRecord data class MissingCan(val id: String, val name: String, val flavour: String, val crafted: Boolean, val chests: ChestOdds?)

  private fun caffeine(call: Call): CaffeineNow {
    val now = asker().caffeine(call.asker().uuid()) ?: throw ToolException(NOT_IN_GAME)
    return caffeineNow(now, Crashes.next(call.asker().uuid()))
  }

  /** What [now] means for the player, with their next crash in [crashIn] seconds. */
  fun caffeineNow(now: Caffeine, crashIn: Int?): CaffeineNow {
    val room = RULES.limit - now.level
    val next = now.copy(level = now.level + RULES.perCan).stage(RULES)
    return CaffeineNow(ceil(now.level).toInt(), RULES.limit.toInt(), now.stage(RULES), floor(room).toInt().coerceAtLeast(0), floor(room / RULES.perCan).toInt().coerceAtLeast(0), now.secondsLeft(RULES), now.sleepless(RULES), next == Stage.CRASH, crashIn, pitOf(now))
  }

  private fun pitOf(now: Caffeine): SugarPitDue? {
    if (now.sugar <= 0) return null
    val buffs = ceil((now.sugarUntil - now.at).coerceAtLeast(0) / 20.0).toInt()
    val effects = sugarPit(now.sugar)
    return SugarPitDue(now.sugar, maxOf(now.secondsLeft(RULES), buffs), effects[0].seconds, effects[1].seconds)
  }

  private fun collection(call: Call): CollectionNow {
    val players = asker()
    val id = call.asker().uuid()
    val held = players.collection(id) ?: throw ToolException(NOT_IN_GAME)
    return collectionOf(held, players.allTime(id), Names.of(call.asker().language()))
  }

  /** The collection of a player who held [held], in [texts]' language. */
  fun collectionOf(held: Set<String>, allTime: AllTime?, texts: Map<String, String>): CollectionNow {
    val (has, misses) = CANS.partition { it.id in held }
    val missing = misses.map { MissingCan("redbull:${it.id}", Names.name(it), Names.flavour(it, texts), it.recipe != null, it.chests?.let(::chests)) }
    return CollectionNow(has.size, CANS.size, has.map { "redbull:${it.id}" }, missing, allTime)
  }

  private fun asker(): RedBullPlayers = RedBullInfo.players ?: throw ToolException("Red Bull isn't running on this server now")

  private const val NOT_IN_GAME = "the asker isn't a player in the world"

  private const val CAFFEINE =
    "The asker's Red Bull caffeine now. Returns `level` and the `limit` above which the heart gives out, `stage` " +
      "(calm, jitters, crash), `room_left` before the limit, `safe_cans`: how many more cans they can drink without " +
      "dying, `seconds_left` of play until the caffeine wears off, `sleepless` (too wired for a bed), " +
      "`next_can_crashes`: whether one more can with sugar brings the crash, `crash_in_seconds` for a crash already " +
      "coming (null without one), and `sugar_pit` (null without one): its depth, `in_seconds` of play until it " +
      "comes, and how long its Hunger and Slowness last. Call it before advising another can."

  private const val COLLECTION =
    "The asker's Red Bull collection, which /redbull shows: `held` of `total` cans, the ids of the `cans` they ever " +
      "held, and the `missing` ones, each with its name, flavour in the player's language, whether it's `crafted`, " +
      "and the biomes and chance of its `chests` (null for a can only crafted). A wandering trader also sells crafted " +
      "cans. `all_time`, where the server counts it: cans drunk, caffeine in mg, overdose deaths, seconds of flight, " +
      "and their favourite can, or null elsewhere. For a missing can's recipe, call x_redbull_recipes with it."

  private const val RECIPES =
    "How Red Bull cans and their ingredients are made and found on this server. Returns `ingredients` (bull bile, " +
      "crude taurine, taurine, the empty can: drops, brewing, smelting, crafting), `grid` (the crafting shape every " +
      "can shares), `names` (those items' names in the player's language), and `cans`: each can's id, name (English " +
      "on every can), flavour in the player's language, and its own dye, flavour item, and sweetener, " +
      "or for a chest-only legend the biomes and chance of its chests. Pass `can` for one can in full, with its " +
      "buffs, wings, chests, and trader price; leave it out for every can in short."

  private val RULES = Rules()

  /** How long a furnace takes to evaporate crude taurine, as every platform's recipe says. */
  const val EVAPORATE_SECONDS = 10

  private val INGREDIENTS = linkedMapOf<String, Any>(
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

  private val GRID = linkedMapOf<String, Any>(
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
    entry["effects"] = can.effects.map { CanEffect(bare(it.id), it.amplifier + 1, it.seconds) }
    entry["wings_seconds"] = can.wings
    entry["sugarfree"] = can.sugarfree
    can.chests?.let { entry["chests"] = chests(it) }
    return entry
  }

  internal fun chests(chests: Chests) = ChestOdds(chests.biomes.map(::bare), chests.chance)

  internal fun bare(id: String) = id.removePrefix("minecraft:")

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

  /** The variants the mods copy from a language core ships, as Allay's players may run them. */
  private val COPIES = mapOf("es_mx" to "es_es", "pt_pt" to "pt_br")

  /** [key]'s text in every language core ships, and in the mods' copies. */
  fun translations(key: String): Map<String, String> {
    val texts = LANGUAGES.mapNotNull { locale -> file(locale)[key]?.let { locale to it } }.toMap()
    return texts + COPIES.mapNotNull { (copy, from) -> texts[from]?.let { copy to it } }
  }

  fun name(can: Can) = english[can.translation] ?: can.id

  fun flavour(can: Can, texts: Map<String, String> = english) = texts["${can.translation}.flavour"] ?: english["${can.translation}.flavour"] ?: can.id

  /** The ingredients' and the empty can's names in [texts]' language, by id: what the recipes name only by id. */
  fun items(texts: Map<String, String>): Map<String, String> =
    listOf(Ids.BULL_BILE, Ids.CRUDE_TAURINE, Ids.TAURINE, Ids.EMPTY_CAN).associate { id ->
      "redbull:$id" to (texts["item.redbull.$id"] ?: english["item.redbull.$id"] ?: id)
    }
}
