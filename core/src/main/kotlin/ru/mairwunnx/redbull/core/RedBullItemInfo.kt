package ru.mairwunnx.redbull.core

import chat.allay.expansion.Ingredient
import chat.allay.expansion.ItemInfo
import chat.allay.expansion.Recipe

/**
 * Red Bull's items as [RedBullExpansion.items] hands them to Allay's catalog: their names in every language, a line on
 * what each one is, and what Allay can't read from the game itself.
 *
 * The mods register every item, and their recipes are data, so Allay finds them all and the items add only names and
 * descriptions. On Paper every item is a potion underneath and Allay sees one only as what a recipe makes, by its
 * model: the crafted cans, the empty can, and taurine. There the items also name the game's item underneath, crude
 * taurine's brew, a mix that matches in code, comes as a recipe, and so does taurine's furnace, whose input Allay reads
 * only as “potion”.
 */
internal object RedBullItemInfo {
  private const val POTION = "potion"

  fun all(paper: Boolean): List<ItemInfo> =
    CANS.map { can(it, paper) } + listOf(emptyCan(paper), bile(paper), crudeTaurine(paper), taurine(paper)) +
      (if (paper) listOf(wingedCan()) else emptyList()) + CANS.map { crumpled(it, paper) }

  private fun can(can: Can, paper: Boolean): ItemInfo {
    val wings = if (can.wings > 0) ", and ${can.wings} s of wings to glide on" else ""
    val way = when {
      can.recipe == null -> "A legend found only in chests."
      can.chests != null -> "Crafted, or found in chests."
      else -> "Crafted at a crafting table."
    }
    val sugar = if (can.sugarfree) "Sugarfree: no crash, no sugar pit." else "With sugar."
    val text = "An energy drink: ${can.effects.joinToString { effect(it) }}, ${RULES.perCan.toInt()} caffeine$wings. $sugar $way"
    return item(can.id, can.translation, text, if (paper) POTION else null)
  }

  private fun crumpled(can: Can, paper: Boolean) = item(
    can.crumpled, "item.redbull.${Ids.CRUMPLED_CAN}",
    "What a drink of ${Names.name(can)} leaves. A furnace smelts it into $CRUMPLED_NUGGETS iron nuggets.", if (paper) POTION else null,
  )

  private fun emptyCan(paper: Boolean) = item(
    Ids.EMPTY_CAN, "item.redbull.${Ids.EMPTY_CAN}",
    "Four iron nuggets and a copper nugget at a crafting table. Every can's recipe fills one with taurine.", if (paper) POTION else null,
  )

  private fun bile(paper: Boolean) = item(
    Ids.BULL_BILE, "item.redbull.${Ids.BULL_BILE}",
    "Drops from an adult cow or mooshroom a player kills (${percent(Bile.CHANCE)}, ${percent(Bile.PER_LOOTING)} more per Looting level). " +
      "Brewed into a water bottle, it makes crude taurine. Drunk, it makes you moo.",
    if (paper) "ominous_bottle" else null,
  )

  private fun crudeTaurine(paper: Boolean): ItemInfo {
    val info = item(
      Ids.CRUDE_TAURINE, "item.redbull.${Ids.CRUDE_TAURINE}",
      "Bull bile brewed into a water bottle. A furnace evaporates it into taurine. Drunk, it adds ${RULES.perCrudeTaurine.toInt()} caffeine.",
      if (paper) POTION else null,
    )
    // Paper's brewing stand takes bile by a mix that matches in code, which Allay can't read.
    return if (paper) info.recipe(Recipe.brewing(Ingredient.potion("water"), Ingredient.item("redbull:${Ids.BULL_BILE}"))) else info
  }

  private fun taurine(paper: Boolean): ItemInfo {
    val info = item(Ids.TAURINE, "item.redbull.${Ids.TAURINE}", "Crude taurine evaporated in a furnace. It goes into every can's recipe.", if (paper) POTION else null)
    // Paper's furnace takes crude taurine by the exact bottle, a potion without a model of its own to Allay.
    return if (paper) info.recipe(Recipe.cooking(Recipe.Type.FURNACE, Ingredient.item("redbull:${Ids.CRUDE_TAURINE}"), RedBullExpansion.EVAPORATE_SECONDS.toDouble(), 0.35)) else info
  }

  private fun wingedCan() = item(
    Ids.WINGED_CAN, "item.redbull.${Ids.WINGED_CAN}",
    "What a can with wings leaves on Paper. Held in the main hand, it glides like an elytra until its wings wear out, faster in flight, then it crumples.",
    POTION,
  )

  private fun item(id: String, key: String, description: String, base: String?): ItemInfo {
    val info = ItemInfo.of("redbull:$id").description(description)
    Names.translations(key).forEach { (locale, name) -> info.name(locale, name) }
    return if (base != null) info.base(base) else info
  }

  private fun effect(effect: Effect) =
    "${RedBullExpansion.bare(effect.id).split('_').joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }} ${ROMAN[effect.amplifier.coerceIn(0, ROMAN.lastIndex)]} for ${effect.seconds} s"

  private fun percent(chance: Double) = "${(chance * 100).toInt()}%"

  private val ROMAN = listOf("I", "II", "III", "IV", "V")

  private val RULES = Rules()
}
