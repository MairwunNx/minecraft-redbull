package ru.mairwunnx.redbull.paper

import org.bukkit.entity.Player
import java.io.File
import ru.mairwunnx.redbull.core.Can
import ru.mairwunnx.redbull.core.Rules
import kotlin.math.roundToLong

/**
 * Where drinks, overdoses, and flights are counted: each player's numbers for all time in [tally], and the counts
 * bStats drains every 30 minutes.
 */
class Stats(file: File) {
  val tally = Tally(file)
  val counts = MetricsCounts()

  fun drank(player: Player, can: Can) {
    val milligrams = milligrams(Caffeines.rules.perCan)
    tally.drank(player.uniqueId, player.name, can.id, milligrams)
    counts.drank(can.id, milligrams)
  }

  fun crudeTaurine(player: Player) {
    val milligrams = milligrams(Caffeines.rules.perCrudeTaurine)
    tally.caffeine(player.uniqueId, player.name, milligrams)
    counts.caffeine(milligrams)
  }

  fun overdose(player: Player) {
    tally.overdose(player.uniqueId, player.name)
    counts.overdose()
  }

  /** [millis] of gliding on a winged can. */
  fun glided(player: Player, millis: Long) {
    tally.glided(player.uniqueId, player.name, millis)
    counts.glided(millis)
  }

  companion object {
    /** A real 250 ml can holds 80 mg of caffeine; a can here is [Rules.perCan] points of it. */
    const val CAN_MILLIGRAMS = 80.0

    fun milligrams(points: Double, rules: Rules = Caffeines.rules): Long = (points * CAN_MILLIGRAMS / rules.perCan).roundToLong()
  }
}
