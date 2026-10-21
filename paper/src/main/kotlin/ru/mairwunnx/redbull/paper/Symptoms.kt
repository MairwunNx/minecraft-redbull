package ru.mairwunnx.redbull.paper

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.util.Vector
import ru.mairwunnx.redbull.core.shake
import ru.mairwunnx.redbull.core.sleepless
import java.util.concurrent.ThreadLocalRandom

/**
 * What caffeine does to a body besides the stages: it keeps a player out of bed, and in the jitters and the crash
 * their arrows and bolts fly wide, as the game makes a skeleton's do.
 */
class Symptoms : Listener {
  @EventHandler(ignoreCancelled = true)
  fun onBed(event: PlayerBedEnterEvent) {
    if (event.bedEnterResult != PlayerBedEnterEvent.BedEnterResult.OK) return
    if (!Caffeines.now(event.player).sleepless()) return
    event.isCancelled = true
    event.player.sendActionBar(Texts.text("redbull.sleepless").color(NamedTextColor.YELLOW))
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onShoot(event: EntityShootBowEvent) {
    val player = event.entity as? Player ?: return
    val shake = Caffeines.now(player).stage(Caffeines.rules).shake()
    if (shake == 0.0) return
    // As Projectile.shoot spreads a shot: each axis of the direction moves by a gaussian times 0.0075 times the inaccuracy.
    val projectile = event.projectile
    val velocity = projectile.velocity
    val speed = velocity.length()
    val random = ThreadLocalRandom.current()
    val direction = velocity.normalize().add(Vector(
      random.nextGaussian() * 0.0075 * shake, random.nextGaussian() * 0.0075 * shake, random.nextGaussian() * 0.0075 * shake,
    ))
    projectile.velocity = direction.normalize().multiply(speed)
  }
}
