package ru.mairwunnx.redbull.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssetsTest {
  private val items = CANS.flatMap { listOf(it.id, it.crumpled) } + listOf(Ids.EMPTY_CAN, Ids.WINGED_CAN, Ids.TAURINE)
  private val languages = listOf("en_us", "ru_ru", "de_de", "es_es", "pt_br")

  private fun resource(path: String) = javaClass.getResource("/assets/redbull/$path")

  private fun keys(language: String): Set<String> {
    val text = assertNotNull(resource("lang/$language.json"), language).readText()
    return Regex("^\\s*\"([^\"]+)\":", RegexOption.MULTILINE).findAll(text).map { it.groupValues[1] }.toSet()
  }

  @Test
  fun `every item has a texture, a model, and an item definition`() {
    items.forEach { id ->
      assertNotNull(resource("textures/item/$id.png"), "texture of $id")
      assertNotNull(resource("models/item/$id.json"), "model of $id")
      assertNotNull(resource("items/$id.json"), "item definition of $id")
    }
  }

  @Test
  fun `every can has a name, a flavour, and a slogan`() {
    val english = keys("en_us")
    CANS.forEach { can ->
      listOf(can.translation, "${can.translation}.flavour", can.slogan)
        .forEach { assertTrue(it in english, "$it is missing") }
    }
  }

  /** The game names a death `<key>.player` when something hurt the player shortly before; the mods die that way. */
  @Test
  fun `every death message has a twin for a death in a fight`() {
    val english = keys("en_us")
    english.filter { it.startsWith("death.attack.") && !it.endsWith(".player") }
      .forEach { assertTrue("$it.player" in english, "$it.player is missing") }
  }

  @Test
  fun `every language has the same keys`() {
    val english = keys("en_us")
    languages.forEach { assertEquals(english, keys(it), it) }
  }
}
