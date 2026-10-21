package ru.mairwunnx.redbull.neoforge

import net.minecraft.ChatFormatting
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.item.Items as Vanilla
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import ru.mairwunnx.redbull.core.Bottles
import ru.mairwunnx.redbull.core.Can
import ru.mairwunnx.redbull.core.Drink
import ru.mairwunnx.redbull.core.Ids
import ru.mairwunnx.redbull.core.Stage
import ru.mairwunnx.redbull.core.crash
import ru.mairwunnx.redbull.core.crashDelay
import ru.mairwunnx.redbull.core.crashes
import ru.mairwunnx.redbull.core.sugarPit
import ru.mairwunnx.redbull.core.sugarPitDelay
import ru.mairwunnx.redbull.core.drink
import ru.mairwunnx.redbull.core.drinkCrudeTaurine
import ru.mairwunnx.redbull.core.milk
import java.util.UUID

/**
 * What a drink does beyond its potion effects, which the game applies itself: caffeine, the crash, the
 * overdose, wings, and the bottles' bull mode. Milk takes some caffeine away; death takes all of it.
 */
object Drinks {
  /** `death.attack.redbull.overdose`; its tags let it pass totems, armour, and resistance. */
  val OVERDOSE: ResourceKey<DamageType> = ResourceKey.create(Registries.DAMAGE_TYPE, id("overdose"))

  private val rules = Caffeines.rules
  private val bile = mutableMapOf<UUID, Long>()
  private val crude = mutableMapOf<UUID, Long>()

  fun listen(bus: IEventBus) {
    bus.on<LivingEntityUseItemEvent.Start> { event ->
      val player = event.entity as? ServerPlayer ?: return@on
      if (Items.canOf(event.item) != null) raise(player)
    }
    bus.on<LivingEntityUseItemEvent.Finish> { event ->
      val player = event.entity as? ServerPlayer ?: return@on
      finish(event, player)
    }
    bus.on<PlayerEvent.PlayerLoggedOutEvent> { event ->
      bile -= event.entity.uuid
      crude -= event.entity.uuid
    }
  }

  /** The tab pops as the player raises the can. */
  private fun raise(player: ServerPlayer) {
    sound(player, SoundEvents.IRON_TRAPDOOR_OPEN, 0.5f, 1.9f)
    sound(player, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.6f, 1.4f)
  }

  private fun finish(event: LivingEntityUseItemEvent.Finish, player: ServerPlayer) {
    if (event.item.`is`(Vanilla.MILK_BUCKET)) {
      Caffeines.set(player, rules.milk(Caffeines.now(player), Caffeines.clock(player)))
      return
    }
    when (Items.id(event.item)) {
      null -> return
      Ids.BULL_BILE -> bottle(player, bile, crude)
      Ids.CRUDE_TAURINE -> {
        bottle(player, crude, bile)
        caffeine(player, rules.drinkCrudeTaurine(Caffeines.now(player), Caffeines.clock(player)))
      }
      else -> {
        val can = Items.canOf(event.item) ?: return
        val drink = rules.drink(Caffeines.now(player), Caffeines.clock(player))
        if (!caffeine(player, drink)) return
        if (crashes(can, drink.stage)) crashLater(player, can)
        if (!can.sugarfree) sugarPitLater(player, can)
        if (Settings.wings.get() && can.wings > 0) wings(player, can)
      }
    }
  }

  /** Writes the new caffeine; false when it was one drink too many and the player is about to die. */
  private fun caffeine(player: ServerPlayer, drink: Drink): Boolean {
    if (!drink.fatal || !Settings.overdose.get() || !player.gameMode().isSurvival) {
      Caffeines.set(player, drink.caffeine.copy(level = drink.caffeine.level.coerceAtMost(rules.limit)))
      return true
    }
    Caffeines.reset(player)
    // A tick later: dying in the middle of the drink would hand the player its leftovers.
    Later.run(1) {
      if (!player.isRemoved && player.isAlive) {
        player.hurtServer(player.level(), player.damageSources().source(OVERDOSE), Float.MAX_VALUE)
      }
    }
    return false
  }

  /** After the buffs, the crash: a minute slower and weaker. */
  private fun crashLater(player: ServerPlayer, can: Can) {
    Later.run(crashDelay(can) * 20) {
      if (player.isRemoved || !player.isAlive) return@run
      crash().mapNotNull(Items::effect).forEach { player.addEffect(it) }
    }
  }

  /** When a can with sugar runs out of buffs, the sugar pit: a little hungry, a little slow, and told why. */
  private fun sugarPitLater(player: ServerPlayer, can: Can) {
    Later.run(sugarPitDelay(can) * 20) {
      if (player.isRemoved || !player.isAlive) return@run
      sugarPit().mapNotNull(Items::effect).forEach { player.addEffect(it) }
      player.sendOverlayMessage(Component.translatable("redbull.sugar_pit").withStyle(ChatFormatting.GOLD))
    }
  }

  /** Up into the air, with the Wings effect to glide on; the can still crumples as any other. */
  private fun wings(player: ServerPlayer, can: Can) {
    Wings.give(player, can.wings)
    player.setDeltaMovement(player.deltaMovement.x, 0.9, player.deltaMovement.z)
    // The client moves its own player; tell it about the push.
    player.connection.send(ClientboundSetEntityMotionPacket(player))
    sound(player, SoundEvents.BREEZE_JUMP, 0.8f, 1.2f)
    player.sendOverlayMessage(text("redbull.wings", ChatFormatting.AQUA))
  }

  /** A bottle makes the player moo; the other one within a minute before it starts bull mode. */
  private fun bottle(player: ServerPlayer, mine: MutableMap<UUID, Long>, other: MutableMap<UUID, Long>) {
    val now = System.currentTimeMillis()
    mine[player.uuid] = now
    sound(player, Sounds.COW_AMBIENT, 1f, 0.8f)
    if (!Bottles.bullMode(other[player.uuid], now)) return
    mine -= player.uuid
    other -= player.uuid
    Bottles.BULL_MODE.mapNotNull(Items::effect).forEach { player.addEffect(it) }
    sound(player, Sounds.COW_HURT, 2f, 0.5f)
    player.sendOverlayMessage(text("redbull.bull_mode", ChatFormatting.RED))
  }

  private fun sound(player: ServerPlayer, sound: SoundEvent, volume: Float, pitch: Float) =
    player.level().playSound(null, player.x, player.y, player.z, sound, SoundSource.PLAYERS, volume, pitch)

  fun text(key: String, color: ChatFormatting): Component = Component.translatable(key).withStyle(color)

  /** The classic cow's sounds, which the game now keeps per cow variant rather than as constants. */
  private object Sounds {
    val COW_AMBIENT: SoundEvent = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("entity.cow.ambient"))
    val COW_HURT: SoundEvent = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("entity.cow.hurt"))
  }
}
