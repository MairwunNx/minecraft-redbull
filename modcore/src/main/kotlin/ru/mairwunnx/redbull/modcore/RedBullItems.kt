package ru.mairwunnx.redbull.modcore

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
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
import net.minecraft.world.level.Level
import ru.mairwunnx.redbull.core.Bottles
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Can
import ru.mairwunnx.redbull.core.Colors
import ru.mairwunnx.redbull.core.Effect
import ru.mairwunnx.redbull.core.Ids
import java.util.Optional

/**
 * Every item is a real item with its own id, named after its asset: `redbull:original`, `redbull:crumpled_original`,
 * `redbull:empty_can`. Data components give each its purpose, as on Paper, but here they're the item's defaults: a can
 * is drunk, applies its effects, feeds when it has sugar, and leaves its crumpled can; the bottles keep the game's
 * potion model, tinted.
 */
object RedBullItems {
  /** Registered first: every can names its crumpled can as what the drink leaves. */
  private val crumpledItems: Map<String, () -> Item> = CANS.associate { it.id to register(it.crumpled) { p -> Item(crumpled(p, it)) } }
  private val canItems: Map<String, () -> CanItem> = CANS.associate { it.id to register(it.id) { p -> CanItem(can(p, it), it) } }

  private val emptyCanItem = register(Ids.EMPTY_CAN) { Item(it.hint(Ids.EMPTY_CAN).stacksTo(16)) }
  private val taurineItem = register(Ids.TAURINE) { Item(it.hint(Ids.TAURINE)) }
  private val bileItem = register(Ids.BULL_BILE) { BottleItem(bottle(it, Ids.BULL_BILE, Colors.BILE, Bottles.BILE), crude = false) }
  private val crudeTaurineItem =
    register(Ids.CRUDE_TAURINE) { BottleItem(bottle(it, Ids.CRUDE_TAURINE, Colors.CRUDE_TAURINE, Bottles.CRUDE_TAURINE), crude = true) }

  val cans: Map<String, CanItem> get() = canItems.mapValues { it.value() }
  val crumpled: Map<String, Item> get() = crumpledItems.mapValues { it.value() }
  val emptyCan: Item get() = emptyCanItem()
  val taurine: Item get() = taurineItem()
  val bile: BottleItem get() = bileItem()
  val crudeTaurine: BottleItem get() = crudeTaurineItem()

  /** Every item, in the order `/redbull give` suggests them. */
  val all: List<Item> get() = cans.values.toList() + listOf(emptyCan, taurine, bile, crudeTaurine) + crumpled.values

  /** `redbull:redbull` in the creative inventory: the cans, their crumpled cans, then the ingredients. */
  val TAB: ResourceKey<CreativeModeTab> = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("redbull"))

  /** What the tab lists, in order; each loader hands them to the game's output, which only loaders may name. */
  val shown: List<Item> get() = cans.values + crumpled.values + listOf(emptyCan, bile, crudeTaurine, taurine)

  /** The tab's title and icon, on the builder each loader makes with [shown] in it. */
  fun tab(builder: CreativeModeTab.Builder): CreativeModeTab = builder
    .title(Component.translatable("itemGroup.$MOD_ID"))
    .icon { ItemStack(can("original")) }
    .build()

  fun can(id: String): CanItem = canItems.getValue(id)()

  fun can(can: Can): ItemStack = ItemStack(can(can.id))

  fun canOf(stack: ItemStack): Can? = (stack.item as? CanItem)?.can

  /** The id of one of our items without its namespace, as `/redbull give` takes it: `original`, `bull_bile`. */
  fun idOf(item: Item): String? = BuiltInRegistries.ITEM.getKey(item).takeIf { it.namespace == MOD_ID }?.path

  fun id(stack: ItemStack): String? = stack.takeUnless { it.isEmpty }?.let { idOf(it.item) }

  /** One of our items by [idOf]'s id, or null for anything else. */
  fun create(id: String): ItemStack? =
    Identifier.tryBuild(MOD_ID, id)?.let { BuiltInRegistries.ITEM.getOptional(it).orElse(null) }?.let(::ItemStack)

  fun effect(effect: Effect): MobEffectInstance? =
    BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(effect.id)).map { MobEffectInstance(it, effect.seconds * 20, effect.amplifier) }
      .orElse(null)

  private fun <T : Item> register(name: String, make: (Item.Properties) -> T): () -> T = Mod.loader.item(name, make)

  private fun can(properties: Item.Properties, can: Can): Item.Properties {
    properties
      .stacksTo(1)
      // The chest-only legends get a yellow name, as a bottle o' enchanting does: things you can find, never make.
      .rarity(if (can.recipe == null) Rarity.UNCOMMON else Rarity.COMMON)
      .component(DataComponents.LORE, lore(line("${can.translation}.flavour"), line(can.slogan, ChatFormatting.GOLD, italic = true)))
      .component(DataComponents.POTION_CONTENTS, contents(null, can.effects))
      .component(DataComponents.CONSUMABLE, drink())
      .component(DataComponents.USE_REMAINDER, UseRemainder(ItemStackTemplate(crumpledItems.getValue(can.id)())))
    // Sugar feeds; the sugarfree cans don't.
    if (!can.sugarfree) properties.component(DataComponents.FOOD, FoodProperties(2, 1f, true))
    return properties
  }

  /** What a drink leaves: the can, crumpled, in its flavour's colours. It stacks, and a furnace melts it down. */
  private fun crumpled(properties: Item.Properties, can: Can) = properties
    .overrideDescription("item.$MOD_ID.${Ids.CRUMPLED_CAN}")
    .component(DataComponents.LORE, lore(line(can.translation), line("item.$MOD_ID.${Ids.CRUMPLED_CAN}.hint")))

  /** A bottle keeps the game's own potion model, tinted, and the glass bottle it leaves. */
  private fun bottle(properties: Item.Properties, id: String, color: Int, effects: List<Effect>) = properties
    .hint(id)
    .stacksTo(1)
    .component(DataComponents.POTION_CONTENTS, contents(color, effects))
    .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
    .usingConvertsTo(Items.GLASS_BOTTLE)

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

/** A can: its components drink it, apply its buffs, and leave the crumpled can; this adds caffeine and Wings. */
class CanItem(properties: Properties, val can: Can) : Item(properties) {
  override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
    val result = super.use(level, player, hand)
    if (result.consumesAction() && player is ServerPlayer) Drinks.raise(player)
    return result
  }

  override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
    val left = super.finishUsingItem(stack, level, entity)
    if (entity is ServerPlayer) {
      Collected.add(entity, setOf(can.id))
      Drinks.can(entity, can)
    }
    return left
  }

  override fun onCraftedBy(stack: ItemStack, player: Player) {
    super.onCraftedBy(stack, player)
    if (player is ServerPlayer) Collected.add(player, setOf(can.id))
  }
}

/** Bull bile or crude taurine: a tinted potion bottle that moos, and crude taurine adds a little caffeine. */
class BottleItem(properties: Properties, private val crude: Boolean) : Item(properties) {
  override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
    val left = super.finishUsingItem(stack, level, entity)
    if (entity is ServerPlayer) {
      if (crude) Drinks.crudeTaurine(entity) else Drinks.bile(entity)
    }
    return left
  }
}
