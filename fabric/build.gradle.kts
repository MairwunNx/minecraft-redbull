plugins {
  kotlin("jvm")
  id("net.fabricmc.fabric-loom")
  id("com.modrinth.minotaur")
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
}

// Core is plain Kotlin, not a mod: its classes go into the mod's jar. Fabric Language Kotlin brings Kotlin itself.
val core = project(":core")
tasks.jar {
  dependsOn(core.tasks.named("classes"))
  from(core.layout.buildDirectory.dir("classes/kotlin/main"))
}

// `./gradlew modrinth` with MODRINTH_TOKEN set uploads this jar next to Paper's, as the Fabric version of the same
// project; the release workflow runs it when asked to. Paper's module syncs the description.
modrinth {
  token = providers.environmentVariable("MODRINTH_TOKEN")
  projectId = "minecraft-redbull"
  versionNumber = "${project.version}+fabric"
  versionName = "Red Bull ${project.version} for Fabric"
  versionType = "release"
  uploadFile.set(tasks.jar)
  gameVersions.addAll(minecraft)
  loaders.addAll("fabric")
  // The release workflow writes the notes to a file; a local upload falls back to the GitHub release.
  changelog = (findProperty("changelogFile") as String?)?.let { rootProject.file(it).readText() }
    ?: "https://github.com/MairwunNx/minecraft-redbull/releases/tag/v${project.version}"
  dependencies {
    required.project("fabric-api")
    required.project("fabric-language-kotlin")
  }
}
