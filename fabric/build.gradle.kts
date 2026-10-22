plugins {
  kotlin("jvm")
  id("net.fabricmc.fabric-loom")
}

val minecraft = "26.3"
val loader = "0.19.5"
val fabricApi = "0.164.0+26.3"
val fabricKotlin = "1.14.2+kotlin.2.4.21"

base {
  archivesName = "redbull-fabric"
}

loom {
  // Anything only a client draws goes in the client source set, so a dedicated server never loads a client class.
  splitEnvironmentSourceSets()
  mods.register("redbull") {
    sourceSet(sourceSets.main.get())
    sourceSet(sourceSets.getByName("client"))
  }
}

// Game tests drink, craft, brew, and loot on a real server with every build (`./gradlew :fabric:runGameTest`);
// `./gradlew :fabric:runClientGameTest` opens a game window and screenshots the HUD. They live in `src/gametest`
// as a mod of their own, so the release jar doesn't carry them.
fabricApi {
  configureTests {
    createSourceSet = true
    modId = "redbull-gametest"
    enableGameTests = true
    enableClientGameTests = true
    eula = true
  }
}

dependencies {
  minecraft("com.mojang:minecraft:$minecraft")
  implementation("net.fabricmc:fabric-loader:$loader")
  implementation("net.fabricmc.fabric-api:fabric-api:$fabricApi")
  implementation("net.fabricmc:fabric-language-kotlin:$fabricKotlin")
  implementation(project(":core"))
}

kotlin {
  jvmToolchain(25)
}

// The recipes come from core, as on Paper: a small program writes them as the game's data before every build.
val generator: SourceSet = sourceSets.create("generator")
dependencies {
  "generatorImplementation"(project(":core"))
}

val generateData = tasks.register<JavaExec>("generateData") {
  val output = layout.buildDirectory.dir("generated/data")
  classpath = generator.runtimeClasspath
  mainClass = "ru.mairwunnx.redbull.fabric.generator.RecipesKt"
  args(output.get().asFile.absolutePath)
  outputs.dir(output)
}

// Core's assets ship in the mod: the textures, models, item definitions, and languages every platform shares.
sourceSets.main {
  resources.srcDir(project(":core").layout.projectDirectory.dir("src/main/resources"))
  resources.srcDir(generateData)
}

tasks.processResources {
  val version = project.version.toString()
  val versions = mapOf("version" to version, "minecraft" to minecraft, "loader" to loader, "kotlin" to fabricKotlin)
  inputs.properties(versions)
  filesMatching("fabric.mod.json") {
    expand(versions)
  }
  from(rootProject.file("media/icon.png")) { into("assets/redbull") }
  // The game has no fallback between dialects: Mexico and Portugal get Spain's and Brazil's texts, as on NeoForge.
  val lang = project(":core").layout.projectDirectory.dir("src/main/resources/assets/redbull/lang")
  from(lang.file("es_es.json")) { into("assets/redbull/lang"); rename { "es_mx.json" } }
  from(lang.file("pt_br.json")) { into("assets/redbull/lang"); rename { "pt_pt.json" } }
}

// Core is plain Kotlin, not a mod: its classes go into the mod's jar. Fabric Language Kotlin brings Kotlin itself.
val core = project(":core")
tasks.jar {
  dependsOn(core.tasks.named("classes"))
  from(core.layout.buildDirectory.dir("classes/kotlin/main"))
}

