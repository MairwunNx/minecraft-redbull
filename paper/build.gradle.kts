import io.papermc.hangarpublishplugin.model.Platforms

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("com.gradleup.shadow")
  id("com.modrinth.minotaur")
  id("io.papermc.hangar-publish-plugin")
}

dependencies {
  implementation(project(":core"))
  compileOnly("io.papermc.paper:paper-api:26.3.build.205-stable")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
  implementation("com.charleskorn.kaml:kaml:0.104.0")

  testImplementation("io.papermc.paper:paper-api:26.3.build.205-stable")
  testImplementation(kotlin("test"))
  testImplementation(platform("org.junit:junit-bom:6.1.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  jvmToolchain(25)
}

tasks.test {
  useJUnitPlatform()
}

// The resource pack: core's assets and this module's pack.mcmeta. The jar carries it to know its hash,
// and the release attaches the same file for players to download. Byte for byte the same on every build.
val pack = tasks.register<Zip>("pack") {
  val assets = project(":core").layout.projectDirectory.dir("src/main/resources")
  archiveFileName = "redbull-pack.zip"
  destinationDirectory = layout.buildDirectory.dir("pack")
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
  from(assets) { include("assets/**") }
  from("src/pack")
  // The game has no fallback between dialects: Mexico and Portugal get Spain's and Brazil's texts.
  from(assets.file("assets/redbull/lang/es_es.json")) { into("assets/redbull/lang"); rename { "es_mx.json" } }
  from(assets.file("assets/redbull/lang/pt_br.json")) { into("assets/redbull/lang"); rename { "pt_pt.json" } }
}

// The data pack the bootstrapper registers: core's data (the advancements) under a pack of its own.
val datapack = tasks.register<Sync>("datapack") {
  into(layout.buildDirectory.dir("datapack/datapack"))
  from(project(":core").layout.projectDirectory.dir("src/main/resources")) { include("data/**") }
  from("src/datapack")
}

tasks.processResources {
  val version = project.version.toString()
  inputs.property("version", version)
  filesMatching("paper-plugin.yml") {
    expand("version" to version)
  }
  from(pack)
  from(datapack.map { it.destinationDir.parentFile })
}

tasks.shadowJar {
  archiveBaseName = "redbull-paper"
  archiveClassifier = ""
  mergeServiceFiles()
  // Relocated so other plugins that ship their own Kotlin or kaml can't clash with ours.
  listOf("kotlin", "kotlinx", "com.charleskorn.kaml", "it.krzeminski.snakeyaml", "okio", "net.thauvin")
    .forEach { relocate(it, "ru.mairwunnx.redbull.libs.$it") }
  // The pack carries the assets; the jar keeps only the English texts, as fallbacks for players without it.
  exclude("assets/redbull/textures/**", "assets/redbull/models/**", "assets/redbull/items/**", "data/**")
  exclude { it.path.startsWith("assets/redbull/lang/") && it.name != "en_us.json" }
  exclude("META-INF/*.kotlin_module", "META-INF/versions/*/module-info.class", "module-info.class")
  dependencies { exclude(dependency("org.jetbrains:annotations")) }
}

tasks.jar { enabled = false }
tasks.assemble { dependsOn(tasks.shadowJar) }

// `./gradlew modrinth` with MODRINTH_TOKEN set; the release workflow runs it when asked to.
modrinth {
  token = providers.environmentVariable("MODRINTH_TOKEN")
  projectId = "minecraft-redbull"
  versionNumber = "${project.version}+paper"
  versionName = "Red Bull ${project.version} for Paper"
  versionType = "release"
  uploadFile.set(tasks.shadowJar)
  gameVersions.addAll("26.3")
  loaders.addAll("paper", "purpur")
  // The release workflow writes the notes to a file; a local upload falls back to the GitHub release.
  changelog = (findProperty("changelogFile") as String?)?.let { rootProject.file(it).readText() }
    ?: "https://github.com/MairwunNx/minecraft-redbull/releases/tag/v${project.version}"
  syncBodyFrom = rootProject.file("MODRINTH.md").takeIf { it.exists() }?.readText()
}

tasks.modrinth {
  dependsOn(tasks.modrinthSyncBody)
}

// `./gradlew publishPluginPublicationToHangar` with HANGAR_TOKEN set; the release workflow runs it when asked to.
hangarPublish {
  publications.register("plugin") {
    version = project.version.toString()
    id = "RedBull"
    channel = "Release"
    apiKey = providers.environmentVariable("HANGAR_TOKEN")
    changelog = (findProperty("changelogFile") as String?)?.let { rootProject.file(it).readText() }
      ?: "https://github.com/MairwunNx/minecraft-redbull/releases/tag/v${project.version}"
    // The project page follows MODRINTH.md, as Modrinth's does; the release syncs it with the version.
    pages {
      resourcePage(providers.provider { rootProject.file("MODRINTH.md").readText() })
    }
    platforms {
      register(Platforms.PAPER) {
        jar = tasks.shadowJar.flatMap { it.archiveFile }
        platformVersions = listOf("26.3")
      }
    }
  }
}
