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

// core: the rules and the assets every platform shares. Each platform is a module next to it;
// NeoForge joins as `include("neoforge")`, as CLAUDE.md says.
include("core", "paper", "fabric")
