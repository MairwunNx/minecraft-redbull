package ru.mairwunnx.redbull.neoforge

import com.mojang.logging.LogUtils
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.crafting.RecipeType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import org.slf4j.Logger
import ru.mairwunnx.redbull.core.CANS

const val MOD_ID = "redbull"

fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

/** Subscribes [handler] to events of type [T]; Kotlin lambdas don't carry the type the bus would look for. */
inline fun <reified T : Event> IEventBus.on(noinline handler: (T) -> Unit) = addListener(T::class.java, handler)

/**
 * Red Bull for NeoForge: the same cans, caffeine, and wings as the Paper plugin, from the same `core`.
 * Items are real items here, recipes and loot are data, and the client draws a caffeine meter.
 */
@Mod(MOD_ID)
class RedBullMod(modBus: IEventBus, container: ModContainer, dist: Dist) {
  init {
    container.registerConfig(ModConfig.Type.LOCAL, Settings.SPEC, "$MOD_ID.toml")
    Items.register(modBus)
    Caffeines.register(modBus)

    val bus = NeoForge.EVENT_BUS
    Drinks.listen(bus)
    Wings.listen(bus)
    Heart.listen(bus)
    Later.listen(bus)
    // Players find every crafting recipe in their recipe book.
    bus.on<PlayerEvent.PlayerLoggedInEvent> { event ->
      val player = event.entity as? ServerPlayer ?: return@on
      player.awardRecipes(player.level().server.recipeManager.recipes.filter {
        it.id().identifier().namespace == MOD_ID && it.value().type == RecipeType.CRAFTING
      })
    }
    bus.on<ServerStartedEvent> { event ->
      val recipes = event.server.recipeManager.recipes.count { it.id().identifier().namespace == MOD_ID }
      LOGGER.info("Red Bull is on: {} cans, {} items, {} recipes", CANS.size, Items.all.size, recipes)
    }
  }

  companion object {
    val LOGGER: Logger = LogUtils.getLogger()
  }
}
