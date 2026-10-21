package ru.mairwunnx.redbull.paper

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.net.URI
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

/**
 * Sends the resource pack on join. Its own, next to any pack the server or another plugin sends: the game
 * stacks them. The jar carries the same zip the release attaches, so the hash comes from it.
 */
class Pack(private val plugin: RedBullPlugin) : Listener {
  private val id = UUID.nameUUIDFromBytes("redbull".toByteArray())
  private val hash: String by lazy {
    val bytes = plugin.getResource("redbull-pack.zip")!!.use { it.readBytes() }
    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes))
  }

  /** The configured URL, or this release's pack on GitHub; a dev build has none. */
  val url: URI?
    get() {
      val configured = plugin.settings.resourcePack.url.trim()
      if (configured.isNotEmpty()) return URI(configured)
      val version = plugin.pluginMeta.version
      if (!Regex("""\d+\.\d+\.\d+""").matches(version)) return null
      return URI("https://github.com/MairwunNx/minecraft-redbull/releases/download/v$version/redbull-pack-$version.zip")
    }

  @EventHandler
  fun onJoin(event: PlayerJoinEvent) {
    val settings = plugin.settings.resourcePack
    if (!settings.enabled) return
    val uri = url ?: return
    event.player.sendResourcePacks(
      ResourcePackRequest.resourcePackRequest()
        .packs(ResourcePackInfo.resourcePackInfo(id, uri, hash))
        .required(settings.required)
        .prompt(Texts.text("redbull.pack.prompt"))
        .replace(false)
        .build(),
    )
  }
}
