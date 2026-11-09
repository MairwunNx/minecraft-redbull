package ru.mairwunnx.redbull.paper

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import ru.mairwunnx.redbull.core.Advancements

/** Red Bull's advancements: the data pack inside the jar holds them, and code grants each when it happens. */
object Awards {
  fun grant(player: Player, id: String) {
    // A child shows only once its parent is done; the first can grants the root anyway.
    listOf(Advancements.ROOT, id).distinct().forEach { name ->
      val advancement = Bukkit.getAdvancement(NamespacedKey("redbull", name)) ?: return@forEach
      val progress = player.getAdvancementProgress(advancement)
      if (!progress.isDone) progress.remainingCriteria.forEach(progress::awardCriteria)
    }
  }

  /** The collector's criterion for every can in [collection], and its last once they're all in: the game counts them. */
  fun collector(player: Player, collection: Set<String>) {
    val advancement = Bukkit.getAdvancement(NamespacedKey("redbull", Advancements.COLLECTOR)) ?: return
    val progress = player.getAdvancementProgress(advancement)
    if (progress.isDone) return
    val remaining = progress.remainingCriteria
    Advancements.collectorCriteria(collection).filter { it in remaining }.forEach(progress::awardCriteria)
  }
}
