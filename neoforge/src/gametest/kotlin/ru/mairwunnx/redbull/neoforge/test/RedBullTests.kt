package ru.mairwunnx.redbull.neoforge.test

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.AABB
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.registries.DeferredRegister
import ru.mairwunnx.redbull.core.Ids
import ru.mairwunnx.redbull.core.can
import ru.mairwunnx.redbull.neoforge.Caffeines
import ru.mairwunnx.redbull.neoforge.Items
import ru.mairwunnx.redbull.neoforge.Settings
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
    functions.register("bile") { -> Consumer(::bile) }
    functions.register(modBus)
  }

  /** A survival player in the test's level, with a connection that goes nowhere. */
  private class Survivor(helper: GameTestHelper) : ServerPlayer(
    helper.level.server, helper.level, GameProfile(UUID.randomUUID(), "redbull-test"),
    net.minecraft.server.level.ClientInformation.createDefault(),
  ) {
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
    val winged = player.mainHandItem
    helper.assertTrue(Items.id(winged) == Ids.WINGED_CAN, "the original leaves a winged can, not $winged")
    helper.assertTrue(winged.get(DataComponents.MAX_DAMAGE) == 9, "eight seconds of wings")
    helper.assertTrue(winged.has(DataComponents.GLIDER), "the winged can glides")

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
    player.drink(Items.can(can("cherry")!!))
    helper.assertTrue(Items.id(player.mainHandItem) == Ids.WINGED_CAN, "cherry has wings")
    // Its grace runs out now; the next sweep crumples it into a crumpled cherry can.
    player.mainHandItem.set(Items.EXPIRES.get(), 0L)
    helper.runAfterDelay(25) {
      helper.assertTrue(Items.id(player.mainHandItem) == "crumpled_cherry", "a spent winged can crumples, not ${player.mainHandItem}")
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
}
