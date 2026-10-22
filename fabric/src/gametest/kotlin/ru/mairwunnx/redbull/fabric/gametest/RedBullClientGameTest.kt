package ru.mairwunnx.redbull.fabric.gametest

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import ru.mairwunnx.redbull.core.Advancements
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.fabric.Awards
import ru.mairwunnx.redbull.fabric.Caffeines
import ru.mairwunnx.redbull.fabric.Collected
import ru.mairwunnx.redbull.fabric.Menu
import ru.mairwunnx.redbull.fabric.RedBullItems
import ru.mairwunnx.redbull.fabric.Wings

/**
 * A real client in a fresh survival world at noon, as a player without operator rights, and the README's screenshots:
 * `world` with cans and bottles in the hotbar, `effects` with Caffeine II and Wings beside the inventory, and
 * `collection` with the `/redbull` chest of a player who has found a few cans, and `advancements` with Red Bull's tab. `./gradlew :fabric:runClientGameTest`
 * opens a game window and saves them in `fabric/build/run/clientGameTest/screenshots`; copy them to
 * `media/screenshots` when the look changes.
 */
class RedBullClientGameTest : FabricClientGameTest {
  override fun runTest(context: ClientGameTestContext) {
    context.runOnClient<RuntimeException> {
      it.options.languageCode = "en_us"
      it.options.guiScale().set(2)
    }
    context.input.resizeWindow(1280, 720)
    context.worldBuilder().create().use { world ->
      world.connection.waitForChunksRender()
      world.server.runCommand("time set noon")
      listOf("original", "cherry", "summer", "crumpled_original", "bull_bile", "crude_taurine", "taurine", "empty_can")
        .forEachIndexed { slot, id -> world.server.runCommand("item replace entity @a hotbar.$slot with redbull:$id") }
      val admin = world.server.computeOnServer<Boolean, RuntimeException> { Menu.admin(it.playerList.players.single()) }
      check(!admin) { "the player is an operator; the collection would show everything" }
      context.waitTicks(20)
      screenshot(context, "world")

      // Two cans' worth of caffeine and a can's wings: Caffeine II and Wings reach the client and show their icons.
      world.server.runOnServer<RuntimeException> { server ->
        val player = server.playerList.players.single()
        Caffeines.set(player, Caffeine(2 * Caffeines.rules.perCan, Caffeines.clock(player)))
      }
      world.server.runCommand("effect give @a redbull:wings 60 0 true")
      listOf("ruby", "zero", "tropical", "glacier", "crumpled_cherry", "sea_blue")
        .forEachIndexed { slot, id -> world.server.runCommand("item replace entity @a inventory.${slot * 2} with redbull:$id") }
      context.waitTicks(20)
      val shown = context.computeOnClient<Pair<Int?, Boolean>, RuntimeException> {
        it.player?.getEffect(Caffeines.EFFECT)?.amplifier to (it.player?.hasEffect(Wings.WINGS) == true)
      }
      check(shown == (1 to true)) { "the client sees Caffeine at level ${shown.first} and Wings ${shown.second}, not II and Wings" }
      context.setScreen { InventoryScreen(Minecraft.getInstance().player!!) }
      context.input.setCursorPos(0.0, 0.0)
      context.waitTicks(5)
      screenshot(context, "effects")
      context.setScreen { null }

      // The cans the player has held, and question marks for the rest.
      world.server.runOnServer<RuntimeException> { server ->
        val player = server.playerList.players.single()
        Collected.add(player, setOf("original", "cherry", "summer", "ruby", "zero", "tropical", "glacier", "sea_blue"))
        Menu.open(player)
      }
      context.waitForScreen(ContainerScreen::class.java)
      context.input.setCursorPos(0.0, 0.0)
      context.waitTicks(5)
      screenshot(context, "collection")
      context.setScreen { null }

      // Red Bull's tab of advancements: the first can came with the collection; add the first glide, the edge, and
      // the sugar pit, and leave the legend and the collector to find.
      world.server.runOnServer<RuntimeException> { server ->
        val player = server.playerList.players.single()
        listOf(Advancements.WINGS, Advancements.EDGE, Advancements.SUGAR_PIT).forEach { Awards.award(player, it) }
      }
      context.waitTicks(5)
      context.runOnClient<RuntimeException> {
        val advancements = it.player!!.connection.advancements
        val root = advancements.get(RedBullItems.id(Advancements.ROOT)) ?: error("the client has no Red Bull advancements")
        advancements.setSelectedTab(root, false)
      }
      context.setScreen { AdvancementsScreen(Minecraft.getInstance().player!!.connection.advancements) }
      context.input.setCursorPos(0.0, 0.0)
      context.waitTicks(5)
      screenshot(context, "advancements")
      context.setScreen { null }
    }
  }

  /**
   * Without the recipe book's toasts, which cover the corner whenever the player gets an ingredient, or the jitters'
   * warning in the action bar.
   */
  private fun screenshot(context: ClientGameTestContext, name: String) {
    context.runOnClient<RuntimeException> {
      it.gui.toastManager().clear()
      it.gui.hud.setOverlayMessage(Component.empty(), false)
    }
    context.waitTicks(2)
    context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix())
  }
}
