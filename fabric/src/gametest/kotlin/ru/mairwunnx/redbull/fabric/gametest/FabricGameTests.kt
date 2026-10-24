package ru.mairwunnx.redbull.fabric.gametest

import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.fabric.Settings
import ru.mairwunnx.redbull.modcore.Caffeines
import ru.mairwunnx.redbull.modcore.RedBullItems
import ru.mairwunnx.redbull.modcore.Wings
import ru.mairwunnx.redbull.modcore.gametest.Players.player

/** What only the Fabric mod has: its own reader of `config/redbull.toml`. modcore's tests cover the rest. */
class FabricGameTests {
  private fun equal(helper: GameTestHelper, actual: Any?, expected: Any?, what: String) =
    helper.assertTrue(actual == expected, "$what: expected $expected, got $actual")

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
      player.drink(ItemStack(RedBullItems.can("red")))
      equal(helper, Caffeines.now(player).level, Caffeines.rules.limit, "caffeine past the limit without the overdose")
      Settings.read("wings = false")
      val other = player(helper)
      other.drink(ItemStack(RedBullItems.can("original")))
      helper.assertFalse(other.hasEffect(Wings.EFFECT) || other.deltaMovement.y > 0.5, "wings switched off")
    } finally {
      Settings.read("")
    }
    equal(helper, Settings.overdose, true, "the default")
    helper.succeed()
  }
}
