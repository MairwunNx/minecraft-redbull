package ru.mairwunnx.redbull.modcore

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.RecipeHolder
import org.slf4j.LoggerFactory
import ru.mairwunnx.redbull.core.CANS

const val MOD_ID = "redbull"

fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

/**
 * Red Bull in both mods: the same cans, caffeine, and wings as the Paper plugin, from the same `core`. Items are real
 * items here, recipes and loot are data, and caffeine shows as an effect. Each loader's entry calls [init] first, then
 * hands in its events through the functions below.
 */
object Mod {
  private val log = LoggerFactory.getLogger("Red Bull")

  lateinit var loader: Loader
    private set

  val settings: Settings get() = loader.settings

  /** The mod's version, as its loader reads it. */
  var version = "dev"
    private set

  /** Registers every item and effect through [loader]; [version] is the mod's, for the log. */
  fun init(loader: Loader, version: String) {
    this.loader = loader
    this.version = version
    // Reaching each registers what it holds.
    RedBullItems.TAB
    Caffeines.EFFECT
    Wings.EFFECT
  }

  fun commands(dispatcher: CommandDispatcher<CommandSourceStack>) = RedBullCommand.register(dispatcher)

  /** The end of every server tick. */
  fun tick(server: MinecraftServer) {
    Later.tick()
    Heart.tick(server)
  }

  /** Players find every crafting recipe in their recipe book, and a collection from before counts at once. */
  fun joined(player: ServerPlayer) {
    player.awardRecipes(crafting(player.level().server))
    Collected.joined(player)
  }

  fun left(player: ServerPlayer) = Drinks.forget(player.uuid)

  /** Death takes all caffeine; the respawned player starts without it anyway, and this clears the dead one's too. */
  fun died(player: ServerPlayer) = Caffeines.reset(player)

  /** As Paper says it: the version, the cans, the crafting recipes, and here the items too. */
  fun started(server: MinecraftServer) {
    log.info("Red Bull $version is on: ${CANS.size} cans, ${crafting(server).size} recipes, ${RedBullItems.all.size} items")
  }

  /** A singleplayer game starts a server for every world; what waited for this one is gone with it. */
  fun stopped() = Later.clear()

  private fun crafting(server: MinecraftServer): List<RecipeHolder<*>> = server.recipeManager.recipes
    .filter { it.id().identifier().namespace == MOD_ID && it.value() is CraftingRecipe }
}
