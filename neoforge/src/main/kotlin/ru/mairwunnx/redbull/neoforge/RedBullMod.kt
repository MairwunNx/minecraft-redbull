package ru.mairwunnx.redbull.neoforge

import com.mojang.serialization.Codec
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.common.NeoForgeMod
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent
import net.neoforged.neoforge.event.entity.living.MobEffectEvent
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.modcore.Caffeines
import ru.mairwunnx.redbull.modcore.Collected
import ru.mairwunnx.redbull.modcore.Drinks
import ru.mairwunnx.redbull.modcore.Loader
import ru.mairwunnx.redbull.modcore.MOD_ID
import ru.mairwunnx.redbull.modcore.RedBullItems
import ru.mairwunnx.redbull.modcore.Store
import ru.mairwunnx.redbull.modcore.Wings
import ru.mairwunnx.redbull.modcore.id
import java.util.function.Supplier
import ru.mairwunnx.redbull.modcore.Mod as Shared

/** Subscribes [handler] to events of type [T]; Kotlin lambdas don't carry the type the bus would look for. */
inline fun <reified T : Event> IEventBus.on(noinline handler: (T) -> Unit) = addListener(T::class.java, handler)

/** Red Bull on NeoForge: modcore with NeoForge's deferred registries, attachments, and events. */
@Mod(MOD_ID)
class RedBullMod(modBus: IEventBus, container: ModContainer) {
  init {
    container.registerConfig(ModConfig.Type.LOCAL, Settings.SPEC, "$MOD_ID.toml")
    Shared.init(OnNeoForge, container.modInfo.version.toString())
    OnNeoForge.tabs.register(MOD_ID) { ->
      RedBullItems.tab(CreativeModeTab.builder().displayItems { _, output -> RedBullItems.shown.forEach(output::accept) })
    }
    OnNeoForge.registers.forEach { it.register(modBus) }
    Loot.register(modBus)

    val bus = NeoForge.EVENT_BUS
    Loot.listen(bus)
    bus.on<RegisterCommandsEvent> { Shared.commands(it.dispatcher) }
    bus.on<ServerTickEvent.Post> { Shared.tick(it.server) }
    bus.on<LivingDeathEvent> { event -> (event.entity as? ServerPlayer)?.let(Shared::died) }
    bus.on<PlayerEvent.PlayerLoggedInEvent> { event -> (event.entity as? ServerPlayer)?.let(Shared::joined) }
    bus.on<PlayerEvent.PlayerLoggedOutEvent> { event -> (event.entity as? ServerPlayer)?.let(Shared::left) }
    bus.on<ServerStartedEvent> { Shared.started(it.server) }
    bus.on<ServerStoppedEvent> { Shared.stopped() }
    // The game has already checked the bed and set the spawn point by then.
    bus.on<CanPlayerSleepEvent> { event -> if (event.problem == null) event.problem = Caffeines.sleepProblem(event.entity) }
    // Milk takes some caffeine once it took every effect.
    bus.on<LivingEntityUseItemEvent.Finish> { event ->
      val player = event.entity as? ServerPlayer ?: return@on
      if (event.item.`is`(Items.MILK_BUCKET)) Drinks.milk(player)
    }
    bus.on<ItemEntityPickupEvent.Post> { event -> (event.player as? ServerPlayer)?.let { Collected.record(it, listOf(event.originalStack)) } }
    bus.on<PlayerContainerEvent.Close> { event -> (event.entity as? ServerPlayer)?.let { Collected.record(it, it.inventory) } }
    bus.on<MobEffectEvent.Expired> { event -> if (wings(event.effectInstance?.effect)) Wings.ended(event.entity, expired = true) }
    // Milk, `/effect clear`, and the like.
    bus.on<MobEffectEvent.Remove> { event ->
      if (wings(event.effect) && event.effectInstance != null) Wings.ended(event.entity, expired = false)
    }
  }

  private fun wings(effect: Holder<MobEffect>?) = effect != null && effect.value() == Wings.EFFECT.value()
}

/** NeoForge's side of modcore: everything waits for the registry events, and both stores are NeoForge's attachments. */
private object OnNeoForge : Loader {
  private val items = DeferredRegister.createItems(MOD_ID)
  private val effects = DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID)
  private val attachments = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID)
  val tabs: DeferredRegister<CreativeModeTab> = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID)
  val registers = listOf(items, effects, attachments, tabs)

  override fun <T : Item> item(name: String, make: (Item.Properties) -> T): () -> T = items.registerItem(name) { make(it) }::get

  override fun effect(name: String, make: () -> MobEffect): Holder<MobEffect> = effects.register(name, Supplier(make))

  /** NeoForge's `gliding_flight` attribute lets a player glide, checked by `LivingEntity.canGlide` on both sides. */
  override fun glider(effect: MobEffect): MobEffect =
    effect.addAttributeModifier(NeoForgeMod.GLIDING_FLIGHT, id("wings"), 1.0, AttributeModifier.Operation.ADD_VALUE)

  private val caffeineType: Supplier<AttachmentType<Caffeine>> = attachments.register("caffeine") { ->
    AttachmentType.builder(Supplier { Caffeine() }).serialize(Caffeines.CODEC).build()
  }

  private val collectedType: Supplier<AttachmentType<List<String>>> = attachments.register("collected") { ->
    AttachmentType.builder(Supplier { listOf<String>() }).serialize(Codec.STRING.listOf().fieldOf("cans")).copyOnDeath().build()
  }

  override val caffeine = attached(caffeineType)
  override val collected = attached(collectedType)
  override val settings = Settings

  private fun <T : Any> attached(type: Supplier<AttachmentType<T>>) = object : Store<T> {
    override fun get(player: ServerPlayer): T = player.getData(type)

    override fun set(player: ServerPlayer, value: T) {
      player.setData(type, value)
    }
  }
}
