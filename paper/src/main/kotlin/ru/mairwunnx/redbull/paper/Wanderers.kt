package ru.mairwunnx.redbull.paper

import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.WanderingTrader
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import ru.mairwunnx.redbull.core.Fishing
import ru.mairwunnx.redbull.core.Ids
import ru.mairwunnx.redbull.core.Trades
import ru.mairwunnx.redbull.core.can
import kotlin.random.Random

/**
 * Cans out in the world besides chests: a wandering trader brings one or two, and a fishing rod's junk is now and
 * then an empty or crumpled can.
 */
class Wanderers : Listener {
  @EventHandler(ignoreCancelled = true)
  fun onTrader(event: EntitySpawnEvent) {
    val trader = event.entity as? WanderingTrader ?: return
    val offers = Trades.pick(Random.Default).map { can ->
      MerchantRecipe(Items.can(can), 0, Trades.MAX_USES, true, 1, 0.05f).apply {
        addIngredient(ItemStack.of(Material.EMERALD, Trades.price(can)))
      }
    }
    trader.recipes = trader.recipes + offers
  }

  @EventHandler(ignoreCancelled = true)
  fun onFish(event: PlayerFishEvent) {
    if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
    val caught = event.caught as? Item ?: return
    val stack = caught.itemStack
    // Only what the game's junk table gives: a plain stack, not a treasure's enchanted rod or a fish.
    if (stack.type !in JUNK || stack.enchantments.isNotEmpty() || Items.id(stack) != null) return
    if (Random.nextDouble() >= Fishing.share) return
    val id = Fishing.pick(Random.Default)
    caught.itemStack = if (id == Ids.EMPTY_CAN) Items.emptyCan() else Items.crumpledCan(can(id.removePrefix("crumpled_"))!!)
  }

  private companion object {
    val JUNK = setOf(
      Material.LILY_PAD, Material.LEATHER_BOOTS, Material.LEATHER, Material.BONE, Material.POTION, Material.STRING,
      Material.FISHING_ROD, Material.BOWL, Material.STICK, Material.INK_SAC, Material.TRIPWIRE_HOOK,
      Material.ROTTEN_FLESH, Material.BAMBOO,
    )
  }
}
