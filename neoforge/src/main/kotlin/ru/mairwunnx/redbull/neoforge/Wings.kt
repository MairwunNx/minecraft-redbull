package ru.mairwunnx.redbull.neoforge

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import java.util.UUID

/**
 * A winged can is spent once its flight wore it down to the last point, or once its grace ran out.
 * A spent can crumples into its flavour's crumpled can, but never under a player still in the air on it.
 * Only players holding a winged can are looked at, so a crowd without wings costs nothing.
 */
object Wings {
  /** Watched players, and whether each was gliding a tick ago. */
  private val watched = mutableMapOf<UUID, Boolean>()
  private var ticks = 0

  fun watch(player: ServerPlayer) {
    watched.putIfAbsent(player.uuid, player.isFallFlying)
  }

  fun listen(bus: IEventBus) {
    bus.on<PlayerEvent.PlayerLoggedInEvent> { event ->
      val player = event.entity as? ServerPlayer ?: return@on
      if (player.inventory.any { Items.id(it) == ru.mairwunnx.redbull.core.Ids.WINGED_CAN }) watch(player)
    }
    bus.on<PlayerEvent.PlayerLoggedOutEvent> { watched -= it.entity.uuid }
    bus.on<ServerTickEvent.Post> { event -> tick(event) }
  }

  /** Every second, and the tick a watched player lands: crumple spent cans, forget players who have none left. */
  private fun tick(event: ServerTickEvent.Post) {
    if (watched.isEmpty()) return
    val second = ++ticks % 20 == 0
    for ((id, wasGliding) in watched.toMap()) {
      val player = event.server.playerList.getPlayer(id)
      if (player == null) {
        watched -= id
        continue
      }
      val gliding = player.isFallFlying
      watched[id] = gliding
      if ((second || (wasGliding && !gliding)) && !sweep(player)) watched -= id
    }
  }

  /** True while the player still holds a winged can. */
  private fun sweep(player: ServerPlayer): Boolean {
    val inventory = player.inventory
    val now = System.currentTimeMillis()
    var left = false
    for (slot in 0 until inventory.containerSize) {
      val item = inventory.getItem(slot)
      if (Items.id(item) != ru.mairwunnx.redbull.core.Ids.WINGED_CAN) continue
      if (player.isFallFlying || !spent(item, now)) {
        left = true
        continue
      }
      inventory.setItem(slot, Items.crumpled(Items.flavourOf(item)))
      player.sendOverlayMessage(Drinks.text("redbull.wings.gone", ChatFormatting.GRAY))
    }
    return left
  }

  private fun spent(item: ItemStack, now: Long): Boolean {
    val expires = item.get(Items.EXPIRES.get()) ?: 0L
    val max = item.get(DataComponents.MAX_DAMAGE) ?: return true
    val damage = item.get(DataComponents.DAMAGE) ?: 0
    return damage >= max - 1 || now >= expires
  }
}
