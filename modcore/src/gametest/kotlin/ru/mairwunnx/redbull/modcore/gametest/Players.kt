package ru.mairwunnx.redbull.modcore.gametest

import com.mojang.authlib.GameProfile
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.Holder
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * Survival players for the tests, on an embedded connection, with a name of their own for commands. The game's own mock
 * player is always in creative, where nothing is used up and caffeine can't kill; and the game protects a player whose
 * client is still loading from all harm.
 */
object Players {
  /** A player who remembers what the client would have shown: the action bar's keys, glides, and feathers. */
  class Survivor(server: MinecraftServer, level: ServerLevel, cookie: CommonListenerCookie) :
    ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation()) {
    val overlays = mutableListOf<String?>()

    /** How many times the glide sent out its vibration. */
    var glides = 0

    /** Where the server's packets to this player end up. */
    lateinit var channel: EmbeddedChannel

    override fun gameEvent(event: Holder<GameEvent>) {
      if (event.`is`(GameEvent.ELYTRA_GLIDE)) glides++
      super.gameEvent(event)
    }

    override fun sendOverlayMessage(message: Component) {
      overlays += (message.contents as? TranslatableContents)?.key
      super.sendOverlayMessage(message)
    }

    /** The bursts of feathers this player was sent near themselves since the last look; other tests glide around too. */
    fun feathers(): Int = generateSequence { channel.readOutbound<Any>() }.count {
      it is ClientboundLevelParticlesPacket && (it.particle() as? ItemParticleOption)?.item?.item()?.value() == Items.FEATHER &&
        position().closerThan(Vec3(it.x(), it.y(), it.z()), 4.0)
    }

    /** Drinks [stack] to the end, as holding the use key would, and returns what's left in the hand. */
    fun drink(stack: ItemStack): ItemStack {
      setItemInHand(InteractionHand.MAIN_HAND, stack)
      startUsingItem(InteractionHand.MAIN_HAND)
      completeUsingItem()
      return mainHandItem
    }

    /** An elytra on the chest; a tick puts its `gliding_flight` on the player on NeoForge, as wearing it would. */
    fun wearElytra() {
      setItemSlot(EquipmentSlot.CHEST, ItemStack(Items.ELYTRA))
      doTick()
    }
  }

  fun player(helper: GameTestHelper): Survivor {
    val server = helper.level.server
    val cookie = CommonListenerCookie.createInitial(UUID.randomUUID().let { GameProfile(it, "rb-${it.toString().take(8)}") }, false)
    val player = Survivor(server, helper.level, cookie)
    val connection = Connection(PacketFlow.SERVERBOUND)
    player.channel = EmbeddedChannel(connection)
    server.playerList.placeNewPlayer(connection, player, cookie)
    player.setGameMode(GameType.SURVIVAL)
    player.connection.handleAcceptPlayerLoad(ServerboundPlayerLoadedPacket())
    return player
  }

  /** A player in the air, as one who jumped off a cliff, [height] blocks above the test. */
  fun airborne(helper: GameTestHelper, height: Double = 40.0, x: Double = 1.5): Survivor = player(helper).apply { lift(helper, height, x) }

  fun Survivor.lift(helper: GameTestHelper, height: Double, x: Double = 1.5) {
    val above = helper.absoluteVec(Vec3(x, height, 1.5))
    teleportTo(above.x, above.y, above.z)
    setOnGround(false)
  }
}
