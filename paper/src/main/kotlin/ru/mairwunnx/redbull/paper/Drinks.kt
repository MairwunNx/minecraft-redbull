package ru.mairwunnx.redbull.paper

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.mairwunnx.redbull.core.Bottles
import ru.mairwunnx.redbull.core.Can
import ru.mairwunnx.redbull.core.Drink
import ru.mairwunnx.redbull.core.Ids
import ru.mairwunnx.redbull.core.Stage
import ru.mairwunnx.redbull.core.WINGS_GRACE_MILLIS
import ru.mairwunnx.redbull.core.crash
import ru.mairwunnx.redbull.core.crashDelay
import ru.mairwunnx.redbull.core.drink
import ru.mairwunnx.redbull.core.drinkCrudeTaurine
import ru.mairwunnx.redbull.core.milk
import java.util.UUID

/**
 * What a drink does beyond its potion effects, which the game applies itself: caffeine, the crash, the
 * overdose, wings, and the bottles' bull mode. Milk takes some caffeine away; death takes all of it.
 */
class Drinks(private val plugin: RedBullPlugin) : Listener {
  private val rules = Caffeines.rules
  private val overdosed = mutableSetOf<UUID>()
  private val bile = mutableMapOf<UUID, Long>()
  private val crude = mutableMapOf<UUID, Long>()

  /** The tab pops as the player raises the can. */
  @EventHandler(priority = EventPriority.MONITOR)
  fun onRaise(event: PlayerInteractEvent) {
    if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
    if (Items.canOf(event.item) == null) return
    val player = event.player
    player.world.playSound(player.location, Sound.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.PLAYERS, 0.5f, 1.9f)
    player.world.playSound(player.location, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundCategory.PLAYERS, 0.6f, 1.4f)
  }

  @EventHandler(ignoreCancelled = true)
  fun onConsume(event: PlayerItemConsumeEvent) {
    val player = event.player
    if (event.item.type == Material.MILK_BUCKET) {
      Caffeines.set(player, rules.milk(Caffeines.now(player), Caffeines.clock(player)))
      return
    }
    when (Items.id(event.item)) {
      null -> return
      Ids.BULL_BILE -> bottle(player, bile, crude)
      Ids.CRUDE_TAURINE -> {
        bottle(player, crude, bile)
        caffeine(player, rules.drinkCrudeTaurine(Caffeines.now(player), Caffeines.clock(player)))
      }
      else -> {
        val can = Items.canOf(event.item) ?: return
        val drink = rules.drink(Caffeines.now(player), Caffeines.clock(player))
        if (!caffeine(player, drink)) return
        if (drink.stage == Stage.CRASH) crashLater(player, can)
        if (plugin.settings.wings && can.wings > 0) wings(event, player, can)
      }
    }
  }

  /** Writes the new caffeine; false when it was one drink too many and the player is about to die. */
  private fun caffeine(player: Player, drink: Drink): Boolean {
    if (!drink.fatal || !plugin.settings.overdose || player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
      Caffeines.set(player, drink.caffeine.copy(level = drink.caffeine.level.coerceAtMost(rules.limit)))
      return true
    }
    Caffeines.reset(player)
    overdosed += player.uniqueId
    // After the drink finishes: dying in the middle of it would hand the player its leftovers.
    plugin.server.scheduler.runTask(plugin, Runnable { if (player.isOnline && !player.isDead) player.health = 0.0 })
    return false
  }

  /** After the buffs, the crash: a minute slower and weaker. */
  private fun crashLater(player: Player, can: Can) {
    plugin.server.scheduler.runTaskLater(plugin, Runnable {
      if (!player.isOnline || player.isDead) return@Runnable
      crash().mapNotNull(Items::potionEffect).forEach(player::addPotionEffect)
    }, crashDelay(can) * 20L)
  }

  private fun wings(event: PlayerItemConsumeEvent, player: Player, can: Can) {
    event.replacement = Items.wingedCan(can.wings, System.currentTimeMillis() + WINGS_GRACE_MILLIS)
    plugin.wings.watch(player)
    player.velocity = player.velocity.setY(0.9)
    player.world.playSound(player.location, Sound.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.8f, 1.2f)
    player.sendActionBar(Texts.text("redbull.wings").color(NamedTextColor.AQUA))
  }

  /** A bottle makes the player moo; the other one within a minute before it starts bull mode. */
  private fun bottle(player: Player, mine: MutableMap<UUID, Long>, other: MutableMap<UUID, Long>) {
    val now = System.currentTimeMillis()
    mine[player.uniqueId] = now
    player.world.playSound(player.location, Sound.ENTITY_COW_AMBIENT, SoundCategory.PLAYERS, 1f, 0.8f)
    if (!Bottles.bullMode(other[player.uniqueId], now)) return
    mine.remove(player.uniqueId)
    other.remove(player.uniqueId)
    Bottles.BULL_MODE.mapNotNull(Items::potionEffect).forEach(player::addPotionEffect)
    player.world.playSound(player.location, Sound.ENTITY_COW_HURT, SoundCategory.PLAYERS, 2f, 0.5f)
    player.sendActionBar(Texts.text("redbull.bull_mode").color(NamedTextColor.RED))
  }

  @EventHandler
  fun onDeath(event: PlayerDeathEvent) {
    val player = event.player
    Caffeines.reset(player)
    if (overdosed.remove(player.uniqueId)) {
      event.deathMessage(Texts.text("death.attack.redbull.overdose", player.displayName()))
    }
  }

  @EventHandler
  fun onQuit(event: PlayerQuitEvent) {
    val id = event.player.uniqueId
    overdosed.remove(id)
    bile.remove(id)
    crude.remove(id)
  }
}
