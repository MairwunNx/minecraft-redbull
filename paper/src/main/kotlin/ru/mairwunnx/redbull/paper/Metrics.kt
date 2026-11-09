package ru.mairwunnx.redbull.paper

import org.bstats.bukkit.Metrics
import org.bstats.charts.AdvancedPie
import org.bstats.charts.SingleLineChart
import ru.mairwunnx.redbull.core.CANS
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * What bStats reads every 30 minutes, from its own thread. The server thread counts; bStats drains the counts, so
 * each send covers the period since the last one. Only numbers and the cans' fixed ids leave the server.
 */
class MetricsCounts {
  private val cans = AtomicLong()
  private val kinds = ConcurrentHashMap<String, AtomicLong>()
  private val caffeine = AtomicLong()
  private val overdoses = AtomicLong()
  private val flight = AtomicLong()

  fun drank(kind: String, milligrams: Long) {
    cans.incrementAndGet()
    if (kind in KINDS) kinds.computeIfAbsent(kind) { AtomicLong() }.incrementAndGet()
    caffeine.addAndGet(milligrams)
  }

  fun caffeine(milligrams: Long) {
    caffeine.addAndGet(milligrams)
  }

  fun overdose() {
    overdoses.incrementAndGet()
  }

  fun glided(millis: Long) {
    if (millis > 0) flight.addAndGet(millis)
  }

  fun drainCans(): Int = cans.getAndSet(0).clampToInt()
  fun drainKinds(): Map<String, Int> = buildMap {
    kinds.forEach { (kind, count) -> count.getAndSet(0).takeIf { it > 0 }?.let { put(kind, it.clampToInt()) } }
  }
  fun drainCaffeine(): Int = caffeine.getAndSet(0).clampToInt()
  fun drainOverdoses(): Int = overdoses.getAndSet(0).clampToInt()

  /** Whole seconds of flight; the milliseconds short of one wait for the next period. */
  fun drainFlightSeconds(): Int = (flight.getAndUpdate { it % 1000 } / 1000).clampToInt()

  private fun Long.clampToInt() = coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

  private companion object {
    val KINDS = CANS.map { it.id }.toSet()
  }
}

/** bStats for Red Bull on Paper. Any failure in it turns into one warning and never reaches the plugin. */
class PluginMetrics(private val plugin: RedBullPlugin, private val counts: MetricsCounts) {
  private var metrics: Metrics? = null
  private val warned = AtomicBoolean()

  /** Starts or stops bStats to match `metrics` in config.yml. */
  fun apply(enabled: Boolean) {
    if (!enabled || BSTATS_ID == 0) return stop()
    if (metrics != null) return
    metrics = safely(null) {
      Metrics(plugin, BSTATS_ID).apply {
        addCustomChart(SingleLineChart("cans_drunk", guarded(counts::drainCans)))
        addCustomChart(AdvancedPie("cans_by_kind", guarded(counts::drainKinds)))
        addCustomChart(SingleLineChart("caffeine_mg", guarded(counts::drainCaffeine)))
        addCustomChart(SingleLineChart("overdose_deaths", guarded(counts::drainOverdoses)))
        addCustomChart(SingleLineChart("wing_flight_seconds", guarded(counts::drainFlightSeconds)))
      }
    }
  }

  fun stop() {
    metrics?.let { safely(Unit) { it.shutdown() } }
    metrics = null
  }

  private fun <T> guarded(read: () -> T?): Callable<T?> = Callable { safely(null, read) }

  private inline fun <T> safely(fallback: T, block: () -> T): T = try {
    block()
  } catch (e: Throwable) {
    if (warned.compareAndSet(false, true)) plugin.logger.warning("bStats failed, Red Bull goes on without it: $e")
    fallback
  }

  companion object {
    /** The plugin's id on bstats.org; 0 keeps bStats off. */
    const val BSTATS_ID = 0
  }
}
