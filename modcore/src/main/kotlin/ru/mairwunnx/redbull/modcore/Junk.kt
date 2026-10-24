package ru.mairwunnx.redbull.modcore

import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer
import net.minecraft.world.level.storage.loot.entries.NestedLootTable
import ru.mairwunnx.redbull.core.Fishing

/**
 * Cans among a fishing rod's junk, the same on both loaders. The junk rolls inside `gameplay/fishing`, where no loot
 * modifier sees it, so the junk table's own pool takes [entries] as it loads.
 */
object Junk {
  const val TABLE = "minecraft:gameplay/fishing/junk"

  /**
   * Beside the game's junk: an empty can, and a crumpled can of any flavour, the flavours sharing one weight, so a can
   * turns up as often as core says however many flavours there are.
   */
  fun entries(): List<LootPoolEntryContainer> {
    val crumpled = LootPool.lootPool()
    RedBullItems.crumpled.values.forEach { crumpled.add(LootItem.lootTableItem(it)) }
    return listOf(
      LootItem.lootTableItem(RedBullItems.emptyCan).setWeight(Fishing.EMPTY_WEIGHT).build(),
      NestedLootTable.inlineLootTable(LootTable.lootTable().withPool(crumpled).build()).setWeight(Fishing.CRUMPLED_WEIGHT).build(),
    )
  }
}
