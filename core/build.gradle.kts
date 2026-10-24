plugins {
  kotlin("jvm")
}

dependencies {
  // Allay's expansion API: its classes come with Allay, so it's never in a jar of ours.
  compileOnly("chat.allay:allay-expansion:1.3.0")
  testImplementation("chat.allay:allay-expansion:1.3.0")
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
