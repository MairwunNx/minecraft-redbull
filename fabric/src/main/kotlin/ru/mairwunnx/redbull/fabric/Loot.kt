package ru.mairwunnx.redbull.fabric

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.minecraft.core.HolderLookup
import net.minecraft.resources.RegistryOps
import net.minecraft.world.level.storage.loot.LootPool
import ru.mairwunnx.redbull.core.Bile
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Can
import ru.mairwunnx.redbull.core.Ids

/**
 * Where ingredients and cans come from besides crafting: bull bile from adult cows and mooshrooms a player kills,
 * and cans in chests by the biome the chest stands in, whatever structure it belongs to. The pools are the game's
 * own loot JSON, built from core's numbers and read through the game's codec, so they behave like vanilla loot.
 */
object Loot {
  private val COWS = setOf("minecraft:entities/cow", "minecraft:entities/mooshroom")

  fun init() {
    LootTableEvents.MODIFY.register { key, table, source, registries ->
      if (!source.isBuiltin) return@register
      val id = key.identifier().toString()
      when {
        id in COWS -> if (Settings.bile > 0) table.pool(pool(registries, bile(Settings.bile)))
        key.identifier().namespace == "minecraft" && key.identifier().path.startsWith("chests/") ->
          if (Settings.chests > 0) CANS.mapNotNull { chest(it, Settings.chests) }.forEach { table.pool(pool(registries, it)) }
      }
    }
  }

  /**
   * 5% from an adult killed by a player, plus 2% per Looting level, as `random_chance_with_enchanted_bonus` counts
   * it; `loot.bile` in the settings multiplies every part.
   */
  private fun bile(times: Double) = item("redbull:${Ids.BULL_BILE}", allOf(
    obj("type" to "minecraft:killed_by_player"),
    obj(
      "type" to "minecraft:entity_properties",
      "entity" to "this",
      "predicate" to obj("minecraft:flags" to obj("is_baby" to false)),
    ),
    obj(
      "type" to "minecraft:random_chance_with_enchanted_bonus",
      "enchantment" to "minecraft:looting",
      "unenchanted_chance" to Bile.chance(0) * times,
      "enchanted_chance" to obj(
        "type" to "minecraft:linear",
        "base" to Bile.chance(1) * times,
        "per_level_above_first" to Bile.PER_LOOTING * times,
      ),
    ),
  ))

  /** A can in a chest of one of its biomes, at its chance times `loot.chests`. The chest's position is the loot's origin. */
  private fun chest(can: Can, times: Double): JsonObject? {
    val chests = can.chests ?: return null
    return item("redbull:${can.id}", allOf(
      obj("type" to "minecraft:location_check", "predicate" to obj("biomes" to array(chests.biomes))),
      obj("type" to "minecraft:random_chance", "chance" to chests.chance * times),
    ))
  }

  private fun item(name: String, condition: JsonObject) = obj(
    "rolls" to 1,
    "condition" to condition,
    "entries" to array(listOf(obj("type" to "minecraft:item", "name" to name))),
  )

  private fun allOf(vararg terms: JsonObject) = obj("type" to "minecraft:all_of", "terms" to array(terms.toList()))

  private fun pool(registries: HolderLookup.Provider, json: JsonObject): LootPool =
    LootPool.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), json).getOrThrow()

  private fun obj(vararg entries: Pair<String, Any>) = JsonObject().apply {
    entries.forEach { (key, value) -> add(key, element(value)) }
  }

  private fun array(values: List<Any>) = JsonArray().apply { values.forEach { add(element(it)) } }

  private fun element(value: Any): JsonElement = when (value) {
    is JsonElement -> value
    is String -> com.google.gson.JsonPrimitive(value)
    is Number -> com.google.gson.JsonPrimitive(value)
    is Boolean -> com.google.gson.JsonPrimitive(value)
    else -> error("No JSON for $value")
  }
}
