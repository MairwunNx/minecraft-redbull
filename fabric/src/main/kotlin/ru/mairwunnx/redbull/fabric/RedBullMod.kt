package ru.mairwunnx.redbull.fabric

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object RedBullMod : ModInitializer {
  val log = LoggerFactory.getLogger("Red Bull")

  override fun onInitialize() {
    log.info("Red Bull is on")
  }
}
