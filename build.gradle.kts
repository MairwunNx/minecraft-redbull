plugins {
  kotlin("jvm") version "2.4.21" apply false
  kotlin("plugin.serialization") version "2.4.21" apply false
  id("com.gradleup.shadow") version "9.6.1" apply false
  id("com.modrinth.minotaur") version "2.10.0" apply false
  id("io.papermc.hangar-publish-plugin") version "0.1.4" apply false
}

allprojects {
  group = "ru.mairwunnx"
  // The release workflow passes the version; every other build is a dev build.
  version = (findProperty("releaseVersion") as String?)?.takeIf { it.isNotBlank() } ?: "dev"

  repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
  }
}
