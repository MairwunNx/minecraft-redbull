package ru.mairwunnx.redbull.fabric

import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

/**
 * `config/redbull.toml`: the same switches as NeoForge's file of that name and Paper's `config.yml` without the pack.
 * It's read on start and on `/reload`; a missing file is written with the defaults, and a missing key keeps its own.
 * Only the few lines it needs of TOML are understood: `key = value`, one `[loot]` table, and `#` comments.
 */
object Settings {
  private val log = LoggerFactory.getLogger("Red Bull")

  /** Four cans too fast, and the heart gives out. Off: caffeine still builds up, but nobody dies. */
  var overdose = true
    private set

  /** Cans with wings launch the player and give the Wings effect to glide with. */
  var wings = true
    private set

  /** Multiplies the chance that a cow drops bull bile. */
  var bile = 1.0
    private set

  /** Multiplies each can's chance to turn up in a chest of its biomes. */
  var chests = 1.0
    private set

  private val DEFAULTS = """
    |# Four cans too fast, and the heart gives out. Off: caffeine still builds up, but nobody dies.
    |overdose = true
    |# Cans with wings launch the player and give the Wings effect to glide with.
    |wings = true
    |
    |[loot]
    |# Multiplies the chance that a cow drops bull bile (5%, plus 2% per Looting level).
    |bile = 1.0
    |# Multiplies each can's chance to turn up in a chest of its biomes.
    |chests = 1.0
    |""".trimMargin()

  fun load(file: Path = FabricLoader.getInstance().configDir.resolve("redbull.toml")) {
    if (Files.notExists(file)) {
      Files.createDirectories(file.parent)
      Files.writeString(file, DEFAULTS)
    }
    read(Files.readString(file))
  }

  /** Takes the values [text] sets; a key it doesn't set, or sets to nonsense, goes back to its default. */
  fun read(text: String) {
    val values = parse(text)
    overdose = values["overdose"]?.toBooleanStrictOrNull() ?: true
    wings = values["wings"]?.toBooleanStrictOrNull() ?: true
    bile = multiplier(values["loot.bile"])
    chests = multiplier(values["loot.chests"])
  }

  private fun multiplier(value: String?) = value?.toDoubleOrNull()?.takeIf { it in 0.0..100.0 } ?: 1.0

  /** Every `key = value` under its table's name: `overdose`, `loot.bile`. */
  private fun parse(text: String): Map<String, String> {
    val values = mutableMapOf<String, String>()
    var table = ""
    for (raw in text.lines()) {
      val line = raw.substringBefore('#').trim()
      when {
        line.isEmpty() -> continue
        line.startsWith("[") && line.endsWith("]") -> table = line.removeSurrounding("[", "]").trim()
        '=' in line -> {
          val key = line.substringBefore('=').trim()
          val value = line.substringAfter('=').trim().removeSurrounding("\"")
          values[if (table.isEmpty()) key else "$table.$key"] = value
        }
        else -> log.warn("Skipped a line of redbull.toml: {}", raw)
      }
    }
    return values
  }
}
