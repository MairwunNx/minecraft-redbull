package ru.mairwunnx.redbull.neoforge

import net.neoforged.neoforge.common.ModConfigSpec

/**
 * `config/redbull.toml`: the same switches, comments, and defaults as Fabric's file of that name and Paper's
 * `config.yml` without the pack. NeoForge reads it again whenever it's saved.
 */
object Settings : ru.mairwunnx.redbull.modcore.Settings {
  private val builder = ModConfigSpec.Builder()

  private val OVERDOSE: ModConfigSpec.BooleanValue = builder
    .comment("Four cans too fast, and the heart gives out. Off: caffeine still builds up, but nobody dies.")
    .define("overdose", true)

  private val WINGS: ModConfigSpec.BooleanValue = builder
    .comment("Cans with wings launch the player and give the Wings effect to glide with.")
    .define("wings", true)

  private val BILE: ModConfigSpec.DoubleValue
  private val CHESTS: ModConfigSpec.DoubleValue

  init {
    builder.push("loot")
    BILE = builder
      .comment("Multiplies the chance that a cow drops bull bile (5%, plus 2% per Looting level).")
      .defineInRange("bile", 1.0, 0.0, 100.0)
    CHESTS = builder
      .comment("Multiplies each can's chance to turn up in a chest of its biomes.")
      .defineInRange("chests", 1.0, 0.0, 100.0)
    builder.pop()
  }

  val SPEC: ModConfigSpec = builder.build()

  override val overdose get() = OVERDOSE.get()
  override val wings get() = WINGS.get()
  override val bile get() = BILE.get()
  override val chests get() = CHESTS.get()
}
