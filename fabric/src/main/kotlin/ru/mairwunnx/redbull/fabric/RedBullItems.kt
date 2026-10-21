package ru.mairwunnx.redbull.fabric

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.minecraft.ChatFormatting
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.Items
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.Consumable
import net.minecraft.world.item.component.Consumables
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.UseRemainder
import ru.mairwunnx.redbull.core.Bottles
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Can
import ru.mairwunnx.redbull.core.Effect
import ru.mairwunnx.redbull.core.Ids
import java.util.Optional

/**
 * Every item is a real item with its own id, named after its asset: `redbull:original`, `redbull:crumpled_original`,
 * `redbull:empty_can`. Data components give each its purpose, as on Paper, but here they're the item's defaults.
 */
object RedBullItems {
  /** Bottles tint the game's potion model, as on Paper. */
  private const val BILE_COLOR = 0x9FB230
  private const val CRUDE_COLOR = 0xEFE3B0

  /** Registered first: every can names its crumpled can as what the drink leaves. */
  val crumpled: Map<String, Item> = CANS.associate { it.id to crumpled(it) }
  val cans: Map<String, CanItem> = CANS.associate { it.id to can(it) }

  val emptyCan: Item = register(Ids.EMPTY_CAN, ::Item, hint(Ids.EMPTY_CAN).stacksTo(16))
  val taurine: Item = register(Ids.TAURINE, ::Item, hint(Ids.TAURINE))
  val bile: BottleItem = bottle(Ids.BULL_BILE, BILE_COLOR, Bottles.BILE)
  val crudeTaurine: BottleItem = bottle(Ids.CRUDE_TAURINE, CRUDE_COLOR, Bottles.CRUDE_TAURINE)

  /** Every item, in the order the creative tab lists them. */
  val all: List<Item> get() =
    cans.values.toList() + listOf(emptyCan, taurine, bile, crudeTaurine) + crumpled.values

  fun init() {
    val key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("redbull"))
    val tab = FabricCreativeModeTab.builder()
      .title(Component.translatable("item.redbull.original"))
      .icon { ItemStack(cans.getValue("original")) }
      .displayItems { _, output -> all.forEach(output::accept) }
      .build()
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key, tab)
  }

  fun canOf(stack: ItemStack): Can? = (stack.item as? CanItem)?.can

  private fun can(can: Can): CanItem {
    val properties = Item.Properties()
      .stacksTo(1)
      .rarity(if (can.recipe == null) Rarity.UNCOMMON else Rarity.COMMON)
      .component(DataComponents.LORE, ItemLore(listOf(
        line("${can.translation}.flavour", ChatFormatting.GRAY),
        line(can.slogan, ChatFormatting.GOLD, italic = true),
      )))
      .component(DataComponents.POTION_CONTENTS, effects(null, can.effects))
      .component(DataComponents.CONSUMABLE, drink())
      .component(DataComponents.USE_REMAINDER, UseRemainder(ItemStackTemplate(crumpled.getValue(can.id))))
    // Sugar feeds; the sugarfree cans don't.
    if (!can.sugarfree) properties.component(DataComponents.FOOD, FoodProperties(2, 1f, true))
    return register(can.id, { CanItem(it, can) }, properties)
  }

  /** What a drink leaves: the can, crumpled, in its flavour's colours. It stacks, and a furnace melts it down. */
  private fun crumpled(can: Can): Item = register(
    can.crumpled, ::Item,
    Item.Properties()
      .overrideDescription("item.redbull.${Ids.CRUMPLED_CAN}")
      .component(DataComponents.LORE, ItemLore(listOf(
        line(can.translation, ChatFormatting.GRAY),
        line("item.redbull.${Ids.CRUMPLED_CAN}.hint", ChatFormatting.GRAY),
      ))),
  )

  /** A bottle keeps the game's own potion model, tinted, and the glass bottle it leaves. */
  private fun bottle(id: String, color: Int, effects: List<Effect>): BottleItem = register(
    id, ::BottleItem,
    hint(id)
      .stacksTo(1)
      .component(DataComponents.POTION_CONTENTS, effects(color, effects))
      .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
      .usingConvertsTo(Items.GLASS_BOTTLE),
  )

  private fun hint(id: String) = Item.Properties()
    .component(DataComponents.LORE, ItemLore(listOf(line("item.redbull.$id.hint", ChatFormatting.GRAY))))

  private fun drink() = Consumable.builder()
    .consumeSeconds(1.6f)
    .animation(ItemUseAnimation.DRINK)
    .sound(SoundEvents.HONEY_DRINK)
    .hasConsumeParticles(false)
    .build()

  private fun effects(color: Int?, effects: List<Effect>) =
    PotionContents(Optional.empty(), Optional.ofNullable(color), effects.map(::effect), Optional.empty())

  fun effect(effect: Effect): MobEffectInstance {
    val type = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(effect.id)).orElseThrow()
    return MobEffectInstance(type, effect.seconds * 20, effect.amplifier)
  }

  /** A line of a tooltip: not italic unless asked, as names the game gives are not. */
  private fun line(key: String, color: ChatFormatting, italic: Boolean = false): Component =
    Component.translatable(key).withStyle { it.withColor(color).withItalic(italic) }

  private fun <T : Item> register(name: String, factory: (Item.Properties) -> T, properties: Item.Properties): T {
    val key = ResourceKey.create(Registries.ITEM, id(name))
    return Registry.register(BuiltInRegistries.ITEM, key, factory(properties.setId(key)))
  }

  fun id(path: String): Identifier = Identifier.fromNamespaceAndPath("redbull", path)
}
