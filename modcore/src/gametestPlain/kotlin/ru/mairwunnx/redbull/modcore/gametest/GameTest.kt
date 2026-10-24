package ru.mairwunnx.redbull.modcore.gametest

/**
 * The game tests' annotation, for a loader without one of its own: NeoForge's test mod finds the tests by it. Fabric
 * compiles the same tests with Fabric API's annotation under this name instead.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GameTest(val environment: String = "minecraft:default", val maxTicks: Int = 20)
