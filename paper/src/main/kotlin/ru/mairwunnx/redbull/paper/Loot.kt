package ru.mairwunnx.redbull.paper

import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Ageable
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.world.LootGenerateEvent
import ru.mairwunnx.redbull.core.Bile
import ru.mairwunnx.redbull.core.CANS
import java.util.concurrent.ThreadLocalRandom

/**
 * Where ingredients and cans come from besides crafting: bull bile from cows a player kills, and cans in
 * chests by the biome the chest stands in, whatever structure it belongs to.
 */
class Loot(private val plugin: RedBullPlugin) : Listener {
  @EventHandler(ignoreCancelled = true)
  fun onCowDeath(event: EntityDeathEvent) {
    val entity = event.entity
    if (entity.type != EntityType.COW && entity.type != EntityType.MOOSHROOM) return
    if ((entity as? Ageable)?.isAdult == false) return
    val killer = entity.killer ?: return
    val looting = killer.inventory.itemInMainHand.getEnchantmentLevel(Enchantment.LOOTING)
    if (ThreadLocalRandom.current().nextDouble() < Bile.chance(looting) * plugin.settings.loot.bile) {
      event.drops += Items.bile()
    }
  }

  @EventHandler(ignoreCancelled = true)
  fun onChest(event: LootGenerateEvent) {
    if (event.inventoryHolder == null) return
    val location = event.lootContext.location
    val biome = location.world.getBiome(location).key.asString()
    val random = ThreadLocalRandom.current()
    val cans = CANS.filter { can ->
      val chests = can.chests ?: return@filter false
      biome in chests.biomes && random.nextDouble() < chests.chance * plugin.settings.loot.chests
    }
    if (cans.isNotEmpty()) event.setLoot(event.loot + cans.map(Items::can))
  }
}
