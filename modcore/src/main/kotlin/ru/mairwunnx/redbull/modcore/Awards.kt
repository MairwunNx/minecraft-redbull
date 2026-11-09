package ru.mairwunnx.redbull.modcore

import net.minecraft.server.level.ServerPlayer
import ru.mairwunnx.redbull.core.Advancements
import ru.mairwunnx.redbull.core.CANS

/**
 * Red Bull's advancements, from core's data: each waits for its `done` criterion, which the game never meets by
 * itself, so these calls award it when the moment comes. The collector counts one criterion per can as well. The root comes along with any of them, since a child shows
 * only once its parent is done.
 */
object Awards {
  private const val CRITERION = "done"

  fun award(player: ServerPlayer, name: String) {
    val advancements = player.level().server.advancements
    for (path in setOf(Advancements.ROOT, name)) {
      advancements.get(id(path))?.let { player.advancements.award(it, CRITERION) }
    }
  }

  /** A collection's milestones: the first can, a legend, and every can. */
  fun collection(player: ServerPlayer, cans: Set<String>) {
    if (cans.isEmpty()) return
    award(player, Advancements.ROOT)
    if (CANS.any { it.id in cans && Advancements.legend(it) }) award(player, Advancements.LEGEND)
    collector(player, cans)
  }

  /** The collector's criterion for every can in [cans], and its last once they're all in: the game counts them. */
  private fun collector(player: ServerPlayer, cans: Set<String>) {
    val advancement = player.level().server.advancements.get(id(Advancements.COLLECTOR)) ?: return
    if (player.advancements.getOrStartProgress(advancement).isDone) return
    Advancements.collectorCriteria(cans).forEach { player.advancements.award(advancement, it) }
  }

  /** After a drink that didn't kill: this much caffeine is the edge. */
  fun caffeine(player: ServerPlayer, level: Double) {
    if (player.isAlive && level >= Advancements.EDGE_LEVEL) award(player, Advancements.EDGE)
  }
}
