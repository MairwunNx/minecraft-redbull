package ru.mairwunnx.redbull.paper

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

/** Registers the data pack inside the jar, with the advancements core describes, before the worlds load. */
class RedBullBootstrap : PluginBootstrap {
  override fun bootstrap(context: BootstrapContext) {
    context.lifecycleManager.registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY) { event ->
      event.registrar().discoverPack(javaClass.getResource("/datapack")!!.toURI(), "redbull")
    }
  }
}
