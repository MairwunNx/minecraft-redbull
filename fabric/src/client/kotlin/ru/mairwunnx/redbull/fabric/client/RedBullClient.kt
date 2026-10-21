package ru.mairwunnx.redbull.fabric.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.AttackIndicatorStatus
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.ItemStack
import ru.mairwunnx.redbull.core.Stage
import ru.mairwunnx.redbull.fabric.Caffeines
import ru.mairwunnx.redbull.fabric.RedBullItems
import kotlin.math.ceil

object RedBullClient : ClientModInitializer {
  override fun onInitializeClient() {
    HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, RedBullItems.id("caffeine"), CaffeineMeter::extract)
  }
}

/**
 * The caffeine meter: a can beside the hotbar with a bar under it, filled up to the limit as a durability bar is,
 * blue while calm, yellow in the jitters, red in the crash. It shows only while there is caffeine to show. The server
 * syncs the level every two seconds, so the bar follows as caffeine wears off.
 */
private object CaffeineMeter {
  private const val WIDTH = 13
  private val can by lazy { ItemStack(RedBullItems.cans.getValue("original")) }

  fun extract(graphics: GuiGraphicsExtractor, @Suppress("UNUSED_PARAMETER") delta: DeltaTracker) {
    val minecraft = Minecraft.getInstance()
    val player = minecraft.player ?: return
    if (player.isSpectator) return
    val caffeine = player.getAttached(Caffeines.CAFFEINE) ?: return
    if (caffeine.level <= 0.0) return

    // Right of the hotbar, past the offhand slot or the attack indicator when either sits there too.
    var x = graphics.guiWidth() / 2 + 91 + 6
    if (player.mainArm == HumanoidArm.LEFT && !player.offhandItem.isEmpty) x += 29
    if (player.mainArm == HumanoidArm.RIGHT && minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) x += 20
    val y = graphics.guiHeight() - 19

    val rules = Caffeines.rules
    val color = when (caffeine.stage(rules)) {
      Stage.CALM -> 0xFF5599FF.toInt()
      Stage.JITTERS -> 0xFFFFFF55.toInt()
      Stage.CRASH, Stage.OVERDOSE -> 0xFFFF5555.toInt()
    }
    val filled = ceil(WIDTH * (caffeine.level / rules.limit).coerceIn(0.0, 1.0)).toInt()
    graphics.item(can, x, y)
    graphics.fill(x + 2, y + 13, x + 2 + WIDTH, y + 15, 0xFF000000.toInt())
    graphics.fill(x + 2, y + 13, x + 2 + filled, y + 14, color)
  }
}
