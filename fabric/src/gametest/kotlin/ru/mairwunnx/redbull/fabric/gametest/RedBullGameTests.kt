package ru.mairwunnx.redbull.fabric.gametest

import com.mojang.authlib.GameProfile
import com.mojang.datafixers.util.Either
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.util.Unit
import net.minecraft.world.attribute.BedRule
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.AbstractBedBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BedPart
import io.netty.channel.embedded.EmbeddedChannel
import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.network.Connection
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.server.players.NameAndId
import net.minecraft.server.permissions.LevelBasedPermissionSet
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.crafting.BrewingInput
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SingleRecipeInput
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.GameType
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import ru.mairwunnx.redbull.core.WINGS_COMMON
import ru.mairwunnx.redbull.core.Bile
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.CRUMPLED_NUGGETS
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.core.Trades
import ru.mairwunnx.redbull.fabric.Caffeines
import ru.mairwunnx.redbull.fabric.Collected
import ru.mairwunnx.redbull.fabric.Menu
import ru.mairwunnx.redbull.fabric.RedBullItems
import ru.mairwunnx.redbull.fabric.Settings
import ru.mairwunnx.redbull.fabric.Wings
import java.util.Optional
import java.util.UUID

/** What a player does with Red Bull on a real server: drink, craft, brew, smelt, and find. */
class RedBullGameTests {
  private fun can(id: String) = RedBullItems.cans.getValue(id)

  private fun drink(player: ServerPlayer, item: Item): ItemStack {
    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(item))
    return player.getItemInHand(InteractionHand.MAIN_HAND).finishUsingItem(player.level(), player)
  }

  /** A player who remembers the keys of the action bar's messages, as the client would have shown them. */
  private class Survivor(server: net.minecraft.server.MinecraftServer, level: net.minecraft.server.level.ServerLevel, cookie: CommonListenerCookie) :
    ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation()) {
    val overlays = mutableListOf<String?>()

    /** How many times the glide sent out its vibration. */
    var glides = 0

    override fun gameEvent(event: net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent>) {
      if (event.`is`(net.minecraft.world.level.gameevent.GameEvent.ELYTRA_GLIDE)) glides++
      super.gameEvent(event)
    }

    override fun sendOverlayMessage(message: Component) {
      overlays += (message.contents as? TranslatableContents)?.key
      super.sendOverlayMessage(message)
    }
  }

  /**
   * A survival player on an embedded connection, with a name of their own for commands. The game's own mock player is always in creative, where nothing
   * is used up and caffeine can't kill; and the game protects a player whose client is still loading from all harm.
   */
  private fun player(helper: GameTestHelper): Survivor {
    val server = helper.level.server
    val cookie = CommonListenerCookie.createInitial(UUID.randomUUID().let { GameProfile(it, "rb-${it.toString().take(8)}") }, false)
    val player = Survivor(server, helper.level, cookie)
    val connection = Connection(PacketFlow.SERVERBOUND)
    EmbeddedChannel(connection)
    server.playerList.placeNewPlayer(connection, player, cookie)
    player.setGameMode(GameType.SURVIVAL)
    player.connection.handleAcceptPlayerLoad(ServerboundPlayerLoadedPacket())
    return player
  }

  private fun caffeine(player: ServerPlayer) = Caffeines.now(player).level

  private fun item(id: String): Item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id))

  private fun equal(helper: GameTestHelper, actual: Any?, expected: Any?, what: String) =
    helper.assertTrue(actual == expected, "$what: expected $expected, got $actual")

  @GameTest
  fun canAddsCaffeineBuffsAndCrumples(helper: GameTestHelper) {
    val player = player(helper)
    val left = drink(player, can("sea_blue"))
    equal(helper, left.item, RedBullItems.crumpled.getValue("sea_blue"), "what a can without wings leaves")
    equal(helper, caffeine(player), Caffeines.rules.perCan, "caffeine after a can")
    helper.assertTrue(player.hasEffect(MobEffects.DOLPHINS_GRACE), "the can's buff")
    helper.succeed()
  }

  @GameTest
  fun wingsLaunchAndLeaveTheCrumpledCan(helper: GameTestHelper) {
    val player = player(helper)
    val left = drink(player, can("original"))
    equal(helper, left.item, RedBullItems.crumpled.getValue("original"), "what a can with wings leaves")
    equal(helper, player.getEffect(Wings.WINGS)?.duration, WINGS_COMMON * 20, "sixteen seconds of Wings")
    helper.assertTrue(player.deltaMovement.y > 0.5, "the launch")
    helper.assertFalse(BuiltInRegistries.ITEM.containsKey(RedBullItems.id("winged_can")), "no winged can left")
    helper.succeed()
  }

  /** A player in the air, as one who jumped off a cliff. */
  private fun airborne(helper: GameTestHelper): Survivor = player(helper).apply {
    val above = helper.absoluteVec(Vec3(1.5, 40.0, 1.5))
    teleportTo(above.x, above.y, above.z)
    setOnGround(false)
  }

  @GameTest
  fun wingsLetPlayersGlide(helper: GameTestHelper) {
    val player = airborne(helper)
    helper.assertFalse(player.tryToStartFallFlying(), "gliding without an elytra or Wings")
    player.addEffect(MobEffectInstance(Wings.WINGS, 10 * 20))
    helper.assertTrue(player.tryToStartFallFlying(), "gliding on Wings")
    helper.assertTrue(player.isFallFlying, "in flight")
    helper.succeed()
  }

  /** Sculk hears a glide on Wings alone every half a second, as it hears an elytra. */
  @GameTest(maxTicks = 100)
  fun glidingOnWingsAloneSendsVibrations(helper: GameTestHelper) {
    val player = airborne(helper)
    player.addEffect(MobEffectInstance(Wings.WINGS, 10 * 20))
    helper.assertTrue(player.tryToStartFallFlying(), "gliding")
    helper.onEachTick {
      player.setOnGround(false)
      player.doTick()
    }
    helper.succeedWhen {
      helper.assertTrue(player.fallFlyingTicks >= 60, "three seconds of flight")
      equal(helper, player.glides, player.fallFlyingTicks / 10, "the glide's vibrations after ${player.fallFlyingTicks} ticks")
    }
  }

  /** The game wears a glider every second of flight; Wings take an elytra's place, so it keeps every point. */
  @GameTest(maxTicks = 100)
  fun glidingOnWingsWearsNothing(helper: GameTestHelper) {
    val player = airborne(helper)
    player.setItemSlot(EquipmentSlot.CHEST, ItemStack(Items.ELYTRA))
    player.addEffect(MobEffectInstance(Wings.WINGS, 10 * 20))
    helper.assertTrue(player.tryToStartFallFlying(), "gliding")
    var ticks = 0
    helper.onEachTick {
      player.setOnGround(false)
      player.doTick()
      ticks++
    }
    helper.succeedWhen {
      helper.assertTrue(ticks >= 60, "three seconds of flight")
      helper.assertTrue(player.isFallFlying, "still gliding after $ticks ticks")
      equal(helper, player.getItemBySlot(EquipmentSlot.CHEST).damageValue, 0, "the elytra's wear")
      // The game sends it every half a second of flight, as with an elytra alone.
      equal(helper, player.glides, player.fallFlyingTicks / 10, "the glide's vibrations after ${player.fallFlyingTicks} ticks")
    }
  }

  @GameTest(maxTicks = 100)
  fun wingsRunningOutMidFlightSlowTheFall(helper: GameTestHelper) {
    val player = airborne(helper)
    player.addEffect(MobEffectInstance(Wings.WINGS, 10))
    helper.assertTrue(player.tryToStartFallFlying(), "gliding")
    helper.onEachTick {
      player.setOnGround(false)
      player.doTick()
    }
    helper.succeedWhen {
      helper.assertFalse(player.hasEffect(Wings.WINGS), "Wings ran out")
      equal(helper, player.getEffect(MobEffects.SLOW_FALLING)?.let { it.duration > 80 }, true, "five seconds of Slow Falling")
      helper.assertTrue("redbull.wings.gone" in player.overlays, "what Wings say when they run out: ${player.overlays}")
    }
  }

  @GameTest
  fun wingsTakenAwayOnTheGroundLeaveNothing(helper: GameTestHelper) {
    val player = player(helper)
    player.addEffect(MobEffectInstance(Wings.WINGS, 10 * 20))
    player.removeEffect(Wings.WINGS)
    helper.runAfterDelay(2) {
      helper.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING), "Slow Falling without a flight")
      helper.assertFalse("redbull.wings.gone" in player.overlays, "a message for Wings taken on the ground")
      helper.succeed()
    }
  }

  @GameTest
  fun wingsTakenAwayMidFlightSayGoodbye(helper: GameTestHelper) {
    val player = airborne(helper)
    player.addEffect(MobEffectInstance(Wings.WINGS, 10 * 20))
    helper.assertTrue(player.tryToStartFallFlying(), "gliding")
    drink(player, Items.MILK_BUCKET)
    helper.runAfterDelay(2) {
      helper.assertTrue("redbull.wings.gone" in player.overlays, "what Wings taken mid-flight say: ${player.overlays}")
      helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "Slow Falling after Wings taken mid-flight")
      helper.succeed()
    }
  }

  @GameTest
  fun milkTakesCaffeineAway(helper: GameTestHelper) {
    val player = player(helper)
    Caffeines.set(player, Caffeine(50.0, Caffeines.clock(player)))
    drink(player, Items.MILK_BUCKET)
    equal(helper, caffeine(player), 50.0 - Caffeines.rules.milk, "caffeine after milk")
    helper.succeed()
  }

  /** The Caffeine effect follows the cans: a level per stage, and as long as the caffeine takes to wear off. */
  @GameTest
  fun caffeineShowsAsAnEffect(helper: GameTestHelper) {
    val player = player(helper)
    // 30 points a can, a point every 6 seconds: 3 minutes a can.
    listOf("original", "sugarfree", "zero").forEachIndexed { index, id ->
      drink(player, can(id))
      val shown = player.getEffect(Caffeines.EFFECT)
      equal(helper, shown?.amplifier, index, "the level after ${index + 1} cans")
      equal(helper, shown?.duration, (index + 1) * 3 * 60 * 20, "the duration after ${index + 1} cans")
      helper.assertTrue(shown?.isVisible == false && shown.showIcon(), "an icon and no particles")
    }
    Caffeines.reset(player)
    helper.assertFalse(player.hasEffect(Caffeines.EFFECT), "the effect without caffeine")
    helper.succeed()
  }

  /** Milk takes 20 caffeine and every effect; the effect comes back for what's left, and after `/effect clear` too. */
  @GameTest(maxTicks = 100)
  fun caffeineComesBackAfterMilk(helper: GameTestHelper) {
    val player = player(helper)
    drink(player, can("original"))
    drink(player, Items.MILK_BUCKET)
    equal(helper, caffeine(player), 10.0, "caffeine after milk")
    val shown = player.getEffect(Caffeines.EFFECT)
    helper.assertTrue(shown?.amplifier == 0 && shown.duration == 60 * 20, "a minute of Caffeine right after milk, not $shown")
    player.removeAllEffects()
    helper.assertFalse(player.hasEffect(Caffeines.EFFECT), "Caffeine after /effect clear")
    // The heart beats every two seconds and puts the effect back.
    helper.succeedWhen { equal(helper, player.getEffect(Caffeines.EFFECT)?.amplifier, 0, "Caffeine after the next beat") }
  }

  /**
   * Lies [player] down in a red bed next to them. The bed's own rule would wait for the night; this one lets them
   * sleep at any hour, so only caffeine stands in the way.
   */
  private fun sleep(helper: GameTestHelper, player: ServerPlayer): Either<Player.BedSleepingProblem, Unit> {
    val head = BlockPos(1, 1, 3)
    val bed = Blocks.BED.red().defaultBlockState().setValue(AbstractBedBlock.FACING, Direction.NORTH)
    helper.setBlock(head.south(), bed.setValue(AbstractBedBlock.PART, BedPart.FOOT))
    helper.setBlock(head, bed.setValue(AbstractBedBlock.PART, BedPart.HEAD))
    val anytime = BedRule(BedRule.Rule.ALWAYS, BedRule.Rule.ALWAYS, false, false, Optional.empty())
    return player.startSleepInBed(Blocks.BED.red() as AbstractBedBlock, helper.getBlockState(head), anytime, helper.absolutePos(head))
  }

  private fun key(component: Component?) = (component?.contents as? TranslatableContents)?.key

  /** Caffeine keeps a player awake; without it the same bed takes them. */
  @GameTest
  fun caffeineKeepsPlayersOutOfBed(helper: GameTestHelper) {
    val player = player(helper)
    helper.absoluteVec(Vec3(1.5, 1.0, 1.5)).let { player.teleportTo(it.x, it.y, it.z) }
    drink(player, can("sea_blue"))
    val refused = sleep(helper, player)
    equal(helper, key(refused.left().orElse(null)?.message), "redbull.sleepless", "what the bed says")
    equal(helper, refused.left().orElse(null)?.message?.style?.color,
      net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.YELLOW), "its colour")
    helper.assertFalse(player.isSleeping, "asleep with caffeine")
    Caffeines.reset(player)
    val allowed = sleep(helper, player)
    helper.assertTrue(allowed.right().isPresent && player.isSleeping, "without caffeine the bed takes them, not $allowed")
    player.stopSleeping()
    helper.succeed()
  }

  @GameTest
  fun oneCanTooManyKills(helper: GameTestHelper) {
    val player = player(helper)
    player.inventory.setItem(8, ItemStack(Items.TOTEM_OF_UNDYING))
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack(Items.TOTEM_OF_UNDYING))
    Caffeines.set(player, Caffeine(80.0, Caffeines.clock(player)))
    drink(player, can("red"))
    helper.succeedWhen { helper.assertTrue(player.isDeadOrDying, "the overdose, totem or not") }
  }

  /** Hurt shortly before, a player dies by the game's `.player` message, which needs its own text. */
  @GameTest
  fun anOverdoseInAFightHasItsMessage(helper: GameTestHelper) {
    val player = player(helper)
    val zombie = helper.spawn(EntityTypes.ZOMBIE, BlockPos(1, 2, 1))
    player.hurtServer(helper.level, helper.level.damageSources().mobAttack(zombie), 1f)
    val overdose = helper.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
      .getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, RedBullItems.id("overdose")))
    val message = key(net.minecraft.world.damagesource.DamageSource(overdose).getLocalizedDeathMessage(player))
    equal(helper, message, "death.attack.redbull.overdose.player", "the death message's key")
    val english = RedBullGameTests::class.java.getResource("/assets/redbull/lang/en_us.json")!!.readText()
    helper.assertTrue("\"$message\":" in english, "$message has a text")
    helper.succeed()
  }

  /** The switches of `config/redbull.toml`; the test puts the defaults back before anything else drinks. */
  @GameTest
  fun settingsSwitchTheOverdoseOff(helper: GameTestHelper) {
    try {
      Settings.read("overdose = false # nobody dies\nwings = off\n\n[loot]\nbile = 2.5\nchests = 500\n")
      equal(helper, Settings.wings, true, "a switch set to nonsense")
      equal(helper, Settings.bile, 2.5, "loot.bile")
      equal(helper, Settings.chests, 1.0, "loot.chests out of range")
      val player = player(helper)
      Caffeines.set(player, Caffeine(80.0, Caffeines.clock(player)))
      drink(player, can("red"))
      equal(helper, caffeine(player), Caffeines.rules.limit, "caffeine past the limit without the overdose")
      Settings.read("wings = false")
      val other = player(helper)
      drink(other, can("original"))
      helper.assertFalse(other.hasEffect(Wings.WINGS) || other.deltaMovement.y > 0.5, "wings switched off")
    } finally {
      Settings.read("")
    }
    equal(helper, Settings.overdose, true, "the default")
    helper.succeed()
  }

  @GameTest
  fun bottlesStartBullMode(helper: GameTestHelper) {
    val player = player(helper)
    drink(player, RedBullItems.bile)
    val left = drink(player, RedBullItems.crudeTaurine)
    equal(helper, left.item, Items.GLASS_BOTTLE, "what a bottle leaves")
    equal(helper, caffeine(player), Caffeines.rules.perCrudeTaurine, "caffeine after crude taurine")
    equal(helper, player.getEffect(MobEffects.STRENGTH)?.amplifier, 1, "bull mode's Strength II")
    helper.succeed()
  }

  /** Every wandering trader brings one or two different cans besides its own wares, never a legend, at core's prices. */
  @GameTest
  fun wanderingTradersSellCans(helper: GameTestHelper) {
    repeat(20) {
      val trader = helper.spawn(EntityTypes.WANDERING_TRADER, BlockPos(1, 2, 1))
      val offers = trader.offers
      val cans = offers.filter { RedBullItems.canOf(it.result) != null }
      val sold = cans.map { RedBullItems.canOf(it.result)!! }
      helper.assertTrue(cans.size in Trades.MIN_OFFERS..Trades.MAX_OFFERS, "one or two cans, not ${sold.map { it.id }}")
      helper.assertTrue(offers.size > cans.size, "the trader's own wares stay")
      equal(helper, sold.distinct(), sold, "different cans")
      for ((offer, can) in cans.zip(sold)) {
        helper.assertTrue(can.recipe != null, "${can.id} is a legend")
        helper.assertTrue(offer.costA.`is`(Items.EMERALD) && offer.costA.count == Trades.price(can), "${can.id} for ${offer.costA}")
        helper.assertTrue(offer.costB.isEmpty && offer.result.count == 1, "one ${can.id} for emeralds alone")
        equal(helper, offer.maxUses to offer.xp, Trades.MAX_USES to 1, "the uses and experience of ${can.id}")
      }
      trader.discard()
    }
    helper.succeed()
  }

  @GameTest
  fun everyCanWithARecipeCrafts(helper: GameTestHelper) {
    val level = helper.level
    val water = PotionContents.createItemStack(Items.POTION, Potions.WATER)
    for (can in CANS) {
      val recipe = can.recipe ?: continue
      val sugar = ItemStack(if (can.sugarfree) Items.HONEYCOMB else Items.SUGAR)
      val grid = listOf(
        ItemStack(RedBullItems.taurine), ItemStack(item(recipe.dye)), ItemStack(RedBullItems.taurine),
        ItemStack(item(recipe.flavour)), ItemStack(RedBullItems.emptyCan), ItemStack(item(recipe.flavour)),
        sugar, water.copy(), sugar.copy(),
      )
      val result = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), level)
        .map { it.value().assemble(CraftingInput.of(3, 3, grid)) }
      equal(helper, result.map { it.item }.orElse(null), can(can.id), "the recipe of ${can.id}")
    }
    val awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD)
    val notWater = CraftingInput.of(3, 3, listOf(
      ItemStack(RedBullItems.taurine), ItemStack(item("minecraft:blue_dye")), ItemStack(RedBullItems.taurine),
      ItemStack(Items.SUGAR), ItemStack(RedBullItems.emptyCan), ItemStack(Items.SUGAR),
      ItemStack(Items.SUGAR), awkward, ItemStack(Items.SUGAR),
    ))
    helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, notWater, level).isEmpty, "only water fills a can")
    helper.succeed()
  }

  @GameTest
  fun bileBrewsAndSmeltsIntoTaurine(helper: GameTestHelper) {
    val level = helper.level
    val recipes = level.recipeAccess()
    val water = PotionContents.createItemStack(Items.POTION, Potions.WATER)
    val brewed = recipes.getRecipeFor(RecipeType.BREWING, BrewingInput(water, ItemStack(RedBullItems.bile)), level)
      .map { it.value().assemble(BrewingInput(water, ItemStack(RedBullItems.bile))) }
    equal(helper, brewed.map { it.item }.orElse(null), RedBullItems.crudeTaurine, "bile in a water bottle")
    helper.assertTrue(brewed.get().has(DataComponents.POTION_CONTENTS), "crude taurine keeps its own effects")
    equal(helper, 
      brewed.get().get(DataComponents.POTION_CONTENTS)?.customEffects()?.size, 3, "crude taurine's three effects",
    )

    val smelted = recipes.getRecipeFor(RecipeType.SMELTING, SingleRecipeInput(ItemStack(RedBullItems.crudeTaurine)), level)
    equal(helper, smelted.map { it.value().assemble(SingleRecipeInput(ItemStack(RedBullItems.crudeTaurine))).item }.orElse(null), RedBullItems.taurine, "evaporated crude taurine")
    val crumpled = SingleRecipeInput(ItemStack(RedBullItems.crumpled.getValue("zero")))
    for (type in listOf(RecipeType.SMELTING, RecipeType.BLASTING)) {
      val nuggets = recipes.getRecipeFor(type, crumpled, level).map { it.value().assemble(crumpled) }.orElse(ItemStack.EMPTY)
      helper.assertTrue(nuggets.`is`(Items.IRON_NUGGET) && nuggets.count == CRUMPLED_NUGGETS, "a crumpled can melted in $type")
    }
    helper.succeed()
  }

  @GameTest(maxTicks = 200)
  fun cowsKilledByPlayersDropBile(helper: GameTestHelper) {
    val player = player(helper)
    val looting = helper.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING)
    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.NETHERITE_SWORD).apply { enchant(looting, 3) })
    // 11% each with Looting III: 150 cows miss every time once in forty million runs.
    repeat(150) {
      val cow = helper.spawn(EntityTypes.COW, BlockPos(1, 2, 1))
      cow.hurtServer(helper.level, helper.level.damageSources().playerAttack(player), 1000f)
    }
    helper.succeedWhen {
      val area = AABB(helper.absolutePos(BlockPos(1, 2, 1))).inflate(8.0)
      val drops = helper.level.getEntitiesOfClass(ItemEntity::class.java, area) { it.item.`is`(RedBullItems.bile) }
      helper.assertTrue(drops.isNotEmpty(), "bull bile at ${Bile.chance(3)} a cow")
    }
  }

  @GameTest
  fun cansJoinTheCollectionAsTheyArrive(helper: GameTestHelper) {
    val player = player(helper)
    drink(player, can("sea_blue"))
    ItemStack(can("cherry")).onCraftedBy(player, 1)
    val dropped = ItemEntity(helper.level, player.x, player.y, player.z, ItemStack(can("summer")))
    dropped.setNoPickUpDelay()
    helper.level.addFreshEntity(dropped)
    dropped.playerTouch(player)
    player.inventory.setItem(20, ItemStack(can("zero")))
    player.doCloseContainer()
    equal(helper, Collected.of(player), setOf("sea_blue", "cherry", "summer", "zero"), "drunk, crafted, picked up, and taken")

    // Death keeps it: the respawned player is a new entity that copies the attachment.
    val respawned = helper.level.server.playerList.respawn(player, false, Entity.RemovalReason.KILLED)
    equal(helper, Collected.of(respawned), Collected.of(player), "the collection after death")
    helper.succeed()
  }

  @GameTest
  fun collectionShowsWhatThePlayerHeld(helper: GameTestHelper) {
    val player = player(helper)
    Collected.add(player, setOf("cherry"))
    Menu.open(player)
    val menu = player.containerMenu as? Menu.View ?: throw helper.assertionException("no collection menu open")
    val cherry = CANS.indexOfFirst { it.id == "cherry" }
    equal(helper, menu.getSlot(cherry).item.item, can("cherry"), "a can the player held")
    val other = menu.getSlot(if (cherry == 0) 1 else 0).item
    equal(helper, other.item, RedBullItems.emptyCan, "a can they haven't")
    equal(helper, other.hoverName.string, "???", "its name")
    helper.assertTrue(menu.getSlot(27).item.isEmpty, "no ingredients for players")

    menu.clicked(cherry, 0, ContainerInput.PICKUP, player)
    menu.clicked(cherry, 0, ContainerInput.QUICK_MOVE, player)
    helper.assertTrue(player.inventory.isEmpty && menu.carried.isEmpty, "nothing to take for players")
    equal(helper, menu.getSlot(cherry).item.item, can("cherry"), "the can stays")
    helper.succeed()
  }

  @GameTest
  fun adminsTakeFromTheMenu(helper: GameTestHelper) {
    val player = player(helper)
    helper.level.server.playerList.op(NameAndId(player.gameProfile), Optional.of(LevelBasedPermissionSet.GAMEMASTER), Optional.of(false))
    helper.assertTrue(Menu.admin(player), "an operator is an admin")
    Menu.open(player)
    val menu = player.containerMenu as? Menu.View ?: throw helper.assertionException("no admin menu open")
    val cherry = CANS.indexOfFirst { it.id == "cherry" }
    menu.clicked(cherry, 0, ContainerInput.PICKUP, player)
    val extras = (27..31).map { RedBullItems.idOf(menu.getSlot(it).item.item) }
    equal(helper, extras, listOf("empty_can", "crumpled_original", "bull_bile", "crude_taurine", "taurine"), "the ingredients' row")
    menu.clicked(31, 0, ContainerInput.QUICK_MOVE, player)
    equal(helper, player.inventory.countItem(can("cherry")), 1, "one can with a click")
    equal(helper, player.inventory.countItem(RedBullItems.taurine), 64, "a stack of taurine with Shift")
    helper.assertTrue(menu.carried.isEmpty, "nothing on the cursor")
    helper.assertTrue("cherry" in Collected.of(player), "a taken can joins the collection")
    helper.level.server.playerList.deop(NameAndId(player.gameProfile))
    helper.succeed()
  }

  @GameTest
  fun commandsGiveAndShowCaffeine(helper: GameTestHelper) {
    val player = player(helper)
    val server = helper.level.server
    val dispatcher = server.commands.dispatcher
    val console = server.createCommandSourceStack().withSuppressedOutput()
    equal(helper, dispatcher.execute("redbull give cherry ${player.plainTextName} 3", console), 3, "the cans given")
    equal(helper, player.inventory.countItem(can("cherry")), 3, "cherry cans in the inventory")
    helper.assertTrue("cherry" in Collected.of(player), "a given can joins the collection")
    equal(helper, dispatcher.execute("redbull give winged_can ${player.plainTextName}", console), 0, "the winged can is gone")

    Caffeines.set(player, Caffeine(42.5, Caffeines.clock(player)))
    equal(helper, dispatcher.execute("redbull caffeine ${player.plainTextName}", console), 42, "caffeine")

    val own = player.createCommandSourceStack().withSuppressedOutput()
    val refused = runCatching { dispatcher.execute("redbull give cherry", own) }.isFailure
    helper.assertTrue(refused, "a player without operator rights gives nothing")
    equal(helper, dispatcher.execute("redbull", own), 1, "anyone opens the collection")
    helper.assertTrue(player.containerMenu is Menu.View, "the collection is open")
    helper.succeed()
  }

  @GameTest
  fun theCreativeTabListsEveryItem(helper: GameTestHelper) {
    val tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(RedBullItems.TAB)
    tab.buildContents(CreativeModeTab.ItemDisplayParameters(helper.level.enabledFeatures(), false, helper.level.registryAccess()))
    val shown = tab.displayItems.map { RedBullItems.idOf(it.item) }
    val expected = CANS.map { it.id } + CANS.map { it.crumpled } + listOf("empty_can", "bull_bile", "crude_taurine", "taurine")
    equal(helper, shown, expected, "the creative tab")
    equal(helper, RedBullItems.idOf(tab.iconItem.item), "original", "the tab's icon")
    helper.succeed()
  }

  /** The test runs wherever the server put it: cans of that biome turn up in its chests, and no other can does. */
  @GameTest
  fun chestsHoldCansOfTheirBiome(helper: GameTestHelper) {
    val level = helper.level
    val origin = helper.absoluteVec(Vec3.atCenterOf(BlockPos(1, 2, 1)))
    val biome = level.getBiome(BlockPos.containing(origin)).unwrapKey().orElseThrow().identifier().toString()
    val local = CANS.filter { biome in (it.chests?.biomes ?: emptyList()) }.map { it.id }.toSet()
    val params = LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.CHEST)
    // A dungeon's chest, and a crate of another mod's: cans turn up in any chest, as on Paper and NeoForge.
    for (id in listOf(Identifier.withDefaultNamespace("chests/simple_dungeon"), Identifier.parse("redbull-gametest:chests/crate"))) {
      val table: LootTable = level.server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id))
      val found = (1..300).flatMap { table.getRandomItems(params) }.mapNotNull { RedBullItems.canOf(it)?.id }.toSet()
      equal(helper, found, local, "cans in 300 chests of $id in $biome")
    }
    helper.succeed()
  }
}
