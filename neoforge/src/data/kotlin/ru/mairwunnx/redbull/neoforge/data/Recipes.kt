package ru.mairwunnx.redbull.neoforge.data

import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.CRUMPLED_NUGGETS
import ru.mairwunnx.redbull.core.Ids
import java.io.File

/**
 * Writes the recipes from `core` as the game's recipe JSON, so the cans' recipes have one source: `Cans.kt`.
 * The way to a can: bile and a water bottle brew into crude taurine, a furnace evaporates it into taurine, and
 * a crafting table puts taurine, a flavour, and a dye into an empty can. A drunk can crumples and melts back
 * into iron nuggets. The build runs it into `build/generated/data`; nothing it writes is checked in.
 */
fun main(args: Array<String>) {
  val out = File(args.single())
  out.deleteRecursively()
  fun write(path: String, value: Any) = File(out, path).apply { parentFile.mkdirs() }.writeText(json(value) + "\n")

  write("data/redbull/recipe/${Ids.EMPTY_CAN}.json", shaped(
    listOf("I I", "I I", " C "),
    mapOf('I' to "minecraft:iron_nugget", 'C' to "minecraft:copper_nugget"),
    item(Ids.EMPTY_CAN, count = 2),
  ))
  for (can in CANS) {
    val recipe = can.recipe ?: continue
    val rows = listOf("TDT", "FCF", "SWS")
    val key = mutableMapOf<Char, Any>(
      'T' to Ids.TAURINE_ITEM,
      'D' to recipe.dye,
      'F' to recipe.flavour,
      'C' to "redbull:${Ids.EMPTY_CAN}",
      'W' to WATER_BOTTLE,
    )
    key['S'] = can.sweetener
    write("data/redbull/recipe/${can.id}.json", shaped(rows, key, item(can.id)))
  }

  write("data/redbull/recipe/${Ids.CRUDE_TAURINE}.json", linkedMapOf(
    "type" to "minecraft:brewing",
    "input" to linkedMapOf("item" to "minecraft:potion", "potion_contents" to linkedMapOf("potions" to "minecraft:water")),
    "reagent" to linkedMapOf("item" to "redbull:${Ids.BULL_BILE}"),
    "output" to item(Ids.CRUDE_TAURINE),
  ))
  write("data/redbull/recipe/${Ids.TAURINE}.json", cooking("smelting", "redbull:${Ids.CRUDE_TAURINE}", item(Ids.TAURINE), 0.35, 200))

  // Crumpled cans of every flavour melt back into iron, in a furnace or twice as fast in a blast furnace.
  write("data/redbull/tags/item/crumpled_cans.json", linkedMapOf("values" to CANS.map { "redbull:${it.crumpled}" }))
  val nuggets = linkedMapOf("id" to "minecraft:iron_nugget", "count" to CRUMPLED_NUGGETS)
  write("data/redbull/recipe/recycle.json", cooking("smelting", "#redbull:crumpled_cans", nuggets, 0.1, 200))
  write("data/redbull/recipe/recycle_blasting.json", cooking("blasting", "#redbull:crumpled_cans", nuggets, 0.1, 100))
}

/** A water bottle and nothing else: a potion whose contents are plain water. */
private val WATER_BOTTLE = linkedMapOf(
  "neoforge:ingredient_type" to "neoforge:components",
  "items" to "minecraft:potion",
  "components" to linkedMapOf("minecraft:potion_contents" to linkedMapOf("potion" to "minecraft:water")),
)

private fun item(id: String, count: Int = 1) =
  linkedMapOf<String, Any>("id" to "redbull:$id").apply { if (count != 1) put("count", count) }

private fun shaped(pattern: List<String>, key: Map<Char, Any>, result: Any) = linkedMapOf(
  "type" to "minecraft:crafting_shaped",
  "category" to "misc",
  "group" to "redbull",
  "key" to key.entries.sortedBy { it.key }.associateTo(linkedMapOf()) { it.key.toString() to it.value },
  "pattern" to pattern,
  "result" to result,
)

private fun cooking(type: String, ingredient: String, result: Any, experience: Double, ticks: Int) = linkedMapOf(
  "type" to "minecraft:$type",
  "category" to "misc",
  "cookingtime" to ticks,
  "experience" to experience,
  "ingredient" to ingredient,
  "result" to result,
)

/** Just enough JSON for maps, lists, strings, and numbers, indented as the game's own data is. */
private fun json(value: Any?, indent: String = ""): String = when (value) {
  null -> "null"
  is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
  is Number, is Boolean -> value.toString()
  is Map<*, *> -> if (value.isEmpty()) "{}" else value.entries.joinToString(",\n", "{\n", "\n$indent}") {
    "$indent  ${json(it.key.toString())}: ${json(it.value, "$indent  ")}"
  }
  is List<*> -> if (value.isEmpty()) "[]" else value.joinToString(",\n", "[\n", "\n$indent]") { "$indent  ${json(it, "$indent  ")}" }
  else -> error("Can't write ${value::class} as JSON")
}
