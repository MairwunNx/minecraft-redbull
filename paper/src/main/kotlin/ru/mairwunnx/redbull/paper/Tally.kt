package ru.mairwunnx.redbull.paper

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import ru.mairwunnx.redbull.core.CANS
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * What one player ever drank and flew: the cans, by kind too, the caffeine in milligrams, deaths by overdose, and
 * milliseconds of gliding on a winged can. [name] is the last one seen, for boards and lookups by name.
 */
@Serializable
data class Drinker(
  val name: String? = null,
  val cans: Long = 0,
  val kinds: Map<String, Long> = emptyMap(),
  val caffeine: Long = 0,
  val overdoses: Long = 0,
  val flight: Long = 0,
) {
  val flightSeconds get() = flight / 1000

  /** The kind they drank most, the first in [CANS] on a tie; null before a can. */
  val favourite get() = CANS.map { it.id }.filter { (kinds[it] ?: 0) > 0 }.maxByOrNull { kinds[it] ?: 0 }
}

/**
 * Every player's [Drinker], all time, in `stats.yml`. The server thread counts; WorldAPI and Plan read from their
 * own threads, so the map is concurrent and each record is replaced whole. [save] writes it beside the config, through
 * a temporary file so a crash never leaves half of it.
 */
class Tally(private val file: File) {
  private val players = ConcurrentHashMap<UUID, Drinker>()
  private val dirty = AtomicBoolean()

  fun drank(player: UUID, name: String, kind: String, milligrams: Long) = update(player, name) {
    it.copy(cans = it.cans + 1, kinds = it.kinds + (kind to (it.kinds[kind] ?: 0) + 1), caffeine = it.caffeine + milligrams)
  }

  fun caffeine(player: UUID, name: String, milligrams: Long) = update(player, name) { it.copy(caffeine = it.caffeine + milligrams) }

  fun overdose(player: UUID, name: String) = update(player, name) { it.copy(overdoses = it.overdoses + 1) }

  fun glided(player: UUID, name: String, millis: Long) {
    if (millis > 0) update(player, name) { it.copy(flight = it.flight + millis) }
  }

  private fun update(player: UUID, name: String, change: (Drinker) -> Drinker) {
    players.compute(player) { _, before -> change(before ?: Drinker()).copy(name = name) }
    dirty.set(true)
  }

  fun of(player: UUID): Drinker = players[player] ?: Drinker()

  /** A UUID, or the name of a player who drank here, any case. */
  fun find(raw: String): UUID? = runCatching { UUID.fromString(raw) }.getOrNull()
    ?: players.entries.firstOrNull { it.value.name.equals(raw, ignoreCase = true) }?.key

  fun all(): Map<UUID, Drinker> = HashMap(players)

  /** Everyone's numbers added up: every can of every kind, in [CANS] order. */
  fun total(): Drinker = players.values.fold(Drinker()) { sum, it ->
    Drinker(
      cans = sum.cans + it.cans,
      kinds = (sum.kinds.keys + it.kinds.keys).associateWith { kind -> (sum.kinds[kind] ?: 0) + (it.kinds[kind] ?: 0) },
      caffeine = sum.caffeine + it.caffeine,
      overdoses = sum.overdoses + it.overdoses,
      flight = sum.flight + it.flight,
    )
  }

  /** Players with more than nothing by [score], best first, the more cans first on a tie; and how many there are. */
  fun board(limit: Int, score: (Drinker) -> Long): Pair<List<Pair<UUID, Drinker>>, Int> {
    val ranked = players.entries.map { it.key to it.value }.filter { score(it.second) > 0 }
      .sortedWith(compareByDescending<Pair<UUID, Drinker>> { score(it.second) }.thenByDescending { it.second.cans })
    return ranked.take(limit) to ranked.size
  }

  fun load() {
    if (!file.exists()) return
    players.putAll(yaml.decodeFromString(SERIALIZER, file.readText()).mapKeys { UUID.fromString(it.key) })
  }

  /** Writes the file when something changed since; true when it did. Safe from any thread, one write at a time. */
  @Synchronized
  fun save(): Boolean {
    if (!dirty.getAndSet(false)) return false
    try {
      file.parentFile?.mkdirs()
      val temporary = File(file.parentFile, "${file.name}.tmp")
      temporary.writeText(HEADER + yaml.encodeToString(SERIALIZER, all().mapKeys { it.key.toString() }.toSortedMap()))
      Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (e: Exception) {
      dirty.set(true)
      throw e
    }
    return true
  }

  companion object {
    private const val HEADER = "# What each player drank and flew, all time; Red Bull writes it. Caffeine in mg, flight in ms.\n"
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
    private val SERIALIZER = MapSerializer(String.serializer(), Drinker.serializer())
  }
}
