package ru.mairwunnx.redbull.paper

/**
 * Red Bull's numbers in WorldAPI, when it runs: the `redbull` namespace with the totals, players' numbers, and boards.
 * [StatsExpansion] speaks WorldAPI's expansion API 2. This class holds no type of it, so it loads without WorldAPI or
 * with one older than 4.5.0. Red Bull keeps its numbers in [Tally], and WorldAPI reads them on request, from its own threads.
 */
class WorldApiLink(private val plugin: RedBullPlugin) {
  @Volatile var connected = false
    private set

  /** Shares the namespace when WorldAPI is installed. Without it nothing happens, and an old one gets one warning. */
  fun connect() {
    if (plugin.server.pluginManager.getPlugin(WORLD_API) == null) return
    try {
      StatsExpansion.share { plugin.stats.tally }
      connected = true
      plugin.logger.info("WorldAPI found: cans, caffeine, overdoses, and flight at /extensions/${StatsExpansion.id()}")
    } catch (error: ExceptionInInitializerError) {
      plugin.logger.warning("WorldAPI didn't take Red Bull's numbers: ${error.cause?.message}")
    } catch (error: LinkageError) {
      // WorldAPI before 4.5.0 has no expansion API 2.
      plugin.logger.warning("WorldAPI is too old for Red Bull's numbers: it needs 4.5.0 or newer")
    } catch (error: Exception) {
      plugin.logger.warning("WorldAPI didn't take Red Bull's numbers: ${error.message}")
    }
  }

  fun disconnect() {
    if (!connected) return
    connected = false
    runCatching { StatsExpansion.unregister() }
  }

  private companion object {
    const val WORLD_API = "WorldAPI"
  }
}
