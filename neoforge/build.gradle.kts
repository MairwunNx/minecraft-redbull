import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
  kotlin("jvm")
  id("net.neoforged.moddev")
  id("com.modrinth.minotaur")
}

val minecraft = "26.3"
val neoforgeVersion = "26.3.0.95"

val core = project(":core")
evaluationDependsOn(core.path)
val coreMain: SourceSet = core.sourceSets.main.get()

// `data`: writes the recipes from core's cans as the game's JSON; see Recipes.kt there.
val data: SourceSet = sourceSets.create("data")
// `gametest`: a dev-only mod with game tests for drinking, wings, and loot; never in the jar.
val gametest: SourceSet = sourceSets.create("gametest") {
  compileClasspath += sourceSets.main.get().output + coreMain.output
  runtimeClasspath += sourceSets.main.get().output + coreMain.output
}

neoForge {
  version = neoforgeVersion
  addModdingDependenciesTo(gametest)

  // The mod is this module and core together, in dev runs as in the jar.
  mods {
    register("redbull") {
      sourceSet(sourceSets.main.get())
      sourceSet(coreMain)
    }
    register("redbull_tests") {
      sourceSet(gametest)
    }
  }

  runs {
    register("client") {
      client()
      loadedMods = listOf(mods["redbull"])
    }
    register("server") {
      server()
      programArgument("--nogui")
      loadedMods = listOf(mods["redbull"])
    }
    // Runs every test and stops: `./gradlew :neoforge:runGameTestServer`.
    register("gameTestServer") {
      type = "gameTestServer"
      sourceSet = gametest
      gameDirectory = file("run/gametest")
      systemProperty("neoforge.enabledGameTestNamespaces", "redbull_tests")
    }
  }
}

dependencies {
  // core's classes go into the jar itself (see tasks.jar), so it isn't a library at runtime.
  compileOnly(project(":core"))
  "dataImplementation"(project(":core"))
  // Kotlin ships inside the jar, so players need no Kotlin mod; NeoForge picks the newest copy if another mod has one.
  jarJar("org.jetbrains.kotlin:kotlin-stdlib:${getKotlinPluginVersion()}") { isTransitive = false }
}

kotlin {
  jvmToolchain(25)
}

val generateData = tasks.register<JavaExec>("generateData") {
  val out = layout.buildDirectory.dir("generated/data")
  classpath = data.runtimeClasspath
  mainClass = "ru.mairwunnx.redbull.neoforge.data.RecipesKt"
  outputs.dir(out)
  argumentProviders += CommandLineArgumentProvider { listOf(out.get().asFile.absolutePath) }
}

sourceSets.main {
  resources.srcDir(generateData)
}

tasks.processResources {
  // FML wants a version number; a dev build is 0.0.0-dev.
  val version = project.version.toString().let { if (it == "dev") "0.0.0-dev" else it }
  val properties = mapOf("version" to version, "neoforge" to neoforgeVersion, "minecraft" to minecraft)
  inputs.properties(properties)
  filesMatching("META-INF/neoforge.mods.toml") { expand(properties) }
  from(rootProject.file("paper/src/pack/pack.png")) { rename { "redbull.png" } }
  // The game has no fallback between dialects: Mexico and Portugal get Spain's and Brazil's texts.
  val lang = core.layout.projectDirectory.dir("src/main/resources/assets/redbull/lang")
  from(lang.file("es_es.json")) { into("assets/redbull/lang"); rename { "es_mx.json" } }
  from(lang.file("pt_br.json")) { into("assets/redbull/lang"); rename { "pt_pt.json" } }
}

tasks.jar {
  archiveBaseName = "redbull-neoforge"
  from(coreMain.output)
}

// `./gradlew modrinth` with MODRINTH_TOKEN set; the release workflow runs it when asked to.
modrinth {
  token = providers.environmentVariable("MODRINTH_TOKEN")
  projectId = "minecraft-redbull"
  versionNumber = "${project.version}+neoforge"
  versionName = "Red Bull ${project.version} for NeoForge"
  versionType = "release"
  uploadFile.set(tasks.jar)
  gameVersions.addAll(minecraft)
  loaders.addAll("neoforge")
  changelog = (findProperty("changelogFile") as String?)?.let { rootProject.file(it).readText() }
    ?: "https://github.com/MairwunNx/minecraft-redbull/releases/tag/v${project.version}"
}
