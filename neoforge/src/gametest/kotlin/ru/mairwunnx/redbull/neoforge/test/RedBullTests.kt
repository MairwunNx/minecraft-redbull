package ru.mairwunnx.redbull.neoforge.test

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.world.InteractionHand
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.AABB
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.registries.DeferredRegister
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Ids
import ru.mairwunnx.redbull.core.can
import ru.mairwunnx.redbull.neoforge.Caffeines
import ru.mairwunnx.redbull.neoforge.Items
import ru.mairwunnx.redbull.neoforge.Settings
import ru.mairwunnx.redbull.neoforge.Wings
import java.util.UUID
import java.util.function.Consumer
import net.minecraft.world.item.Items as Vanilla

/**
 * Game tests for what a client would otherwise have to try by hand: drinking, the overdose, the bottles, milk,
 * wings, and bull bile. A dev-only mod next to Red Bull; `./gradlew :neoforge:runGameTestServer` runs them.
 */
@Mod("redbull_tests")
class RedBullTests(modBus: IEventBus) {
  init {
    val functions = DeferredRegister.create(Registries.TEST_FUNCTION, "redbull_tests")
    functions.register("drinks") { -> Consumer(::drinks) }
    functions.register("bottles") { -> Consumer(::bottles) }
    functions.register("wings") { -> Consumer(::wings) }
    functions.register("landing") { -> Consumer(::landing) }
    functions.register("bile") { -> Consumer(::bile) }
    functions.register("tab") { -> Consumer(::tab) }
    functions.register(modBus)
  }

  /** A survival player in the test's level, with a connection that goes nowhere. */
  private class Survivor(helper: GameTestHelper) : ServerPlayer(
    helper.level.server, helper.level, GameProfile(UUID.randomUUID(), "redbull-test"),
    net.minecraft.server.level.ClientInformation.createDefault(),
  ) {
    /** Up in the air above the test's floor, where a jump would spread an elytra. */
    fun fly(helper: GameTestHelper) {
      val at = helper.absoluteVec(net.minecraft.world.phys.Vec3(1.5, 10.0, 1.5))
      teleportTo(at.x, at.y, at.z)
      setOnGround(false)
    }

    /** Drinks [stack] to the end, as holding the use key would. */
    fun drink(stack: ItemStack) {
      setItemInHand(InteractionHand.MAIN_HAND, stack)
      startUsingItem(InteractionHand.MAIN_HAND)
      completeUsingItem()
    }

    companion object {
      fun join(helper: GameTestHelper): Survivor {
        val player = Survivor(helper)
        val cookie = CommonListenerCookie.createInitial(player.gameProfile, false)
        val connection = Connection(PacketFlow.SERVERBOUND)
        EmbeddedChannel(connection)
        helper.level.server.playerList.placeNewPlayer(connection, player, cookie)
        // As a real client's first frames would: survival, and no longer shielded as a player still loading.
        player.setGameMode(GameType.SURVIVAL)
        player.connection.markClientLoaded()
        val at = helper.absoluteVec(net.minecraft.world.phys.Vec3(1.5, 1.0, 1.5))
        player.teleportTo(at.x, at.y, at.z)
        return player
      }
    }
  }

  private fun level(player: ServerPlayer) = Caffeines.now(player).level

  private fun drinks(helper: GameTestHelper) {
    val player = Survivor.join(helper)
    player.drink(Items.can(can("original")!!))
    helper.assertTrue(level(player) == 30.0, "a can adds 30 caffeine, not ${level(player)}")
    helper.assertTrue(player.hasEffect(MobEffects.SPEED), "the original gives Speed")
    helper.assertTrue(Items.id(player.mainHandItem) == "crumpled_original", "a can with wings leaves a crumpled can, not ${player.mainHandItem}")
    helper.assertTrue(player.getEffect(Wings.EFFECT)?.duration == 8 * 20, "the original gives eight seconds of wings")
    helper.assertTrue(Items.create(Ids.WINGED_CAN) == null, "no winged can is left")
    helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("redbull", Ids.WINGED_CAN)), "no winged can is registered")

    player.drink(Items.can(can("sugarfree")!!))
    player.drink(Items.can(can("zero")!!))
    helper.assertTrue(level(player) == 90.0, "three cans make 90 caffeine, not ${level(player)}")
    helper.assertTrue(player.isAlive, "90 caffeine is survivable")
    player.drink(ItemStack(Vanilla.MILK_BUCKET))
    helper.assertTrue(level(player) == 70.0, "milk takes 20, leaving ${level(player)}")
    player.drink(Items.can(can("red")!!))
    player.drink(Items.can(can("red_sugarfree")!!))
    helper.runAfterDelay(3) {
      helper.assertTrue(player.isDeadOrDying, "130 caffeine stops the heart")
      helper.assertTrue(level(player) == 0.0, "death resets caffeine")
      helper.level.server.playerList.remove(player)
      helper.succeed()
    }
  }

  private fun bottles(helper: GameTestHelper) {
    val player = Survivor.join(helper)
    player.drink(ItemStack(Items.bile.get()))
    helper.assertTrue(player.hasEffect(MobEffects.NAUSEA), "bile makes the player sick")
    helper.assertTrue(player.mainHandItem.`is`(Vanilla.GLASS_BOTTLE), "bile leaves a glass bottle")
    player.drink(ItemStack(Items.crudeTaurine.get()))
    helper.assertTrue(level(player) == 10.0, "crude taurine adds 10 caffeine, not ${level(player)}")
    helper.assertTrue(player.getEffect(MobEffects.STRENGTH)?.amplifier == 1, "both bottles within a minute start bull mode")
    helper.level.server.playerList.remove(player)
    helper.succeed()
  }

  private fun wings(helper: GameTestHelper) {
    val player = Survivor.join(helper)
    player.fly(helper)
    helper.assertTrue(!player.tryToStartFallFlying(), "no gliding without an elytra or wings")
    player.drink(Items.can(can("cherry")!!))
    helper.assertTrue(player.hasEffect(Wings.EFFECT), "cherry has wings")
    player.fly(helper)
    helper.assertTrue(player.tryToStartFallFlying() && player.isFallFlying, "wings glide without an elytra")
    helper.assertTrue(player.inventory.none { it.isDamaged }, "gliding on wings wears nothing")
    helper.level.server.playerList.remove(player)
    helper.succeed()
  }

  /** Wings that end mid-glide leave Slow Falling. The fake player has no client to tick it, so the test does. */
  private fun landing(helper: GameTestHelper) {
    val player = Survivor.join(helper)
    player.fly(helper)
    player.forceAddEffect(MobEffectInstance(Wings.EFFECT, 3), null)
    helper.assertTrue(player.tryToStartFallFlying(), "wings glide")
    helper.onEachTick { if (!player.isRemoved) player.doTick() }
    helper.runAfterDelay(10) {
      helper.assertTrue(!player.hasEffect(Wings.EFFECT), "the wings are over")
      helper.assertTrue(player.getEffect(MobEffects.SLOW_FALLING)?.duration in 80..100, "wings that end mid-glide leave Slow Falling")
      helper.level.server.playerList.remove(player)
      helper.succeed()
    }
  }

  private fun bile(helper: GameTestHelper) {
    val player = Survivor.join(helper)
    val before = Settings.bile.get()
    // A hundred times 5% is a sure drop.
    Settings.bile.set(100.0)
    val cow = helper.spawn(EntityTypes.COW, BlockPos(1, 1, 1))
    cow.hurtServer(helper.level, player.damageSources().playerAttack(player), 1000f)
    Settings.bile.set(before)
    val area = AABB(cow.blockPosition()).inflate(3.0)
    val drops = helper.level.getEntitiesOfClass(ItemEntity::class.java, area).map { Items.id(it.item) }
    helper.assertTrue(Ids.BULL_BILE in drops, "a cow a player killed drops bile, dropped $drops")
    helper.level.server.playerList.remove(player)
    helper.succeed()
  }

  private fun tab(helper: GameTestHelper) {
    val tab = Items.tab.get()
    tab.buildContents(CreativeModeTab.ItemDisplayParameters(helper.level.enabledFeatures(), false, helper.level.registryAccess()))
    val shown = tab.displayItems.map { Items.id(it) }
    val expected = CANS.map { it.id } + CANS.map { it.crumpled } + listOf(Ids.EMPTY_CAN, Ids.BULL_BILE, Ids.CRUDE_TAURINE, Ids.TAURINE)
    helper.assertTrue(shown == expected, "the creative tab lists every item in order, not $shown")
    helper.assertTrue(Items.id(tab.iconItem) == "original", "the tab shows the original")
    helper.succeed()
  }
}
