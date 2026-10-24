package ru.mairwunnx.redbull.modcore

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

/**
 * The cans a player has ever held, in a data attachment ([Loader.collected]) kept through death. A can counts the
 * moment it reaches them: picked up, crafted, drunk, given, or taken from a chest, which shows when they close it.
 * Nothing runs between those events, so a full server costs no more than an empty one. A drink and a craft come from
 * [CanItem]; the pickup and the closed container from each loader.
 */
object Collected {
  fun of(player: ServerPlayer): Set<String> = Mod.loader.collected.get(player)?.toSet() ?: emptySet()

  @JvmStatic
  fun record(player: ServerPlayer, items: Iterable<ItemStack>) =
    add(player, items.mapNotNullTo(mutableSetOf()) { RedBullItems.canOf(it)?.id })

  fun add(player: ServerPlayer, ids: Set<String>) {
    if (ids.isEmpty()) return
    val known = of(player)
    if (known.containsAll(ids)) return
    Mod.loader.collected.set(player, (known + ids).sorted())
    Awards.collection(player, known + ids)
  }

  /** What they carry in counts, and a collection from before the advancements counts at once. */
  fun joined(player: ServerPlayer) {
    record(player, player.inventory)
    Awards.collection(player, of(player))
  }
}
