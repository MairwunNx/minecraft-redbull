plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "redbull"

// core: the rules and the assets every platform shares. Each platform is a module next to it;
// NeoForge and Fabric join as `include("neoforge")` and `include("fabric")`, as CLAUDE.md says.
include("core", "paper")
include("neoforge")
