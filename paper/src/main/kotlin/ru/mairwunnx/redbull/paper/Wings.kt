@file:Suppress("UnstableApiUsage")

package ru.mairwunnx.redbull.paper

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
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
import ru.mairwunnx.redbull.core.WINGS_WARNING_FLIGHT_SECONDS
import ru.mairwunnx.redbull.core.WINGS_WARNING_SECONDS
import ru.mairwunnx.redbull.core.wingedCanFlightPace
import java.util.UUID
import kotlin.math.ceil

/**
 * A winged can's durability is all it has left: a point a second on the ground, and as many times faster in flight as
 * its wings are shorter than its life on the ground, so the bar always shows what's left and a half-worn can glides
 * half as long. The game takes a point a second in flight itself and stops the glide a point before the can breaks;
 * the rest goes here, once a second, into the durability alone, since a change to any other component plays the
 * swap animation in the hand. A can also crumples at its `redbull:expires`, which catches up on a time away.
 * A can about to run out warns its holder once. A spent can crumples, but never under a player still in the air on
 * it. When the glide ends in the air, the player floats down under Slow Falling instead of dropping like a stone.
 * Only players holding a winged can are looked at, so a crowd without wings costs nothing.
 */
class Wings(private val plugin: RedBullPlugin) : Listener {
  private companion object {
    val FEATHER = ItemStack.of(Material.FEATHER)
  }

  private val watched = mutableSetOf<UUID>()

  /** When each watched player's cans were last worn. */
  private val worn = mutableMapOf<UUID, Long>()

  /** Each can's wear short of a whole point, by its holder and its `redbull:expires`. */
  private val fractions = mutableMapOf<Pair<UUID, Long>, Double>()

  /** The cans that already warned, by their holder and their `redbull:expires`. */
  private val warned = mutableSetOf<Pair<UUID, Long>>()

  fun watch(player: Player) {
    watched += player.uniqueId
  }

  @EventHandler
  fun onJoin(event: PlayerJoinEvent) {
    if (event.player.inventory.contents.any { Items.id(it) == Ids.WINGED_CAN }) watch(event.player)
  }

  @EventHandler
  fun onQuit(event: PlayerQuitEvent) = forget(event.player.uniqueId)

  private fun forget(id: UUID) {
    watched -= id
    worn -= id
    fractions.keys.removeIf { it.first == id }
    warned.removeIf { it.first == id }
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
      if (player == null || !sweep(player)) forget(id)
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
    val seconds = (now - (worn.put(player.uniqueId, now) ?: now)).coerceIn(0, 2_000) / 1000.0
    var left = false
    for (slot in 0 until inventory.size) {
      val item = inventory.getItem(slot) ?: continue
      if (Items.id(item) != Ids.WINGED_CAN) continue
      val expires = item.persistentDataContainer.getOrDefault(Items.EXPIRES, PersistentDataType.LONG, 0L)
      val can = player.uniqueId to expires
      if (!player.isGliding && spent(item, expires, now)) {
        fractions -= can
        warned -= can
        inventory.setItem(slot, Items.crumpledCan(Items.flavourOf(item) ?: CANS.first()))
        player.sendActionBar(Texts.text("redbull.wings.gone").color(NamedTextColor.GRAY))
        continue
      }
      left = true
      val wings = (Items.flavourOf(item) ?: CANS.first()).wings
      val flying = player.isGliding && slot == inventory.heldItemSlot
      val pointsLeft = wear(item, can, expires, now, if (flying) seconds * (wingedCanFlightPace(wings) - 1) else seconds)
      if (pointsLeft != null) inventory.setItem(slot, item)
      val secondsLeft = (pointsLeft ?: remaining(item)) / if (flying) wingedCanFlightPace(wings) else 1.0
      if (secondsLeft <= if (flying) WINGS_WARNING_FLIGHT_SECONDS else WINGS_WARNING_SECONDS) warn(player, can)
    }
    return left
  }

  /**
   * Wears [item] by [points], whole ones now and the rest later, but never less than its `redbull:expires` asks.
   * The points left when the durability changed, or null when it didn't.
   */
  private fun wear(item: ItemStack, can: Pair<UUID, Long>, expires: Long, now: Long, points: Double): Int? {
    val max = item.getData(DataComponentTypes.MAX_DAMAGE) ?: return null
    val damage = item.getData(DataComponentTypes.DAMAGE) ?: 0
    val total = (fractions[can] ?: 0.0) + points
    val whole = total.toInt()
    fractions[can] = total - whole
    val byClock = max - 1 - ceil((expires - now) / 1000.0).toInt()
    val next = maxOf(damage + whole, byClock).coerceIn(damage, max - 1)
    if (next == damage) return null
    item.setData(DataComponentTypes.DAMAGE, next)
    return max - 1 - next
  }

  private fun remaining(item: ItemStack): Int =
    (item.getData(DataComponentTypes.MAX_DAMAGE) ?: 1) - 1 - (item.getData(DataComponentTypes.DAMAGE) ?: 0)

  /** Once a can: its wings are about to go. Only its holder hears it. */
  private fun warn(player: Player, can: Pair<UUID, Long>) {
    if (!warned.add(can)) return
    player.sendActionBar(Texts.text("redbull.wings.fading").color(NamedTextColor.YELLOW))
    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8f, 1.2f)
  }

  private fun spent(item: ItemStack, expires: Long, now: Long): Boolean {
    val max = item.getData(DataComponentTypes.MAX_DAMAGE) ?: return true
    val damage = item.getData(DataComponentTypes.DAMAGE) ?: 0
    return damage >= max - 1 || now >= expires
  }
}
