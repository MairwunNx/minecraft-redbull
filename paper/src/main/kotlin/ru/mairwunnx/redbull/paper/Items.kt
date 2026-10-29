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
import net.kyori.adventure.text.Component
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
import ru.mairwunnx.redbull.core.Colors
import ru.mairwunnx.redbull.core.Ids.CRUDE_TAURINE
import ru.mairwunnx.redbull.core.Ids.CRUMPLED_CAN
import ru.mairwunnx.redbull.core.Ids.EMPTY_CAN
import ru.mairwunnx.redbull.core.Ids.TAURINE
import ru.mairwunnx.redbull.core.Ids.WINGED_CAN
import ru.mairwunnx.redbull.core.wingedCanExpiry
import ru.mairwunnx.redbull.core.wingedCanSeconds
import ru.mairwunnx.redbull.core.can

/**
 * Every item but bull bile is a potion underneath: the game drinks it, shows its effects in the tooltip, applies them,
 * and keeps it out of vanilla crafting and trading. Components give it a model, a name, and a purpose;
 * the `redbull:item` mark says which of ours it is.
 */
object Items {
  val ITEM = NamespacedKey("redbull", "item")
  val EXPIRES = NamespacedKey("redbull", "expires")
  private val FLAVOUR = NamespacedKey("redbull", "flavour")

  private val DRINK_SOUND = Key.key("minecraft:item.honey_bottle.drink")
  private val BILE_COLOR = Color.fromRGB(Colors.BILE)
  private val CRUDE_COLOR = Color.fromRGB(Colors.CRUDE_TAURINE)

  fun id(item: ItemStack?): String? =
    item?.takeUnless { it.isEmpty }?.persistentDataContainer?.get(ITEM, PersistentDataType.STRING)

  fun canOf(item: ItemStack?): Can? = id(item)?.let(::can)

  /** Any of our items by id: a can, a bottle, taurine, or a can's leftovers. */
  fun create(id: String): ItemStack? = when (id) {
    EMPTY_CAN -> emptyCan()
    WINGED_CAN -> CANS.first().let { wingedCan(it.wings, wingedCanExpiry(System.currentTimeMillis(), it.wings), it) }
    CRUMPLED_CAN -> crumpledCan(CANS.first())
    BULL_BILE -> bile()
    CRUDE_TAURINE -> crudeTaurine()
    TAURINE -> taurine()
    else -> can(id)?.let(::can)
  }

  val ids get() = CANS.map { it.id } + listOf(EMPTY_CAN, WINGED_CAN, CRUMPLED_CAN, BULL_BILE, CRUDE_TAURINE, TAURINE)

  fun can(can: Can): ItemStack = base(can.id, rarity(can)).apply {
    setData(DataComponentTypes.LORE, ItemLore.lore(listOf(
      Texts.line("${can.translation}.flavour", NamedTextColor.GRAY),
      Texts.line(can.slogan, NamedTextColor.GOLD, italic = true),
    )))
    setData(DataComponentTypes.POTION_CONTENTS, effects(null, can.effects))
    setData(DataComponentTypes.CONSUMABLE, drink())
    setData(DataComponentTypes.USE_REMAINDER, UseRemainder.useRemainder(crumpledCan(can)))
    if (!can.sugarfree) {
      setData(DataComponentTypes.FOOD, FoodProperties.food().nutrition(2).saturation(1f).canAlwaysEat(true).build())
    }
  }

  fun emptyCan(): ItemStack = base(EMPTY_CAN).apply {
    unsetData(DataComponentTypes.CONSUMABLE)
    setData(DataComponentTypes.MAX_STACK_SIZE, 16)
    hint(EMPTY_CAN)
  }

  /** What a drink leaves: the can, crumpled, in its flavour's colours. It stacks, and a furnace melts it down. */
  fun crumpledCan(can: Can): ItemStack = base(CRUMPLED_CAN).apply {
    unsetData(DataComponentTypes.CONSUMABLE)
    setData(DataComponentTypes.ITEM_MODEL, Key.key("redbull", can.crumpled))
    setData(DataComponentTypes.MAX_STACK_SIZE, 64)
    setData(DataComponentTypes.LORE, ItemLore.lore(listOf(
      Texts.line(can.translation, NamedTextColor.GRAY),
      Texts.line("item.redbull.$CRUMPLED_CAN.hint", NamedTextColor.GRAY),
    )))
    editPersistentDataContainer { it.set(FLAVOUR, PersistentDataType.STRING, can.id) }
  }

  /** The can a winged or crumpled can was before it was drunk. */
  fun flavourOf(item: ItemStack): Can? = item.persistentDataContainer.get(FLAVOUR, PersistentDataType.STRING)?.let(::can)

  /**
   * The can after a drink with wings: a glider held in the main hand. Its durability is its seconds on the ground,
   * [wingedCanSeconds] of [seconds] of wings, which [Wings] wears down in flight as many times faster, so a fresh can
   * still glides for [seconds]. The game stops a glider a point before it breaks. However it was worn, it crumples at
   * [expires] (epoch milliseconds), as a can of [flavour] would.
   */
  fun wingedCan(seconds: Int, expires: Long, flavour: Can): ItemStack = base(WINGED_CAN).apply {
    unsetData(DataComponentTypes.CONSUMABLE)
    setData(DataComponentTypes.GLIDER)
    setData(DataComponentTypes.EQUIPPABLE, Equippable.equippable(EquipmentSlot.HAND).swappable(false).build())
    setData(DataComponentTypes.MAX_DAMAGE, wingedCanSeconds(seconds) + 1)
    setData(DataComponentTypes.DAMAGE, 0)
    editPersistentDataContainer {
      it.set(EXPIRES, PersistentDataType.LONG, expires)
      it.set(FLAVOUR, PersistentDataType.STRING, flavour.id)
    }
    setData(DataComponentTypes.LORE, ItemLore.lore(listOf(
      Texts.line("item.redbull.$WINGED_CAN.hint", NamedTextColor.GRAY),
      Texts.line("item.redbull.$WINGED_CAN.fades", NamedTextColor.GRAY, args = arrayOf(Component.text(wingedCanSeconds(seconds)))),
    )))
  }

  /**
   * Bull bile is an ominous bottle underneath, drawn and drunk as a potion. A brewing stand's ingredient slot, the
   * client's included, takes an item by its type alone, so a potion there would let every potion in and send a
   * shift-clicked water bottle to the ingredient slot. The ominous bottle is an item of components only, without a
   * recipe, tag, or trade; without its amplifier it does nothing, and every brewing ingredient slot takes it.
   */
  fun bile(): ItemStack = mark(ItemStack.of(Material.OMINOUS_BOTTLE), BULL_BILE).apply {
    val potion = ItemStack.of(Material.POTION)
    unsetData(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER)
    setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft:potion"))
    setData(DataComponentTypes.CUSTOM_NAME, Texts.line("item.redbull.$BULL_BILE"))
    setData(DataComponentTypes.RARITY, ItemRarity.COMMON)
    setData(DataComponentTypes.MAX_STACK_SIZE, 1)
    setData(DataComponentTypes.POTION_CONTENTS, effects(BILE_COLOR, Bottles.BILE))
    setData(DataComponentTypes.CONSUMABLE, potion.getData(DataComponentTypes.CONSUMABLE)!!)
    setData(DataComponentTypes.USE_REMAINDER, potion.getData(DataComponentTypes.USE_REMAINDER)!!)
    hint(BULL_BILE)
  }

  /** Bile from 1.4.1 and before: a potion, which no brewing stand takes. [Upgrades] swaps it for today's. */
  fun isOldBile(item: ItemStack?): Boolean = item?.type == Material.POTION && id(item) == BULL_BILE

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

  /** The chest-only legends get a yellow name, as a bottle o' enchanting does: things you can find, never make. */
  private fun rarity(can: Can) = if (can.recipe == null) ItemRarity.UNCOMMON else ItemRarity.COMMON
}
