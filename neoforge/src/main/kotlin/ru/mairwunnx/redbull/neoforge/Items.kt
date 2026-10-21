package ru.mairwunnx.redbull.neoforge

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.Consumable
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.UseRemainder
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import ru.mairwunnx.redbull.core.Bottles
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Can
import ru.mairwunnx.redbull.core.Colors
import ru.mairwunnx.redbull.core.Effect
import ru.mairwunnx.redbull.core.Ids
import ru.mairwunnx.redbull.core.can
import java.util.Optional
import java.util.function.UnaryOperator
import net.minecraft.world.item.Items as Vanilla

/**
 * Every item, registered under the name its assets carry: `redbull:original`, `redbull:crumpled_original`,
 * `redbull:empty_can`, and so on. Components do what the Paper plugin builds onto potions: a can is drunk,
 * applies its effects, feeds when it has sugar, and leaves its crumpled can; the bottles keep the game's
 * potion model, tinted.
 */
object Items {
  private val ITEMS = DeferredRegister.createItems(MOD_ID)
  private val TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID)

  private fun simple(name: String, properties: (Item.Properties) -> Item.Properties): DeferredItem<Item> =
    ITEMS.registerSimpleItem(name, UnaryOperator { properties(it) })

  val crumpled: Map<String, DeferredItem<Item>> = CANS.associate { can ->
    can.id to simple(can.crumpled) {
      it.stacksTo(64)
        .overrideDescription("item.${MOD_ID}.${Ids.CRUMPLED_CAN}")
        .component(DataComponents.LORE, lore(line(can.translation), line("item.$MOD_ID.${Ids.CRUMPLED_CAN}.hint")))
    }
  }

  val cans: Map<String, DeferredItem<Item>> = CANS.associate { can ->
    can.id to simple(can.id) { canProperties(it, can) }
  }

  val emptyCan: DeferredItem<Item> = simple(Ids.EMPTY_CAN) { it.stacksTo(16).hint(Ids.EMPTY_CAN) }

  val taurine: DeferredItem<Item> = simple(Ids.TAURINE) { it.stacksTo(64).hint(Ids.TAURINE) }

  val bile: DeferredItem<Item> = simple(Ids.BULL_BILE) { bottle(it, Ids.BULL_BILE, Colors.BILE, Bottles.BILE) }

  val crudeTaurine: DeferredItem<Item> =
    simple(Ids.CRUDE_TAURINE) { bottle(it, Ids.CRUDE_TAURINE, Colors.CRUDE_TAURINE, Bottles.CRUDE_TAURINE) }

  /** Every item, in the order the collection and `/redbull give` list them. */
  val all: List<DeferredItem<Item>>
    get() = cans.values + listOf(emptyCan, taurine, bile, crudeTaurine) + crumpled.values

  /** `redbull:redbull` in the creative inventory: the cans, their crumpled cans, then the ingredients. */
  val tab: DeferredHolder<CreativeModeTab, CreativeModeTab> = TABS.register("redbull") { ->
    CreativeModeTab.builder()
      .title(Component.translatable("itemGroup.$MOD_ID"))
      .icon { ItemStack(cans.getValue("original").get()) }
      .displayItems { _, output ->
        (cans.values + crumpled.values + listOf(emptyCan, bile, crudeTaurine, taurine)).forEach { output.accept(it.get()) }
      }
      .build()
  }

  fun register(bus: IEventBus) {
    ITEMS.register(bus)
    TABS.register(bus)
  }

  /** The id of one of our items, like `original` or `bull_bile`; null for anything else. */
  fun id(stack: ItemStack): String? =
    stack.takeUnless { it.isEmpty }?.typeHolder()?.unwrapKey()?.orElse(null)?.identifier()
      ?.takeIf { it.namespace == MOD_ID }?.path

  fun canOf(stack: ItemStack): Can? = id(stack)?.let(::can)

  fun create(id: String): ItemStack? = all.firstOrNull { it.id.path == id }?.let { ItemStack(it.get()) }

  fun can(can: Can): ItemStack = ItemStack(cans.getValue(can.id).get())

  fun crumpled(can: Can): ItemStack = ItemStack(crumpled.getValue(can.id).get())

  fun effect(effect: Effect): MobEffectInstance? =
    BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(effect.id)).map { MobEffectInstance(it, effect.seconds * 20, effect.amplifier) }
      .orElse(null)

  private fun canProperties(properties: Item.Properties, can: Can): Item.Properties {
    properties
      .stacksTo(1)
      // The chest-only legends get a yellow name, as a bottle o' enchanting does: things you can find, never make.
      .rarity(if (can.recipe == null) Rarity.UNCOMMON else Rarity.COMMON)
      .component(DataComponents.POTION_CONTENTS, contents(null, can.effects))
      .component(DataComponents.CONSUMABLE, drink())
      .component(DataComponents.USE_REMAINDER, UseRemainder(ItemStackTemplate(crumpled.getValue(can.id).get())))
      .component(
        DataComponents.LORE,
        lore(line("${can.translation}.flavour"), line(can.slogan, ChatFormatting.GOLD, italic = true)),
      )
    if (!can.sugarfree) properties.component(DataComponents.FOOD, FoodProperties(2, 1f, true))
    return properties
  }

  /** A bottle keeps the game's potion model, tinted by its contents, and leaves a glass bottle. */
  private fun bottle(properties: Item.Properties, id: String, color: Int, effects: List<Effect>) = properties
    .stacksTo(1)
    .component(DataComponents.POTION_CONTENTS, contents(color, effects))
    .component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(1.6f).animation(ItemUseAnimation.DRINK)
      .sound(SoundEvents.GENERIC_DRINK).hasConsumeParticles(false).build())
    .usingConvertsTo(Vanilla.GLASS_BOTTLE)
    .hint(id)

  private fun drink() = Consumable.builder()
    .consumeSeconds(1.6f)
    .animation(ItemUseAnimation.DRINK)
    .sound(SoundEvents.HONEY_DRINK)
    .hasConsumeParticles(false)
    .build()

  private fun contents(color: Int?, effects: List<Effect>) =
    PotionContents(Optional.empty(), Optional.ofNullable(color), effects.mapNotNull(::effect), Optional.empty())

  private fun Item.Properties.hint(id: String) = component(DataComponents.LORE, lore(line("item.$MOD_ID.$id.hint")))

  private fun lore(vararg lines: Component) = ItemLore(lines.toList())

  /** A tooltip line: gray and upright unless told otherwise, as the game's own lines are. */
  private fun line(key: String, color: ChatFormatting = ChatFormatting.GRAY, italic: Boolean = false): Component =
    Component.translatable(key).withStyle { it.withColor(color).withItalic(italic) }
}
