@file:Suppress("UnstableApiUsage")

package ru.mairwunnx.redbull.paper

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.Equippable
import io.papermc.paper.datacomponent.item.FoodProperties
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.PotionContents
import io.papermc.paper.datacomponent.item.UseRemainder
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionType
import ru.mairwunnx.redbull.core.Bottles
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Can
import ru.mairwunnx.redbull.core.Effect
import ru.mairwunnx.redbull.core.Ids.BULL_BILE
import ru.mairwunnx.redbull.core.Ids.CRUDE_TAURINE
import ru.mairwunnx.redbull.core.Ids.EMPTY_CAN
import ru.mairwunnx.redbull.core.Ids.TAURINE
import ru.mairwunnx.redbull.core.Ids.WINGED_CAN
import ru.mairwunnx.redbull.core.WINGS_GRACE_MILLIS
import ru.mairwunnx.redbull.core.can

/**
 * Every item is a potion underneath: the game drinks it, shows its effects in the tooltip, applies them,
 * and keeps it out of vanilla crafting and trading. Components give it a model, a name, and a purpose;
 * the `redbull:item` mark says which of ours it is.
 */
object Items {
  val ITEM = NamespacedKey("redbull", "item")
  val EXPIRES = NamespacedKey("redbull", "expires")

  private val DRINK_SOUND = Key.key("minecraft:item.honey_bottle.drink")
  private val BILE_COLOR = Color.fromRGB(0x9FB230)
  private val CRUDE_COLOR = Color.fromRGB(0xEFE3B0)

  fun id(item: ItemStack?): String? =
    item?.takeUnless { it.isEmpty }?.persistentDataContainer?.get(ITEM, PersistentDataType.STRING)

  fun canOf(item: ItemStack?): Can? = id(item)?.let(::can)

  /** Any of our items by id: a can, a bottle, taurine, or a can's leftovers. */
  fun create(id: String): ItemStack? = when (id) {
    EMPTY_CAN -> emptyCan()
    WINGED_CAN -> wingedCan(8, System.currentTimeMillis() + WINGS_GRACE_MILLIS)
    BULL_BILE -> bile()
    CRUDE_TAURINE -> crudeTaurine()
    TAURINE -> taurine()
    else -> can(id)?.let(::can)
  }

  val ids get() = CANS.map { it.id } + listOf(EMPTY_CAN, WINGED_CAN, BULL_BILE, CRUDE_TAURINE, TAURINE)

  fun can(can: Can): ItemStack = base(can.id, rarity(can)).apply {
    setData(DataComponentTypes.LORE, ItemLore.lore(listOf(
      Texts.line("${can.translation}.flavour", NamedTextColor.GRAY),
      Texts.line(can.slogan, NamedTextColor.GOLD, italic = true),
    )))
    setData(DataComponentTypes.POTION_CONTENTS, effects(null, can.effects))
    setData(DataComponentTypes.CONSUMABLE, drink())
    setData(DataComponentTypes.USE_REMAINDER, UseRemainder.useRemainder(emptyCan()))
    if (!can.sugarfree) {
      setData(DataComponentTypes.FOOD, FoodProperties.food().nutrition(2).saturation(1f).canAlwaysEat(true).build())
    }
  }

  fun emptyCan(): ItemStack = base(EMPTY_CAN).apply {
    unsetData(DataComponentTypes.CONSUMABLE)
    setData(DataComponentTypes.MAX_STACK_SIZE, 16)
    hint(EMPTY_CAN)
  }

  /**
   * The can after a drink with wings: a glider held in the main hand. The game wears a glider by one point per
   * second of flight and stops one point before it breaks, so [seconds] of flight is all it gives. Unflown, it
   * turns back into an empty can at [expires] (epoch milliseconds); see [Wings].
   */
  fun wingedCan(seconds: Int, expires: Long): ItemStack = base(WINGED_CAN).apply {
    unsetData(DataComponentTypes.CONSUMABLE)
    setData(DataComponentTypes.GLIDER)
    setData(DataComponentTypes.EQUIPPABLE, Equippable.equippable(EquipmentSlot.HAND).swappable(false).build())
    setData(DataComponentTypes.MAX_DAMAGE, seconds + 1)
    setData(DataComponentTypes.DAMAGE, 0)
    editPersistentDataContainer { it.set(EXPIRES, PersistentDataType.LONG, expires) }
    hint(WINGED_CAN)
  }

  fun bile(): ItemStack = bottle(BULL_BILE, BILE_COLOR, Bottles.BILE)

  fun crudeTaurine(): ItemStack = bottle(CRUDE_TAURINE, CRUDE_COLOR, Bottles.CRUDE_TAURINE)

  fun taurine(): ItemStack = base(TAURINE).apply {
    unsetData(DataComponentTypes.CONSUMABLE)
    setData(DataComponentTypes.MAX_STACK_SIZE, 64)
    hint(TAURINE)
  }

  fun waterBottle(): ItemStack = ItemStack.of(Material.POTION).apply {
    setData(DataComponentTypes.POTION_CONTENTS, PotionContents.potionContents().potion(PotionType.WATER).build())
  }

  fun isWaterBottle(item: ItemStack): Boolean =
    item.type == Material.POTION && id(item) == null &&
      item.getData(DataComponentTypes.POTION_CONTENTS)?.potion() == PotionType.WATER

  /** A bottle keeps the game's own potion model, tinted [color], and the glass bottle it leaves. */
  private fun bottle(id: String, color: Color, effects: List<Effect>): ItemStack = mark(ItemStack.of(Material.POTION), id).apply {
    setData(DataComponentTypes.CUSTOM_NAME, Texts.line("item.redbull.$id"))
    setData(DataComponentTypes.POTION_CONTENTS, effects(color, effects))
    hint(id)
  }

  private fun base(id: String, rarity: ItemRarity = ItemRarity.COMMON): ItemStack = mark(ItemStack.of(Material.POTION), id).apply {
    setData(DataComponentTypes.ITEM_MODEL, Key.key("redbull", id))
    // A potion names itself after its contents and ignores item_name; only custom_name wins over that.
    setData(DataComponentTypes.CUSTOM_NAME, Texts.line("item.redbull.$id"))
    setData(DataComponentTypes.RARITY, rarity)
    unsetData(DataComponentTypes.POTION_CONTENTS)
  }

  private fun mark(item: ItemStack, id: String) = item.apply {
    editPersistentDataContainer { it.set(ITEM, PersistentDataType.STRING, id) }
  }

  private fun ItemStack.hint(id: String) {
    setData(DataComponentTypes.LORE, ItemLore.lore(listOf(Texts.line("item.redbull.$id.hint", NamedTextColor.GRAY))))
  }

  private fun drink() = Consumable.consumable()
    .consumeSeconds(1.6f)
    .animation(ItemUseAnimation.DRINK)
    .sound(DRINK_SOUND)
    .hasConsumeParticles(false)
    .build()

  private fun effects(color: Color?, effects: List<Effect>): PotionContents {
    val builder = PotionContents.potionContents()
    color?.let(builder::customColor)
    effects.forEach { effect -> potionEffect(effect)?.let(builder::addCustomEffect) }
    return builder.build()
  }

  fun potionEffect(effect: Effect): PotionEffect? {
    val type = Registry.MOB_EFFECT.get(Key.key(effect.id)) ?: return null
    return PotionEffect(type, effect.seconds * 20, effect.amplifier)
  }

  /** Chest-only cans are the legends; cans that also turn up in chests are a step above the basics. */
  private fun rarity(can: Can) = when {
    can.recipe == null -> ItemRarity.EPIC
    can.chests != null -> ItemRarity.UNCOMMON
    else -> ItemRarity.COMMON
  }
}
