package ru.mairwunnx.redbull.neoforge.test

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
import net.minecraft.world.entity.EquipmentSlot
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
import java.util.Optional
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
    functions.register("caffeine") { -> Consumer(::caffeine) }
    functions.register("milk") { -> Consumer(::milk) }
    functions.register("bed") { -> Consumer(::bed) }
    functions.register("bottles") { -> Consumer(::bottles) }
    functions.register("wings") { -> Consumer(::wings) }
    functions.register("landing") { -> Consumer(::landing) }
    functions.register("elytra") { -> Consumer(::elytra) }
    functions.register("wings_gone") { -> Consumer(::wingsGone) }
    functions.register("bile") { -> Consumer(::bile) }
    functions.register("tab") { -> Consumer(::tab) }
    functions.register(modBus)
  }

  /** A survival player in the test's level, with a connection that goes nowhere. */
  private class Survivor(helper: GameTestHelper) : ServerPlayer(
    helper.level.server, helper.level, GameProfile(UUID.randomUUID(), "redbull-test"),
    net.minecraft.server.level.ClientInformation.createDefault(),
  ) {
    /** The keys of the action bar's messages, as the client would have shown them. */
    val overlays = mutableListOf<String?>()

    override fun sendOverlayMessage(message: Component) {
      overlays += (message.contents as? TranslatableContents)?.key
      super.sendOverlayMessage(message)
    }

    /** How many times the glide sent out its vibration. */
    var glides = 0

    override fun gameEvent(event: net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent>) {
      if (event.`is`(net.minecraft.world.level.gameevent.GameEvent.ELYTRA_GLIDE)) glides++
      super.gameEvent(event)
    }

    /** Up in the air above the test's floor, where a jump would spread an elytra. */
    fun fly(helper: GameTestHelper, x: Double = 1.5, height: Double = 10.0) {
      val at = helper.absoluteVec(net.minecraft.world.phys.Vec3(x, height, 1.5))
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

  /** The Caffeine effect follows the cans: a level per stage, and as long as the caffeine takes to wear off. */
  private fun caffeine(helper: GameTestHelper) {
    val player = Survivor.join(helper)
    // 30 points a can, a point every 6 seconds: 3 minutes a can.
    listOf("original", "sugarfree", "zero").forEachIndexed { index, id ->
      player.drink(Items.can(can(id)!!))
      val shown = player.getEffect(Caffeines.EFFECT)
      helper.assertTrue(shown?.amplifier == index, "Caffeine ${index + 1} after ${index + 1} cans, not $shown")
      helper.assertTrue(shown?.duration == (index + 1) * 3 * 60 * 20, "${(index + 1) * 3} minutes after ${index + 1} cans, not $shown")
      helper.assertTrue(shown?.isVisible == false && shown.showIcon(), "an icon and no particles")
    }
    Caffeines.reset(player)
    helper.assertTrue(!player.hasEffect(Caffeines.EFFECT), "no caffeine, no effect")
    helper.level.server.playerList.remove(player)
    helper.succeed()
  }

  /** Milk takes 20 caffeine and every effect; the effect comes back for what's left, and after `/effect clear` too. */
  private fun milk(helper: GameTestHelper) {
    val player = Survivor.join(helper)
    player.drink(Items.can(can("original")!!))
    player.drink(ItemStack(Vanilla.MILK_BUCKET))
    helper.runAfterDelay(2) {
      helper.assertTrue(level(player) == 10.0, "milk leaves 10 caffeine, not ${level(player)}")
      val shown = player.getEffect(Caffeines.EFFECT)
      helper.assertTrue(shown?.amplifier == 0 && shown.duration == 60 * 20, "a minute of Caffeine after milk, not $shown")
      player.removeAllEffects()
      helper.assertTrue(!player.hasEffect(Caffeines.EFFECT), "/effect clear takes Caffeine off")
      // The heart beats every two seconds and puts the effect back.
      helper.succeedWhen {
        helper.assertTrue(player.getEffect(Caffeines.EFFECT)?.amplifier == 0, "Caffeine is back after /effect clear")
        helper.level.server.playerList.remove(player)
      }
    }
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
  private fun bed(helper: GameTestHelper) {
    val player = Survivor.join(helper)
    player.drink(Items.can(can("sea_blue")!!))
    val refused = sleep(helper, player)
    helper.assertTrue(key(refused.left().orElse(null)?.message) == "redbull.sleepless", "a caffeinated player stays awake, not $refused")
    helper.assertTrue(!player.isSleeping, "awake")
    Caffeines.reset(player)
    val allowed = sleep(helper, player)
    helper.assertTrue(allowed.right().isPresent && player.isSleeping, "without caffeine the bed takes them, not $allowed")
    player.stopSleeping()
    helper.level.server.playerList.remove(player)
    helper.succeed()
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
      helper.assertTrue("redbull.wings.gone" in player.overlays, "wings that run out say so, not ${player.overlays}")
      helper.level.server.playerList.remove(player)
      helper.succeed()
    }
  }

  /** Wings taken on the ground go quietly; taken mid-glide, they say so and leave Slow Falling. */
  private fun wingsGone(helper: GameTestHelper) {
    val walker = Survivor.join(helper)
    walker.addEffect(MobEffectInstance(Wings.EFFECT, 10 * 20))
    walker.removeEffect(Wings.EFFECT)
    val glider = Survivor.join(helper)
    glider.fly(helper, height = 40.0)
    glider.addEffect(MobEffectInstance(Wings.EFFECT, 10 * 20))
    helper.assertTrue(glider.tryToStartFallFlying(), "wings glide")
    glider.drink(ItemStack(Vanilla.MILK_BUCKET))
    helper.runAfterDelay(2) {
      helper.assertTrue("redbull.wings.gone" !in walker.overlays, "wings taken on the ground say nothing, not ${walker.overlays}")
      helper.assertTrue(!walker.hasEffect(MobEffects.SLOW_FALLING), "no Slow Falling without a flight")
      helper.assertTrue("redbull.wings.gone" in glider.overlays, "wings taken mid-glide say so, not ${glider.overlays}")
      helper.assertTrue(glider.hasEffect(MobEffects.SLOW_FALLING), "wings taken mid-glide leave Slow Falling")
      listOf(walker, glider).forEach { helper.level.server.playerList.remove(it) }
      helper.succeed()
    }
  }

  /**
   * The game wears a glider every second of flight; with Wings an elytra keeps every point, while the same flight
   * without them wears one down.
   */
  private fun elytra(helper: GameTestHelper) {
    val winged = Survivor.join(helper)
    val plain = Survivor.join(helper)
    listOf(winged, plain).forEachIndexed { index, player ->
      player.setItemSlot(EquipmentSlot.CHEST, ItemStack(Vanilla.ELYTRA))
      // A tick puts the elytra's `gliding_flight` on the player, as wearing it would.
      player.doTick()
      player.fly(helper, x = 1.5 + index * 8, height = 40.0)
    }
    winged.addEffect(MobEffectInstance(Wings.EFFECT, 10 * 20))
    helper.assertTrue(winged.tryToStartFallFlying() && plain.tryToStartFallFlying(), "both glide")
    var ticks = 0
    helper.onEachTick {
      listOf(winged, plain).filter { !it.isRemoved }.forEach {
        it.setOnGround(false)
        it.doTick()
      }
      ticks++
    }
    helper.succeedWhen {
      helper.assertTrue(ticks >= 60, "three seconds of flight")
      helper.assertTrue(winged.isFallFlying, "still gliding after $ticks ticks")
      val wear = winged.getItemBySlot(EquipmentSlot.CHEST).damageValue
      helper.assertTrue(wear == 0, "an elytra worn with Wings keeps every point, not $wear lost")
      helper.assertTrue(plain.getItemBySlot(EquipmentSlot.CHEST).damageValue > 0, "an elytra alone wears")
      // Sculk hears the glide every half a second, Wings or not.
      helper.assertTrue(winged.glides == winged.fallFlyingTicks / 10, "${winged.glides} vibrations in ${winged.fallFlyingTicks} ticks")
      listOf(winged, plain).forEach { helper.level.server.playerList.remove(it) }
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
