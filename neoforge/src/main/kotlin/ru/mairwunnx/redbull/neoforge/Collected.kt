package ru.mairwunnx.redbull.neoforge

import com.mojang.serialization.Codec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

/**
 * The cans a player has ever held, in a data attachment that survives death. A can counts the moment it reaches
 * them: picked up, crafted, drunk, given, or taken from a chest, which shows when they close it. Nothing runs
 * between those events, so a full server costs no more than an empty one.
 */
object Collected {
  private val ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID)

  private val COLLECTED: Supplier<AttachmentType<List<String>>> = ATTACHMENTS.register("collected") { ->
    AttachmentType.builder(Supplier { listOf<String>() })
      .serialize(Codec.STRING.listOf().fieldOf("cans"))
      .copyOnDeath()
      .build()
  }

  fun register(bus: IEventBus) = ATTACHMENTS.register(bus)

  fun of(player: ServerPlayer): Set<String> = player.getData(COLLECTED).toSet()

  fun record(player: ServerPlayer, items: Iterable<ItemStack>) {
    val ids = items.mapNotNullTo(mutableSetOf()) { Items.canOf(it)?.id }
    if (ids.isEmpty()) return
    val known = of(player)
    if (!known.containsAll(ids)) player.setData(COLLECTED, (known + ids).sorted())
  }

  fun listen(bus: IEventBus) {
    bus.on<ItemEntityPickupEvent.Post> { event ->
      (event.player as? ServerPlayer)?.let { record(it, listOf(event.originalStack)) }
    }
    bus.on<PlayerEvent.ItemCraftedEvent> { event ->
      (event.entity as? ServerPlayer)?.let { record(it, listOf(event.crafting)) }
    }
    bus.on<LivingEntityUseItemEvent.Finish> { event ->
      (event.entity as? ServerPlayer)?.let { record(it, listOf(event.item)) }
    }
    bus.on<PlayerContainerEvent.Close> { event ->
      (event.entity as? ServerPlayer)?.let { record(it, it.inventory) }
    }
    bus.on<PlayerEvent.PlayerLoggedInEvent> { event ->
      (event.entity as? ServerPlayer)?.let { record(it, it.inventory) }
    }
  }
}
