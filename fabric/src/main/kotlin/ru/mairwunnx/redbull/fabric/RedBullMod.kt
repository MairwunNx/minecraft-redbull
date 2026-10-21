package ru.mairwunnx.redbull.fabric

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.RecipeHolder
import org.slf4j.LoggerFactory
import ru.mairwunnx.redbull.core.CANS

object RedBullMod : ModInitializer {
  private val log = LoggerFactory.getLogger("Red Bull")

  override fun onInitialize() {
    Settings.load()
    RedBullItems.init()
    Caffeines.CAFFEINE
    Loot.init()
    Wings.init()

    ServerTickEvents.END_SERVER_TICK.register { server ->
      Drinks.tick(server)
      Heart.tick(server)
    }
    // Death takes all caffeine: the respawned player starts without the attachment; this clears the dead one's too.
    ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ -> if (entity is ServerPlayer) Caffeines.reset(entity) }
    // Players find every recipe in their recipe book.
    ServerPlayConnectionEvents.JOIN.register { handler, _, server -> handler.player.awardRecipes(crafting(server)) }
    ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> Drinks.forget(handler.player.uuid) }
    // `/reload` reads the settings again before the loot tables, which take the multipliers, load anew.
    ServerLifecycleEvents.START_DATA_PACK_RELOAD.register { _, _ -> Settings.load() }
    ServerLifecycleEvents.SERVER_STARTED.register { server ->
      val version = FabricLoader.getInstance().getModContainer("redbull").map { it.metadata.version.friendlyString }.orElse("dev")
      log.info("Red Bull $version is on: ${CANS.size} cans, ${crafting(server).size} recipes, ${RedBullItems.all.size} items")
    }
  }

  private fun crafting(server: MinecraftServer): List<RecipeHolder<*>> = server.recipeManager.recipes
    .filter { it.id().identifier().namespace == "redbull" && it.value() is CraftingRecipe }
}
