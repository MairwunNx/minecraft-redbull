@file:Suppress("UnstableApiUsage")

package ru.mairwunnx.redbull.paper

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.mairwunnx.redbull.core.Advancements
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Ids
import ru.mairwunnx.redbull.core.SOFT_LANDING_SECONDS
import java.util.UUID

/**
 * A winged can is spent once its flight wore it down to the last point, or once its grace ran out.
 * A spent can crumples, but never under a player still in the air on it. When the glide ends in the air, the
 * player floats down under Slow Falling instead of dropping like a stone.
 * Only players holding a winged can are looked at, so a crowd without wings costs nothing.
 */
class Wings(private val plugin: RedBullPlugin) : Listener {
  private companion object {
    val FEATHER = ItemStack.of(Material.FEATHER)
  }

  private val watched = mutableSetOf<UUID>()

  fun watch(player: Player) {
    watched += player.uniqueId
  }

  @EventHandler
  fun onJoin(event: PlayerJoinEvent) {
    if (event.player.inventory.contents.any { Items.id(it) == Ids.WINGED_CAN }) watch(event.player)
  }

  @EventHandler
  fun onQuit(event: PlayerQuitEvent) {
    watched -= event.player.uniqueId
  }

  @EventHandler
  fun onLand(event: EntityToggleGlideEvent) {
    if (event.isGliding) {
      (event.entity as? Player)?.takeIf { it.uniqueId in watched }?.let { Awards.grant(it, Advancements.WINGS) }
      return
    }
    val player = event.entity as? Player ?: return
    if (player.uniqueId in watched && !player.isOnGround && !player.isInWater) {
      player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, SOFT_LANDING_SECONDS * 20, 0))
    }
    // The game stops the glide before it writes the item; look a tick later.
    plugin.server.scheduler.runTask(plugin, Runnable { if (player.isOnline) sweep(player) })
  }

  /** Every second: crumple spent winged cans, and stop watching players who have none left. */
  fun tick() {
    watched.toList().forEach { id ->
      val player = plugin.server.getPlayer(id)
      if (player == null || !sweep(player)) watched -= id
    }
  }

  /** Every other tick: feather bits trail behind whoever glides on a winged can. */
  fun feathers() {
    for (id in watched) {
      val player = plugin.server.getPlayer(id) ?: continue
      if (!player.isGliding) continue
      val behind = player.location.subtract(player.location.direction.multiply(0.8)).add(0.0, 0.3, 0.0)
      player.world.spawnParticle(Particle.ITEM, behind, 3, 0.2, 0.1, 0.2, 0.02, FEATHER)
    }
  }

  /** True while the player still holds a winged can. */
  private fun sweep(player: Player): Boolean {
    val inventory = player.inventory
    val now = System.currentTimeMillis()
    var left = false
    for (slot in 0 until inventory.size) {
      val item = inventory.getItem(slot) ?: continue
      if (Items.id(item) != Ids.WINGED_CAN) continue
      if (player.isGliding || !spent(item, now)) {
        left = true
        continue
      }
      inventory.setItem(slot, Items.crumpledCan(Items.flavourOf(item) ?: CANS.first()))
      player.sendActionBar(Texts.text("redbull.wings.gone").color(NamedTextColor.GRAY))
    }
    return left
  }

  private fun spent(item: ItemStack, now: Long): Boolean {
    val expires = item.persistentDataContainer.getOrDefault(Items.EXPIRES, PersistentDataType.LONG, 0L)
    val max = item.getData(DataComponentTypes.MAX_DAMAGE) ?: return true
    val damage = item.getData(DataComponentTypes.DAMAGE) ?: 0
    return damage >= max - 1 || now >= expires
  }
}
