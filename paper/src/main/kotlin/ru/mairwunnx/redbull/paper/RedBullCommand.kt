package ru.mairwunnx.redbull.paper

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.Locale

/**
 * `/redbull give <item> [player] [amount]` hands out any of our items; `/redbull caffeine [player]` shows
 * a player's caffeine and stage. Both need `redbull.admin`.
 */
class RedBullCommand : BasicCommand {
  override fun permission() = "redbull.admin"

  override fun execute(source: CommandSourceStack, args: Array<String>) {
    val sender = source.sender
    when (args.firstOrNull()?.lowercase()) {
      "give" -> {
        val id = args.getOrNull(1) ?: return usage(source)
        val item = Items.create(id) ?: return sender.sendMessage(Texts.text("redbull.command.unknown", Component.text(id)))
        val target = player(source, args.getOrNull(2)) ?: return usage(source)
        val amount = args.getOrNull(3)?.toIntOrNull()?.coerceIn(1, 64) ?: 1
        repeat(amount) { target.inventory.addItem(item.clone()).values.forEach { target.world.dropItem(target.location, it) } }
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

  override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> = when (args.size) {
    0, 1 -> listOf("give", "caffeine").filter { it.startsWith(args.firstOrNull().orEmpty()) }
    2 -> if (args[0] == "give") Items.ids.filter { it.startsWith(args[1]) } else players(args[1])
    3 -> if (args[0] == "give") players(args[2]) else emptyList()
    else -> emptyList()
  }

  private fun players(prefix: String) = Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(prefix, ignoreCase = true) }

  private fun player(source: CommandSourceStack, name: String?): Player? =
    if (name != null) Bukkit.getPlayerExact(name) else source.executor as? Player ?: source.sender as? Player

  private fun usage(source: CommandSourceStack) {
    source.sender.sendMessage(Component.text("/redbull give <item> [player] [amount] · /redbull caffeine [player]"))
  }
}
