plugins {
  kotlin("jvm")
  id("net.neoforged.moddev")
}

// The game tests both mods run (`src/gametest`): Fabric compiles them with Fabric API's `GameTest` annotation, NeoForge
// with `src/gametestPlain`'s, and here they compile once with the plain one, checked against the game alone.
val gametest: SourceSet = sourceSets.create("gametest") {
  compileClasspath += sourceSets.main.get().output
}
kotlin.sourceSets.named("gametest") {
  kotlin.srcDir("src/gametestPlain/kotlin")
}

// `data`: writes the mods' recipes from core's cans as the game's JSON; each mod runs it into its own build, through
// `recipeGenerator`.
val data: SourceSet = sourceSets.create("data")
val dataJar = tasks.register<Jar>("dataJar") {
  archiveClassifier = "data"
  from(data.output)
}
configurations.consumable("recipeGenerator") {
  extendsFrom(configurations["dataImplementation"])
  outgoing.artifact(dataJar)
}

// The game without a loader, as both mods see it: NeoForm's Minecraft, whose names are the game's own since 26.1.
// Whatever reads or changes the world the same way on Fabric and NeoForge lives here; `fabric` and `neoforge` hand in
// what their loader does differently (see `Loader`).
neoForge {
  neoFormVersion = "26.3-1"
  addModdingDependenciesTo(gametest)
}

dependencies {
  implementation(project(":core"))
  // Mixins: Fabric Loader and NeoForge both bring Mixin at runtime.
  compileOnly("net.fabricmc:sponge-mixin:0.17.5+mixin.0.8.7")
  "gametestImplementation"(project(":core"))
  "dataImplementation"(project(":core"))
}

kotlin {
  jvmToolchain(25)
}
