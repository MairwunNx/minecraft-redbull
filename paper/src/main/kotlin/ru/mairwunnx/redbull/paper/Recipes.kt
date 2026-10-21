package ru.mairwunnx.redbull.paper

import io.papermc.paper.potion.PotionMix
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Ids

/**
 * The way to a can: bile and a water bottle brew into crude taurine, a furnace evaporates it into taurine,
 * and a crafting table puts taurine, a flavour, and a dye into an empty can. Cans without a recipe are only
 * found in chests. Crafting recipes show in the recipe book, so players learn them there.
 */
class Recipes(private val server: Server) {
  private val crafting = mutableListOf<NamespacedKey>()
  private val brewing = NamespacedKey("redbull", "crude_taurine")
  private val evaporating = NamespacedKey("redbull", "taurine")

  /** The crafting recipes, for the recipe book. */
  val keys: List<NamespacedKey> get() = crafting

  fun register() {
    shaped(Ids.EMPTY_CAN, Items.emptyCan().asQuantity(2), "I I", "I I", " C ") {
      setIngredient('I', Material.IRON_NUGGET)
      setIngredient('C', Material.COPPER_NUGGET)
    }
    for (can in CANS) {
      val recipe = can.recipe ?: continue
      val rows = if (can.sugarfree) arrayOf("TDT", "FCF", " W ") else arrayOf("TDT", "FCF", "SWS")
      shaped(can.id, Items.can(can), *rows) {
        setIngredient('T', RecipeChoice.ExactChoice(Items.taurine()))
        setIngredient('D', material(recipe.dye))
        setIngredient('F', choice(recipe.flavour))
        setIngredient('C', RecipeChoice.ExactChoice(Items.emptyCan()))
        setIngredient('W', RecipeChoice.ExactChoice(Items.waterBottle()))
        if (!can.sugarfree) setIngredient('S', Material.SUGAR)
      }
    }

    server.potionBrewer.addPotionMix(PotionMix(
      brewing, Items.crudeTaurine(),
      PotionMix.createPredicateChoice(Items::isWaterBottle),
      PotionMix.createPredicateChoice { Items.id(it) == Ids.BULL_BILE },
    ))

    server.addRecipe(FurnaceRecipe(evaporating, Items.taurine(), RecipeChoice.ExactChoice(Items.crudeTaurine()), 0.35f, 200), true)
  }

  fun unregister() {
    crafting.forEach(server::removeRecipe)
    crafting.clear()
    server.removeRecipe(evaporating)
    server.potionBrewer.removePotionMix(brewing)
  }

  private fun shaped(id: String, result: ItemStack, vararg rows: String, ingredients: ShapedRecipe.() -> Unit) {
    val key = NamespacedKey("redbull", id)
    val recipe = ShapedRecipe(key, result).shape(*rows).apply(ingredients)
    recipe.group = "redbull"
    server.addRecipe(recipe, true)
    crafting += key
  }

  private fun material(id: String): Material = Material.matchMaterial(id) ?: error("No item $id")

  private fun choice(id: String): RecipeChoice =
    if (id == Ids.TAURINE_ITEM) RecipeChoice.ExactChoice(Items.taurine()) else RecipeChoice.MaterialChoice(material(id))
}
