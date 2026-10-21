package ru.mairwunnx.redbull.fabric

import com.mojang.serialization.Codec
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

/**
 * The cans a player has ever held, in a data attachment saved with them and kept through death. A can counts the
 * moment it reaches them: picked up, crafted, drunk, given, or taken from a chest, which shows when they close it.
 * Nothing runs between those events, so a full server costs no more than an empty one. The pickup and the closed
 * container come from `PickupMixin` and `CloseContainerMixin`; Fabric has no event for either.
 */
object Collected {
  val COLLECTED: AttachmentType<List<String>> = AttachmentRegistry.create(RedBullItems.id("collected")) {
    it.persistent(Codec.STRING.listOf()).copyOnDeath()
  }

  fun init() {
    ServerPlayConnectionEvents.JOIN.register { handler, _, _ -> record(handler.player, handler.player.inventory) }
  }

  fun of(player: ServerPlayer): Set<String> = player.getAttached(COLLECTED)?.toSet() ?: emptySet()

  @JvmStatic
  fun record(player: ServerPlayer, items: Iterable<ItemStack>) =
    add(player, items.mapNotNullTo(mutableSetOf()) { RedBullItems.canOf(it)?.id })

  fun add(player: ServerPlayer, ids: Set<String>) {
    if (ids.isEmpty()) return
    val known = of(player)
    if (!known.containsAll(ids)) player.setAttached(COLLECTED, (known + ids).sorted())
  }
}
