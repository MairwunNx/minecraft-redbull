package ru.mairwunnx.redbull.paper

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerBedEnterEvent
import ru.mairwunnx.redbull.core.sleepless

/** What caffeine does to a body besides the stages: any of it keeps a player out of bed. */
class Symptoms : Listener {
  @EventHandler(ignoreCancelled = true)
  fun onBed(event: PlayerBedEnterEvent) {
    if (event.bedEnterResult != PlayerBedEnterEvent.BedEnterResult.OK) return
    if (!Caffeines.now(event.player).sleepless()) return
    event.isCancelled = true
    event.player.sendActionBar(Texts.text("redbull.sleepless").color(NamedTextColor.YELLOW))
  }
}
