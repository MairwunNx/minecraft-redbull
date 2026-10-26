import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
  kotlin("jvm")
  id("net.neoforged.moddev")
}

val minecraft = "26.3"
val neoforgeVersion = "26.3.0.99"

val core = project(":core")
val modcore = project(":modcore")
evaluationDependsOn(core.path)
evaluationDependsOn(modcore.path)
val coreMain: SourceSet = core.sourceSets.main.get()
val modcoreMain: SourceSet = modcore.sourceSets.main.get()

// `gametest`: a dev-only mod that runs modcore's game tests, shared with Fabric; never in the jar.
val gametest: SourceSet = sourceSets.create("gametest") {
  compileClasspath += sourceSets.main.get().output + coreMain.output + modcoreMain.output
  runtimeClasspath += sourceSets.main.get().output + coreMain.output + modcoreMain.output
  // The tests' data (a crate of another mod's) is modcore's, shared with Fabric.
  resources.srcDir(modcore.file("src/gametest/resources"))
}
kotlin.sourceSets.named("gametest") {
  kotlin.srcDir(modcore.file("src/gametest/kotlin"))
  kotlin.srcDir(modcore.file("src/gametestPlain/kotlin"))
}

neoForge {
  version = neoforgeVersion
  addModdingDependenciesTo(gametest)

  // The mod is this module, modcore, and core together, in dev runs as in the jar.
  mods {
    register("redbull") {
      sourceSet(sourceSets.main.get())
      sourceSet(modcoreMain)
      sourceSet(coreMain)
    }
    register("redbull_gametest") {
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
      systemProperty("neoforge.enabledGameTestNamespaces", "redbull_gametest")
    }
  }
}

dependencies {
  // core's and modcore's classes go into the jar itself (see tasks.jar), so they aren't libraries at runtime.
  compileOnly(project(":core"))
  // Without its Minecraft: the game comes from NeoForge here.
  compileOnly(project(":modcore")) { isTransitive = false }
  // Kotlin ships inside the jar, so players need no Kotlin mod; NeoForge picks the newest copy if another mod has one.
  jarJar("org.jetbrains.kotlin:kotlin-stdlib:${getKotlinPluginVersion()}") { isTransitive = false }
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
  val out = layout.buildDirectory.dir("generated/data")
  classpath = recipeGenerator
  mainClass = "ru.mairwunnx.redbull.modcore.data.RecipesKt"
  outputs.dir(out)
  argumentProviders += CommandLineArgumentProvider { listOf(out.get().asFile.absolutePath, "neoforge") }
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
  // The mod list shows the store's icon, as Fabric's does.
  from(rootProject.file("media/icon.png")) { rename { "redbull.png" } }
  // The game has no fallback between dialects: Mexico and Portugal get Spain's and Brazil's texts.
  val lang = core.layout.projectDirectory.dir("src/main/resources/assets/redbull/lang")
  from(lang.file("es_es.json")) { into("assets/redbull/lang"); rename { "es_mx.json" } }
  from(lang.file("pt_br.json")) { into("assets/redbull/lang"); rename { "pt_pt.json" } }
}

tasks.jar {
  archiveBaseName = "redbull-neoforge"
  from(modcoreMain.output, coreMain.output)
}

