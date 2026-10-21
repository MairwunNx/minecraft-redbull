package ru.mairwunnx.redbull.neoforge

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Prediction
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.RegisterCommandsEvent
import ru.mairwunnx.redbull.core.Ids
import java.util.Locale

/**
 * `/redbull` opens the collection for every player. `/redbull give <item> [player] [amount]` hands out any of
 * our items, and `/redbull caffeine [player]` shows a player's caffeine and stage; both need operator rights.
 */
object RedBullCommand {
  fun listen(bus: IEventBus) {
    bus.on<RegisterCommandsEvent> { event ->
      val admin = Commands.hasPermission<CommandSourceStack>(Commands.LEVEL_GAMEMASTERS)
      event.dispatcher.register(
        Commands.literal("redbull")
          .executes { Menu.open(it.source.playerOrException); 1 }
          .then(
            Commands.literal("give").requires(admin).then(
              Commands.argument("item", StringArgumentType.word())
                .suggests { _, builder -> SharedSuggestionProvider.suggest(Items.all.map { it.id.path }, builder) }
                .executes { give(it, it.source.playerOrException, 1) }
                .then(
                  Commands.argument("player", EntityArgument.player())
                    .executes { give(it, EntityArgument.getPlayer(it, "player"), 1) }
                    .then(
                      Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                        .executes { give(it, EntityArgument.getPlayer(it, "player"), IntegerArgumentType.getInteger(it, "amount")) },
                    ),
                ),
            ),
          )
          .then(
            Commands.literal("caffeine").requires(admin)
              .executes { caffeine(it, it.source.playerOrException) }
              .then(Commands.argument("player", EntityArgument.player()).executes { caffeine(it, EntityArgument.getPlayer(it, "player")) }),
          ),
      )
    }
  }

  /** An item handed to a player outside the game's own ways: it joins their collection, and a winged can is watched. */
  fun hand(player: ServerPlayer, item: ItemStack) {
    Collected.record(player, listOf(item))
    if (Items.id(item) == Ids.WINGED_CAN) Wings.watch(player)
    val stack = item.copy()
    player.inventory.add(stack)
    if (!stack.isEmpty) player.drop(stack, false, Prediction.SERVER_ONLY)
  }

  private fun give(context: CommandContext<CommandSourceStack>, target: ServerPlayer, amount: Int): Int {
    val id = StringArgumentType.getString(context, "item")
    val item = Items.create(id)
    if (item == null) {
      context.source.sendFailure(Component.translatable("redbull.command.unknown", id))
      return 0
    }
    repeat(amount) { hand(target, item.copy()) }
    context.source.sendSuccess({ Component.translatable("redbull.command.given", amount, item.hoverName, target.displayName) }, true)
    return amount
  }

  private fun caffeine(context: CommandContext<CommandSourceStack>, target: ServerPlayer): Int {
    val caffeine = Caffeines.now(target)
    val stage = caffeine.stage(Caffeines.rules).name.lowercase(Locale.ROOT).let { if (it == "overdose") "crash" else it }
    context.source.sendSuccess({
      Component.translatable(
        "redbull.command.caffeine",
        target.displayName,
        String.format(Locale.ROOT, "%.1f", caffeine.level),
        Caffeines.rules.limit.toInt(),
        Component.translatable("redbull.stage.$stage"),
      )
    }, false)
    return caffeine.level.toInt()
  }
}
