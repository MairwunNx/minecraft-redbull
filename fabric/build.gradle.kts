plugins {
  kotlin("jvm")
  id("net.fabricmc.fabric-loom")
}

val minecraft = "26.3"
val loader = "0.20.1"
val fabricApi = "0.165.0+26.3"
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
    modId = "redbull_gametest"
    enableGameTests = true
    enableClientGameTests = true
    eula = true
  }
}

val core = project(":core")
val modcore = project(":modcore")
evaluationDependsOn(modcore.path)

// The game tests are modcore's, shared with NeoForge; Fabric API's `GameTest` stands in for the annotation they name.
kotlin.sourceSets.named("gametest") {
  kotlin.srcDir(modcore.file("src/gametest/kotlin"))
}
// The tests' data (a crate of another mod's) is modcore's too.
sourceSets.named("gametest") {
  resources.srcDir(modcore.file("src/gametest/resources"))
}

dependencies {
  minecraft("com.mojang:minecraft:$minecraft")
  implementation("net.fabricmc:fabric-loader:$loader")
  implementation("net.fabricmc.fabric-api:fabric-api:$fabricApi")
  implementation("net.fabricmc:fabric-language-kotlin:$fabricKotlin")
  implementation(project(":core"))
  // Without its Minecraft: the game comes from Loom here.
  implementation(project(":modcore")) { isTransitive = false }
}

kotlin {
  jvmToolchain(25)
}

// The recipes come from core, as on Paper: modcore's small program writes them as the game's data before every build.
val recipeGenerator: Configuration by configurations.creating {
  attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME)) }
}
dependencies {
  recipeGenerator(project(":modcore", "recipeGenerator"))
}
val generateData = tasks.register<JavaExec>("generateData") {
  val output = layout.buildDirectory.dir("generated/data")
  classpath = recipeGenerator
  mainClass = "ru.mairwunnx.redbull.modcore.data.RecipesKt"
  args(output.get().asFile.absolutePath, "fabric")
  outputs.dir(output)
}

// Core's assets ship in the mod: the textures, models, item definitions, and languages every platform shares; and
// modcore's data and mixins, which both mods share.
sourceSets.main {
  resources.srcDir(core.layout.projectDirectory.dir("src/main/resources"))
  resources.srcDir(modcore.layout.projectDirectory.dir("src/main/resources"))
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
  val lang = core.layout.projectDirectory.dir("src/main/resources/assets/redbull/lang")
  from(lang.file("es_es.json")) { into("assets/redbull/lang"); rename { "es_mx.json" } }
  from(lang.file("pt_br.json")) { into("assets/redbull/lang"); rename { "pt_pt.json" } }
}

// Core and modcore aren't mods: their classes go into the mod's jar. Fabric Language Kotlin brings Kotlin itself.
tasks.jar {
  dependsOn(core.tasks.named("classes"), modcore.tasks.named("classes"))
  from(core.layout.buildDirectory.dir("classes/kotlin/main"))
  from(modcore.layout.buildDirectory.dir("classes/kotlin/main"), modcore.layout.buildDirectory.dir("classes/java/main"))
}

