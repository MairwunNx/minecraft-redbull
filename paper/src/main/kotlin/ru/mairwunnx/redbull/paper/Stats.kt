package ru.mairwunnx.redbull.paper

import org.bukkit.entity.Player
import ru.mairwunnx.redbull.core.Can
import ru.mairwunnx.redbull.core.Rules
import kotlin.math.roundToLong

/** Where drinks, overdoses, and flights are counted: the counts bStats drains every 30 minutes. */
class Stats {
  val counts = MetricsCounts()

  fun drank(player: Player, can: Can) {
    counts.drank(can.id, milligrams(Caffeines.rules.perCan))
  }

  fun crudeTaurine(player: Player) {
    counts.caffeine(milligrams(Caffeines.rules.perCrudeTaurine))
  }

  fun overdose(player: Player) {
    counts.overdose()
  }

  /** [millis] of gliding on a winged can. */
  fun glided(player: Player, millis: Long) {
    counts.glided(millis)
  }

  companion object {
    /** A real 250 ml can holds 80 mg of caffeine; a can here is [Rules.perCan] points of it. */
    const val CAN_MILLIGRAMS = 80.0

    fun milligrams(points: Double, rules: Rules = Caffeines.rules): Long = (points * CAN_MILLIGRAMS / rules.perCan).roundToLong()
  }
}
