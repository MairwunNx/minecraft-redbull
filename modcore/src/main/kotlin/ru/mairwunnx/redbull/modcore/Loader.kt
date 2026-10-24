package ru.mairwunnx.redbull.modcore

import net.minecraft.core.Holder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.item.Item
import ru.mairwunnx.redbull.core.Caffeine

/**
 * What a loader hands modcore: its registries, its data attachments, and its settings. Fabric registers at once,
 * NeoForge when its registry events come, so what it registers comes back as something to read later.
 */
interface Loader {
  /** Registers `redbull:<name>`; [make] builds the item from properties that already carry its id. */
  fun <T : Item> item(name: String, make: (Item.Properties) -> T): () -> T

  /** Registers the effect `redbull:<name>`. */
  fun effect(name: String, make: () -> MobEffect): Holder<MobEffect>

  /** The Wings effect as this loader lets a player glide on it; Fabric asks an event instead and leaves it as it is. */
  fun glider(effect: MobEffect): MobEffect = effect

  /** A player's caffeine, saved with them and gone on death. */
  val caffeine: Store<Caffeine>

  /** The cans a player has ever held, kept through death. */
  val collected: Store<List<String>>

  val settings: Settings
}

/** Something saved with each player. */
interface Store<T : Any> {
  fun get(player: ServerPlayer): T?

  fun set(player: ServerPlayer, value: T)
}

/** `config/redbull.toml`, the same switches on both loaders and Paper's `config.yml` without the pack. */
interface Settings {
  /** Four cans too fast, and the heart gives out. Off: caffeine still builds up, but nobody dies. */
  val overdose: Boolean

  /** Cans with wings launch the player and give the Wings effect to glide with. */
  val wings: Boolean

  /** Multiplies the chance that a cow drops bull bile. */
  val bile: Double

  /** Multiplies each can's chance to turn up in a chest of its biomes. */
  val chests: Double
}
