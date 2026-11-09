package ru.mairwunnx.redbull.modcore

import net.minecraft.server.MinecraftServer
import ru.mairwunnx.redbull.core.Caffeine
import ru.mairwunnx.redbull.core.RedBullPlayers
import java.util.UUID

/**
 * Red Bull's Allay tools' view of a player on the mods: caffeine and the collection from the loader's attachments. The
 * mods count no numbers for all time, so [allTime] stays null. One per server, as a singleplayer world starts its own.
 */
class AllayPlayers(private val server: MinecraftServer) : RedBullPlayers {
  override fun caffeine(player: UUID): Caffeine? = server.playerList.getPlayer(player)?.let(Caffeines::now)

  override fun collection(player: UUID): Set<String>? = server.playerList.getPlayer(player)?.let(Collected::of)
}
