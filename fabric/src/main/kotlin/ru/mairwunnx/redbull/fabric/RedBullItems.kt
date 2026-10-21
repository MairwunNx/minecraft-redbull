package ru.mairwunnx.redbull.fabric

import com.mojang.serialization.Codec
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.minecraft.ChatFormatting
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Unit
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EquipmentSlot
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
import net.minecraft.world.item.equipment.Equippable
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
  /** When an unflown winged can crumples, in epoch milliseconds. */
  val EXPIRES: DataComponentType<Long> = component("expires", Codec.LONG, ByteBufCodecs.VAR_LONG)

  /** The can a winged can was before the drink, to crumple into. */
  val FLAVOUR: DataComponentType<String> = component("flavour", Codec.STRING, ByteBufCodecs.STRING_UTF8)

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

  /**
   * The can after a drink with wings: a glider held in the main hand. The game wears a glider by a point per second
   * of flight and stops a point before it breaks, so its durability is the wing seconds plus one; see [WingedCanItem].
   */
  val wingedCan: WingedCanItem = register(
    Ids.WINGED_CAN, ::WingedCanItem,
    hint(Ids.WINGED_CAN)
      .durability(CANS.first().wings + 1)
      .component(DataComponents.GLIDER, Unit.INSTANCE)
      .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.MAINHAND).setSwappable(false).build()),
  )

  /** Every item, in the order the creative tab lists them. */
  val all: List<Item> get() =
    cans.values.toList() + listOf(emptyCan, taurine, bile, crudeTaurine, wingedCan) + crumpled.values

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

  /** A winged can of [can]'s flavour: [Can.wings] seconds of flight, and crumpling at [expires] if unflown. */
  fun wingedCan(can: Can, expires: Long): ItemStack = ItemStack(wingedCan).apply {
    set(DataComponents.MAX_DAMAGE, can.wings + 1)
    set(DataComponents.DAMAGE, 0)
    set(EXPIRES, expires)
    set(FLAVOUR, can.id)
  }

  fun crumpledOf(stack: ItemStack): ItemStack =
    ItemStack(crumpled[stack.get(FLAVOUR)] ?: crumpled.values.first())

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

  private fun <T : Any> component(name: String, codec: Codec<T>, stream: net.minecraft.network.codec.StreamCodec<in net.minecraft.network.RegistryFriendlyByteBuf, T>) =
    Registry.register(
      BuiltInRegistries.DATA_COMPONENT_TYPE, id(name),
      DataComponentType.builder<T>().persistent(codec).networkSynchronized(stream).build(),
    )

  fun id(path: String): Identifier = Identifier.fromNamespaceAndPath("redbull", path)
}
