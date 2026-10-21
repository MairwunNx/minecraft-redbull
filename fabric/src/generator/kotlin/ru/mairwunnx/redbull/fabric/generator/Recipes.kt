package ru.mairwunnx.redbull.fabric.generator

import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.CRUMPLED_NUGGETS
import ru.mairwunnx.redbull.core.Ids
import java.io.File

/**
 * Writes the mod's recipes as the game's data from core, the one place cans and their recipes live: a crafting
 * recipe for each can with one and for the empty can, the furnace that evaporates crude taurine, the furnaces that
 * melt crumpled cans, and the brewing of bull bile into a water bottle. Gradle runs it before every build.
 */
fun main(args: Array<String>) {
  val data = File(args.single(), "data/redbull")
  data.deleteRecursively()
  val write = { path: String, content: Any -> File(data, path).apply { parentFile.mkdirs() }.writeText(json(content) + "\n") }
  val redbull = { id: String -> "redbull:$id" }

  write("recipe/${Ids.EMPTY_CAN}.json", shaped(
    listOf("I I", "I I", " C "), mapOf("I" to "minecraft:iron_nugget", "C" to "minecraft:copper_nugget"),
    redbull(Ids.EMPTY_CAN), count = 2,
  ))
  // A plain water bottle, as Paper's exact choice takes it: a potion of water and nothing else.
  val water = mapOf(
    "fabric:type" to "fabric:components",
    "base" to "minecraft:potion",
    "components" to mapOf("minecraft:potion_contents" to mapOf("potion" to "minecraft:water")),
  )
  for (can in CANS) {
    val recipe = can.recipe ?: continue
    val rows = listOf("TDT", "FCF", "SWS")
    val key = mutableMapOf<String, Any>(
      "T" to redbull(Ids.TAURINE), "D" to recipe.dye, "F" to recipe.flavour, "C" to redbull(Ids.EMPTY_CAN), "W" to water,
    )
    key["S"] = can.sweetener
    write("recipe/${can.id}.json", shaped(rows, key, redbull(can.id)))
  }

  write("recipe/${Ids.TAURINE}.json", cooking("smelting", redbull(Ids.CRUDE_TAURINE), redbull(Ids.TAURINE), 1, 0.35, 200))
  // Crumpled cans of every flavour melt back into iron, in a furnace or twice as fast in a blast furnace.
  write("tags/item/crumpled_cans.json", mapOf("values" to CANS.map { redbull(it.crumpled) }))
  write("recipe/recycle.json", cooking("smelting", "#redbull:crumpled_cans", "minecraft:iron_nugget", CRUMPLED_NUGGETS, 0.1, 200))
  write("recipe/recycle_blasting.json", cooking("blasting", "#redbull:crumpled_cans", "minecraft:iron_nugget", CRUMPLED_NUGGETS, 0.1, 100))

  write("recipe/${Ids.CRUDE_TAURINE}.json", mapOf(
    "type" to "minecraft:brewing",
    "input" to mapOf("item" to "minecraft:potion", "potion_contents" to mapOf("potions" to "minecraft:water")),
    "reagent" to mapOf("item" to redbull(Ids.BULL_BILE)),
    "output" to mapOf("id" to redbull(Ids.CRUDE_TAURINE)),
  ))
}

private fun shaped(pattern: List<String>, key: Map<String, Any>, result: String, count: Int = 1) = mapOf(
  "type" to "minecraft:crafting_shaped",
  "category" to "misc",
  "group" to "redbull",
  "pattern" to pattern,
  "key" to key,
  "result" to result(result, count),
)

private fun cooking(type: String, ingredient: String, result: String, count: Int, experience: Double, ticks: Int) = mapOf(
  "type" to "minecraft:$type",
  "category" to "misc",
  "ingredient" to ingredient,
  "result" to result(result, count),
  "experience" to experience,
  "cookingtime" to ticks,
)

private fun result(id: String, count: Int) = if (count == 1) mapOf("id" to id) else mapOf("id" to id, "count" to count)

/** Just enough JSON for maps, lists, strings, and numbers, indented by two spaces as the game's own files are. */
private fun json(value: Any?, indent: String = ""): String = when (value) {
  null -> "null"
  is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
  is Number, is Boolean -> value.toString()
  is Map<*, *> -> if (value.isEmpty()) "{}" else value.entries.joinToString(",\n", "{\n", "\n$indent}") { (key, item) ->
    "$indent  ${json(key.toString())}: ${json(item, "$indent  ")}"
  }
  is List<*> -> if (value.isEmpty()) "[]" else value.joinToString(",\n", "[\n", "\n$indent]") { "$indent  ${json(it, "$indent  ")}" }
  else -> error("No JSON for $value")
}
