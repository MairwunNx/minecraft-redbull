package ru.mairwunnx.redbull.fabric.gametest

import com.mojang.authlib.GameProfile
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
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.item.ItemEntity
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
import ru.mairwunnx.redbull.core.Bile
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.CRUMPLED_NUGGETS
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.fabric.Caffeines
import ru.mairwunnx.redbull.fabric.RedBullItems
import ru.mairwunnx.redbull.fabric.Settings
import ru.mairwunnx.redbull.fabric.Wings
import java.util.UUID

/** What a player does with Red Bull on a real server: drink, craft, brew, smelt, and find. */
class RedBullGameTests {
  private fun can(id: String) = RedBullItems.cans.getValue(id)

  private fun drink(player: ServerPlayer, item: Item): ItemStack {
    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(item))
    return player.getItemInHand(InteractionHand.MAIN_HAND).finishUsingItem(player.level(), player)
  }

  /**
   * A survival player on an embedded connection. The game's own mock player is always in creative, where nothing
   * is used up and caffeine can't kill; and the game protects a player whose client is still loading from all harm.
   */
  private fun player(helper: GameTestHelper): ServerPlayer {
    val server = helper.level.server
    val cookie = CommonListenerCookie.createInitial(GameProfile(UUID.randomUUID(), "test-player"), false)
    val player = ServerPlayer(server, helper.level, cookie.gameProfile(), cookie.clientInformation())
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
    equal(helper, player.getEffect(Wings.WINGS)?.duration, 8 * 20, "eight seconds of Wings")
    helper.assertTrue(player.deltaMovement.y > 0.5, "the launch")
    helper.assertFalse(BuiltInRegistries.ITEM.containsKey(RedBullItems.id("winged_can")), "no winged can left")
    helper.succeed()
  }

  /** A player in the air, as one who jumped off a cliff. */
  private fun airborne(helper: GameTestHelper): ServerPlayer = player(helper).apply {
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
    }
  }

  @GameTest
  fun wingsTakenAwayOnTheGroundLeaveNothing(helper: GameTestHelper) {
    val player = player(helper)
    player.addEffect(MobEffectInstance(Wings.WINGS, 10 * 20))
    player.removeEffect(Wings.WINGS)
    helper.runAfterDelay(2) {
      helper.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING), "Slow Falling without a flight")
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

  @GameTest
  fun oneCanTooManyKills(helper: GameTestHelper) {
    val player = player(helper)
    player.inventory.setItem(8, ItemStack(Items.TOTEM_OF_UNDYING))
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack(Items.TOTEM_OF_UNDYING))
    Caffeines.set(player, Caffeine(80.0, Caffeines.clock(player)))
    drink(player, can("red"))
    helper.succeedWhen { helper.assertTrue(player.isDeadOrDying, "the overdose, totem or not") }
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

  @GameTest
  fun everyCanWithARecipeCrafts(helper: GameTestHelper) {
    val level = helper.level
    val water = PotionContents.createItemStack(Items.POTION, Potions.WATER)
    for (can in CANS) {
      val recipe = can.recipe ?: continue
      val sugar = if (can.sugarfree) ItemStack.EMPTY else ItemStack(Items.SUGAR)
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

  /** The test runs wherever the server put it: cans of that biome turn up in its chests, and no other can does. */
  @GameTest
  fun chestsHoldCansOfTheirBiome(helper: GameTestHelper) {
    val level = helper.level
    val origin = helper.absoluteVec(Vec3.atCenterOf(BlockPos(1, 2, 1)))
    val biome = level.getBiome(BlockPos.containing(origin)).unwrapKey().orElseThrow().identifier().toString()
    val local = CANS.filter { biome in (it.chests?.biomes ?: emptyList()) }.map { it.id }.toSet()
    val key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon"))
    val table: LootTable = level.server.reloadableRegistries().getLootTable(key)
    val params = LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.CHEST)
    val found = (1..300).flatMap { table.getRandomItems(params) }.mapNotNull { RedBullItems.canOf(it)?.id }.toSet()
    equal(helper, found, local, "cans in 300 dungeon chests in $biome")
    helper.succeed()
  }
}
