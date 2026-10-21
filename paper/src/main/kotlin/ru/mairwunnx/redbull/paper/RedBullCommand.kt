package ru.mairwunnx.redbull.paper

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.Locale

/**
 * `/redbull` opens the menu of cans for every player. `/redbull give <item> [player] [amount]` hands out any
 * of our items, and `/redbull caffeine [player]` shows a player's caffeine and stage; both need [ADMIN].
 */
class RedBullCommand(private val plugin: RedBullPlugin, private val menu: Menu) : BasicCommand {
  override fun execute(source: CommandSourceStack, args: Array<String>) {
    val sender = source.sender
    if (args.isEmpty()) {
      val player = source.executor as? Player ?: sender as? Player ?: return usage(source)
      return menu.open(player)
    }
    if (!sender.hasPermission(ADMIN)) return menu.open(sender as? Player ?: return)
    when (args.firstOrNull()?.lowercase()) {
      "give" -> {
        val id = args.getOrNull(1) ?: return usage(source)
        val item = Items.create(id) ?: return sender.sendMessage(Texts.text("redbull.command.unknown", Component.text(id)))
        val target = player(source, args.getOrNull(2)) ?: return usage(source)
        val amount = args.getOrNull(3)?.toIntOrNull()?.coerceIn(1, 64) ?: 1
        repeat(amount) { target.inventory.addItem(item.clone()).values.forEach { target.world.dropItem(target.location, it) } }
        plugin.handed(target, item)
        sender.sendMessage(Texts.text("redbull.command.given", Component.text(amount), item.effectiveName(), target.name()))
      }
      "caffeine" -> {
        val target = player(source, args.getOrNull(1)) ?: return usage(source)
        val caffeine = Caffeines.now(target)
        val stage = caffeine.stage(Caffeines.rules).name.lowercase(Locale.ROOT)
        sender.sendMessage(Texts.text(
          "redbull.command.caffeine",
          target.name(),
          Component.text(String.format(Locale.ROOT, "%.1f", caffeine.level)),
          Component.text(Caffeines.rules.limit.toInt()),
          Texts.text("redbull.stage.${if (stage == "overdose") "crash" else stage}"),
        ))
      }
      else -> usage(source)
    }
  }

  override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> = if (!source.sender.hasPermission(ADMIN)) emptyList() else when (args.size) {
    0, 1 -> listOf("give", "caffeine").filter { it.startsWith(args.firstOrNull().orEmpty()) }
    2 -> if (args[0] == "give") Items.ids.filter { it.startsWith(args[1]) } else players(args[1])
    3 -> if (args[0] == "give") players(args[2]) else emptyList()
    else -> emptyList()
  }

  private fun players(prefix: String) = Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(prefix, ignoreCase = true) }

  private fun player(source: CommandSourceStack, name: String?): Player? =
    if (name != null) Bukkit.getPlayerExact(name) else source.executor as? Player ?: source.sender as? Player

  private fun usage(source: CommandSourceStack) {
    source.sender.sendMessage(Component.text("/redbull · /redbull give <item> [player] [amount] · /redbull caffeine [player]"))
  }

  companion object {
    const val ADMIN = "redbull.admin"
  }
}
