pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "redbull"

// core: the rules and the assets every platform shares; Paper, NeoForge, and Fabric each build on it. modcore is what
// both mods share, the game without a loader; neoforge and fabric are thin adapters around it.
include("core", "paper", "modcore", "neoforge", "fabric")
