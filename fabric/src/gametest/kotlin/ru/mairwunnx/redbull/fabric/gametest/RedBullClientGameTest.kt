package ru.mairwunnx.redbull.fabric.gametest

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.fabric.Caffeines

/**
 * A real client in a fresh world, with cans and bottles in the hotbar and caffeine in the jitters: the caffeine
 * meter reaches the client and draws beside the hotbar. `./gradlew :fabric:runClientGameTest` opens a game window
 * and saves the screenshot in `fabric/build/run/clientGameTest/screenshots`.
 */
class RedBullClientGameTest : FabricClientGameTest {
  override fun runTest(context: ClientGameTestContext) {
    context.worldBuilder().create().use { world ->
      world.connection.waitForChunksRender()
      listOf("original", "cherry", "summer", "winged_can", "crumpled_original", "bull_bile", "crude_taurine", "taurine", "empty_can")
        .forEachIndexed { slot, id -> world.server.runCommand("item replace entity @a hotbar.$slot with redbull:$id") }
      world.server.runOnServer<RuntimeException> { server ->
        val player = server.playerList.players.single()
        Caffeines.set(player, Caffeine(60.0, Caffeines.clock(player)))
      }
      context.waitTicks(20)
      val level = context.computeOnClient<Double?, RuntimeException> { it.player?.getAttached(Caffeines.CAFFEINE)?.level }
      check(level != null && level > 55.0) { "the client sees caffeine $level" }
      context.takeScreenshot("redbull-hud")
    }
  }
}
