package ru.mairwunnx.redbull.fabric

import com.mojang.serialization.Codec
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.item.Item
import net.minecraft.world.level.gameevent.GameEvent
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.modcore.Caffeines
import ru.mairwunnx.redbull.modcore.Loader
import ru.mairwunnx.redbull.modcore.Mod
import ru.mairwunnx.redbull.modcore.RedBullItems
import ru.mairwunnx.redbull.modcore.Store
import ru.mairwunnx.redbull.modcore.Wings
import ru.mairwunnx.redbull.modcore.id

/** Red Bull on Fabric: modcore with Fabric API's registries, attachments, and events. */
object RedBullMod : ModInitializer {
  override fun onInitialize() {
    Settings.load()
    val version = FabricLoader.getInstance().getModContainer("redbull").map { it.metadata.version.friendlyString }.orElse("dev")
    Mod.init(OnFabric, version)
    val tab = FabricCreativeModeTab.builder().displayItems { _, output -> RedBullItems.shown.forEach(output::accept) }
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, RedBullItems.TAB, RedBullItems.tab(tab))
    Loot.init()

    CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> Mod.commands(dispatcher) }
    ServerTickEvents.END_SERVER_TICK.register(Mod::tick)
    ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ -> if (entity is ServerPlayer) Mod.died(entity) }
    ServerPlayConnectionEvents.JOIN.register { handler, _, _ -> Mod.joined(handler.player) }
    ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> Mod.left(handler.player) }
    // `/reload` reads the settings again before the loot tables, which take the multipliers, load anew.
    ServerLifecycleEvents.START_DATA_PACK_RELOAD.register { _, _ -> Settings.load() }
    ServerLifecycleEvents.SERVER_STARTED.register(Mod::started)
    ServerLifecycleEvents.SERVER_STOPPED.register { Mod.stopped() }
    EntitySleepEvents.ALLOW_SLEEPING.register { player, _ -> (player as? ServerPlayer)?.let(Caffeines::sleepProblem) }

    // Wings let their holder glide, on the server and the client alike. Asked every second of flight whether to wear
    // a glider, the event skips the rest of the game's tick, the vibration of the glide included: sculk still hears a
    // player on Wings, so the vibration goes out here.
    EntityElytraEvents.CUSTOM.register { entity, wear ->
      entity.hasEffect(Wings.EFFECT).also { if (it && wear) entity.gameEvent(GameEvent.ELYTRA_GLIDE) }
    }
    ServerMobEffectEvents.AFTER_REMOVE.register { instance, entity, _ ->
      // An effect that ran out has no time left; milk or a command take one that still has some.
      if (instance.`is`(Wings.EFFECT)) Wings.ended(entity, expired = !instance.isInfiniteDuration && instance.duration <= 0)
    }
  }
}

/** Fabric's side of modcore: everything registers at once, and both stores are Fabric API's attachments. */
private object OnFabric : Loader {
  override fun <T : Item> item(name: String, make: (Item.Properties) -> T): () -> T {
    val key = ResourceKey.create(Registries.ITEM, id(name))
    val item = Registry.register(BuiltInRegistries.ITEM, key, make(Item.Properties().setId(key)))
    return { item }
  }

  override fun effect(name: String, make: () -> MobEffect): Holder<MobEffect> =
    Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id(name), make())

  private val caffeineType: AttachmentType<Caffeine> = AttachmentRegistry.create(id("caffeine")) { it.persistent(Caffeines.CODEC.codec()) }

  private val collectedType: AttachmentType<List<String>> = AttachmentRegistry.create(id("collected")) {
    it.persistent(Codec.STRING.listOf()).copyOnDeath()
  }

  override val caffeine = attached(caffeineType)
  override val collected = attached(collectedType)
  override val settings = Settings

  private fun <T : Any> attached(type: AttachmentType<T>) = object : Store<T> {
    override fun get(player: ServerPlayer): T? = player.getAttached(type)

    override fun set(player: ServerPlayer, value: T) {
      player.setAttached(type, value)
    }
  }
}
