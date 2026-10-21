package ru.mairwunnx.redbull.fabric.gametest

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.fabric.Caffeines
import ru.mairwunnx.redbull.fabric.Wings

/**
 * A real client in a fresh world, with cans and bottles in the hotbar and caffeine in the jitters: Caffeine II and
 * Wings reach the client and show their icons on the HUD and in the inventory.
 * `./gradlew :fabric:runClientGameTest` opens a game window and saves the screenshots in
 * `fabric/build/run/clientGameTest/screenshots`.
 */
class RedBullClientGameTest : FabricClientGameTest {
  override fun runTest(context: ClientGameTestContext) {
    context.worldBuilder().create().use { world ->
      world.connection.waitForChunksRender()
      listOf("original", "cherry", "summer", "crumpled_original", "bull_bile", "crude_taurine", "taurine", "empty_can")
        .forEachIndexed { slot, id -> world.server.runCommand("item replace entity @a hotbar.$slot with redbull:$id") }
      world.server.runOnServer<RuntimeException> { server ->
        val player = server.playerList.players.single()
        Caffeines.set(player, Caffeine(60.0, Caffeines.clock(player)))
      }
      context.waitTicks(20)
      val level = context.computeOnClient<Int?, RuntimeException> { it.player?.getEffect(Caffeines.EFFECT)?.amplifier }
      check(level == 1) { "the client sees Caffeine at level $level, not II" }
      context.takeScreenshot("redbull-hud")

      // Wings reach the client too, which asks them whether its player may glide: the icon shows with the others.
      world.server.runCommand("effect give @a redbull:wings 60")
      context.waitTicks(20)
      val wings = context.computeOnClient<Boolean, RuntimeException> { it.player?.hasEffect(Wings.WINGS) == true }
      check(wings) { "the client doesn't see Wings" }
      context.takeScreenshot("redbull-wings-hud")
      context.setScreen { InventoryScreen(Minecraft.getInstance().player!!) }
      context.waitTicks(5)
      context.takeScreenshot("redbull-wings-inventory")
      context.setScreen { null }
    }
  }
}
