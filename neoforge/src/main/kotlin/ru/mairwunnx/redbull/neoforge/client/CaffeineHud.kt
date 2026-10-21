package ru.mairwunnx.redbull.neoforge.client

import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import ru.mairwunnx.redbull.core.CANS
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.core.Stage
import ru.mairwunnx.redbull.neoforge.Caffeines
import ru.mairwunnx.redbull.neoforge.Items
import ru.mairwunnx.redbull.neoforge.id
import ru.mairwunnx.redbull.neoforge.on
import kotlin.math.ceil

/**
 * The caffeine meter: a row of ten small cans above the hunger bar, shown only while there's caffeine. A full can
 * stands for each tenth of the limit taken, an empty one for the room left; in the jitters the row trembles now
 * and then, and in the crash stage it shakes. The server sends the level when it changes; between those,
 * the meter wears it off at the rules' pace, as the server does.
 */
object CaffeineHud {
  private const val ICONS = 10

  private var seen: Caffeine? = null
  private var seenAt = 0

  fun listen(modBus: IEventBus) {
    modBus.on<RegisterGuiLayersEvent> { it.registerAbove(VanillaGuiLayers.FOOD_LEVEL, id("caffeine"), ::render) }
  }

  private fun render(graphics: GuiGraphicsExtractor, delta: DeltaTracker) {
    val minecraft = Minecraft.getInstance()
    val player = minecraft.player ?: return
    if (minecraft.gui.hud.isHidden || minecraft.gameMode?.canHurtPlayer() != true) return
    val rules = Caffeines.rules
    val caffeine = player.getData(Caffeines.CAFFEINE)
    if (caffeine !== seen) {
      seen = caffeine
      seenAt = player.tickCount
    }
    val level = (caffeine.level - (player.tickCount - seenAt).coerceAtLeast(0) * rules.decayPerTick).coerceIn(0.0, rules.limit)
    if (level <= 0.0) return

    val hud = minecraft.gui.hud
    val right = graphics.guiWidth() / 2 + 91
    val y = graphics.guiHeight() - hud.rightHeight
    hud.rightHeight += 10
    val full = ceil(level / rules.limit * ICONS).toInt()
    val stage = Caffeine(level).stage(rules)
    // Seeded by the tick, as the hunger bar's shake is, so the row holds still between ticks.
    val random = RandomSource.create(player.tickCount * 312871L)
    for (i in 0 until ICONS) {
      val shake = when (stage) {
        Stage.JITTERS -> if (player.tickCount % 10 == 0) random.nextInt(3) - 1 else 0
        Stage.CRASH, Stage.OVERDOSE -> random.nextInt(3) - 1
        Stage.CALM -> 0
      }
      val x = right - i * 8 - 9
      val stack = if (i < full) CAN else EMPTY
      graphics.pose().pushMatrix()
      graphics.pose().translate(x.toFloat(), (y + shake).toFloat())
      graphics.pose().scale(0.5625f, 0.5625f)
      graphics.item(stack, 0, 0)
      graphics.pose().popMatrix()
    }
  }

  private val CAN: ItemStack by lazy { Items.can(CANS.first()) }
  private val EMPTY: ItemStack by lazy { ItemStack(Items.emptyCan.get()) }
}
