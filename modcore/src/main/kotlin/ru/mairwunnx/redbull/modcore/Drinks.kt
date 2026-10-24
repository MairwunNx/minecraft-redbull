package ru.mairwunnx.redbull.modcore

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
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import ru.mairwunnx.redbull.core.Advancements
import ru.mairwunnx.redbull.core.Bottles
import ru.mairwunnx.redbull.core.Can
import ru.mairwunnx.redbull.core.Drink
import ru.mairwunnx.redbull.core.crash
import ru.mairwunnx.redbull.core.crashDelay
import ru.mairwunnx.redbull.core.crashes
import ru.mairwunnx.redbull.core.drink
import ru.mairwunnx.redbull.core.drinkCrudeTaurine
import ru.mairwunnx.redbull.core.milk
import ru.mairwunnx.redbull.core.sugarPit
import ru.mairwunnx.redbull.core.sugarPitDelay
import java.util.UUID

/**
 * What a drink does beyond its potion effects, which the game applies itself: caffeine, the crash, the overdose,
 * wings, and the bottles' bull mode. Milk takes some caffeine away; death takes all of it.
 */
object Drinks {
  /** Dies without a totem: the damage type is in `bypasses_invulnerability`, and its message names the drink. */
  private val OVERDOSE: ResourceKey<DamageType> = ResourceKey.create(Registries.DAMAGE_TYPE, id("overdose"))

  /** The classic cow's sounds, which the game now keeps per cow variant rather than as constants. */
  private val COW_AMBIENT = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("entity.cow.ambient"))
  private val COW_HURT = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("entity.cow.hurt"))

  private val rules = Caffeines.rules
  private val bile = mutableMapOf<UUID, Long>()
  private val crude = mutableMapOf<UUID, Long>()

  /** The tab pops as the player raises the can. */
  fun raise(player: ServerPlayer) {
    sound(player, SoundEvents.IRON_TRAPDOOR_OPEN, 0.5f, 1.9f)
    sound(player, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.6f, 1.4f)
  }

  /** After a can went down: caffeine, maybe a crash later, and wings. */
  fun can(player: ServerPlayer, can: Can) {
    val drink = rules.drink(Caffeines.now(player), Caffeines.clock(player))
    if (!caffeine(player, drink)) return
    if (crashes(can, drink.stage)) crashLater(player, can)
    if (!can.sugarfree) sugarPitLater(player, can)
    if (Mod.settings.wings && can.wings > 0) wings(player, can)
  }

  fun bile(player: ServerPlayer) = bottle(player, bile, crude)

  fun crudeTaurine(player: ServerPlayer) {
    bottle(player, crude, bile)
    caffeine(player, rules.drinkCrudeTaurine(Caffeines.now(player), Caffeines.clock(player)))
  }

  /** Milk takes some caffeine, once it took every effect: the Caffeine effect shows what's left. */
  fun milk(player: ServerPlayer) = Caffeines.set(player, rules.milk(Caffeines.now(player), Caffeines.clock(player)))

  /** Writes the new caffeine; false when it was one drink too many and the player is about to die. */
  private fun caffeine(player: ServerPlayer, drink: Drink): Boolean {
    if (!drink.fatal || !Mod.settings.overdose || player.isCreative || player.isSpectator) {
      val level = drink.caffeine.level.coerceAtMost(rules.limit)
      Caffeines.set(player, drink.caffeine.copy(level = level))
      Awards.caffeine(player, level)
      return true
    }
    Caffeines.reset(player)
    // After the drink finishes: dying in the middle of it would hand the player its leftovers.
    Later.run(1) {
      if (!player.hasDisconnected() && player.isAlive) {
        val type = player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(OVERDOSE)
        player.hurtServer(player.level(), DamageSource(type), Float.MAX_VALUE)
      }
    }
    return false
  }

  /** After the buffs, the crash: a minute slower and weaker. */
  private fun crashLater(player: ServerPlayer, can: Can) = Later.run(crashDelay(can) * 20L) {
    if (!player.hasDisconnected() && player.isAlive) crash().mapNotNull(RedBullItems::effect).forEach(player::addEffect)
  }

  /** When a can with sugar runs out of buffs, the sugar pit: a little hungry, a little slow, and told why. */
  private fun sugarPitLater(player: ServerPlayer, can: Can) = Later.run(sugarPitDelay(can) * 20L) {
    if (!player.hasDisconnected() && player.isAlive) sugarPit(player)
  }

  /** The sugar pit itself, and its advancement. */
  fun sugarPit(player: ServerPlayer) {
    sugarPit().mapNotNull(RedBullItems::effect).forEach(player::addEffect)
    player.sendOverlayMessage(text("redbull.sugar_pit", ChatFormatting.GOLD))
    Awards.award(player, Advancements.SUGAR_PIT)
  }

  /** Up into the air, with [Can.wings] seconds of the Wings effect to glide on; the can still crumples as any other. */
  private fun wings(player: ServerPlayer, can: Can) {
    val motion = player.deltaMovement
    player.setDeltaMovement(motion.x, 0.9, motion.z)
    // The client moves its own player; tell it about the push.
    player.connection.send(ClientboundSetEntityMotionPacket(player))
    sound(player, SoundEvents.BREEZE_JUMP, 0.8f, 1.2f)
    player.sendOverlayMessage(text("redbull.wings", ChatFormatting.AQUA))
    Wings.give(player, can.wings)
  }

  /** A bottle makes the player moo; the other one within a minute before it starts bull mode. */
  private fun bottle(player: ServerPlayer, mine: MutableMap<UUID, Long>, other: MutableMap<UUID, Long>) {
    val now = System.currentTimeMillis()
    mine[player.uuid] = now
    sound(player, COW_AMBIENT, 1f, 0.8f)
    if (!Bottles.bullMode(other[player.uuid], now)) return
    mine.remove(player.uuid)
    other.remove(player.uuid)
    Bottles.BULL_MODE.mapNotNull(RedBullItems::effect).forEach(player::addEffect)
    sound(player, COW_HURT, 2f, 0.5f)
    player.sendOverlayMessage(text("redbull.bull_mode", ChatFormatting.RED))
  }

  fun forget(id: UUID) {
    bile.remove(id)
    crude.remove(id)
  }

  /** Everyone around hears it. */
  private fun sound(player: ServerPlayer, sound: SoundEvent, volume: Float, pitch: Float) =
    player.level().playSound(null, player.x, player.y, player.z, sound, SoundSource.PLAYERS, volume, pitch)

  fun text(key: String, color: ChatFormatting): Component = Component.translatable(key).withStyle(color)
}
