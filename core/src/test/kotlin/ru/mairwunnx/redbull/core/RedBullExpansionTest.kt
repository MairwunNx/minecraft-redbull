package ru.mairwunnx.redbull.core

import chat.allay.expansion.AllayApi
import chat.allay.expansion.Args
import chat.allay.expansion.Asker
import chat.allay.expansion.ToolCall
import chat.allay.expansion.ToolException
import chat.allay.expansion.ToolResult
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RedBullExpansionTest {
  /** A player whose game is in [locale] and who reads Allay in [language]. */
  private fun asker(locale: String = "en_us", language: String = locale) = object : Asker {
    override fun uuid(): UUID = UUID(0, 1)
    override fun name() = "MairwunNx"
    override fun locale() = locale
    override fun language() = language
    override fun isOperator() = false
    override fun hasPermission(permission: String, byDefault: Boolean) = byDefault
    override fun dimension() = "overworld"
    override fun x() = 0
    override fun y() = 64
    override fun z() = 0
    override fun <T> unwrap(type: Class<T>): T? = null
  }

  private fun call(vararg args: Pair<String, Any>, asker: Asker = asker()) = object : ToolCall {
    override fun asker(): Asker = asker
    override fun args() = Args(mapOf(*args))
    override fun mode() = ToolCall.Mode.ASK
    override fun isPublic() = false
    override fun nameOf(player: UUID, name: String) = name
    override fun canSee(player: UUID) = true
  }

  private fun run(vararg args: Pair<String, Any>): ToolResult = RedBullExpansion.Recipes.run(call(*args))

  @Suppress("UNCHECKED_CAST")
  private fun cans(result: ToolResult) = result.data()["cans"] as List<Map<String, Any>>

  /** As Allay measures a result: its JSON, which it cuts at 8000 characters. */
  private fun json(value: Any?): String = when (value) {
    is Map<*, *> -> value.entries.joinToString(",", "{", "}") { "\"${it.key}\":${json(it.value)}" }
    is List<*> -> value.joinToString(",", "[", "]") { json(it) }
    is String -> "\"$value\""
    else -> value.toString()
  }

  @Test
  fun `every can in short fits Allay's limit`() {
    val result = run()
    assertEquals(CANS.size, cans(result).size)
    val size = json(result.data()).length
    assertTrue(size < 8000, "$size characters")
    assertTrue(json(RedBullExpansion().promptExpansion()).length < 2000)
  }

  @Test
  fun `a can by id, name, or flavour`() {
    assertEquals("redbull:cherry", cans(run("can" to "redbull:cherry")).single()["id"])
    assertEquals("redbull:cherry", cans(run("can" to "Red Bull Cherry Edition")).single()["id"])
    assertEquals(listOf("redbull:cherry", "redbull:spring"), cans(run("can" to "cherry-sakura")).map { it["id"] })
    assertEquals(listOf("redbull:winter_apple", "redbull:winter_pistachio"), cans(run("can" to "Winter Edition")).map { it["id"] })
  }

  @Test
  fun `a can in full has its recipe, or chests for a legend`() {
    val original = cans(run("can" to "original")).single()
    assertEquals("Red Bull Energy Drink", original["name"])
    @Suppress("UNCHECKED_CAST")
    val key = (original["recipe"] as Map<String, Any>)["key"] as Map<String, String>
    assertEquals("blue_dye", key["D"])
    assertEquals("sugar", key["S"])
    val ruby = cans(run("can" to "ruby")).single()
    assertEquals(true, ruby["chest_only"])
    assertTrue("chests" in ruby && "recipe" !in ruby)
  }

  @Test
  fun `an unknown can is refused with the ones there are`() {
    val refusal = assertFailsWith<ToolException> { run("can" to "monster") }
    assertTrue("redbull:original" in refusal.message!!)
  }

  @Test
  fun `the expansion is what Allay checks`() {
    val expansion = RedBullExpansion()
    assertTrue(expansion.id().matches(Regex("[a-z0-9_]{1,24}")))
    // 1.0, so an older Allay still runs it; 1.3's Asker.language only where Allay has it.
    assertEquals("1.0.0", expansion.requiresApi())
    assertTrue(AllayApi.satisfies(AllayApi.VERSION, expansion.requiresApi()))
    expansion.tools().forEach {
      assertTrue(it.name().matches(Regex("[a-z0-9_]{1,32}")))
      assertTrue(it.description().length <= 1000)
    }
  }

  @Test
  fun `names come in the language Allay answers in, not the game's`() {
    // The owner's game is in English, and Allay answers him in Russian: taurine is «Таурин», never a guess.
    val result = RedBullExpansion.Recipes.run(call(asker = asker("en_us", "ru_ru")))
    @Suppress("UNCHECKED_CAST")
    val names = result.data()["names"] as Map<String, String>
    assertEquals("Таурин", names["redbull:taurine"])
    assertEquals("Сырой таурин", names["redbull:crude_taurine"])
    assertEquals("Бычья желчь", names["redbull:bull_bile"])
    assertEquals("Пустая банка", names["redbull:empty_can"])
    val original = cans(result).first { it["id"] == "redbull:original" }
    assertEquals("Red Bull Energy Drink", original["name"])
    assertEquals("Оригинальный вкус", original["flavour"])
    assertTrue(json(result.data()).length < 8000, "${json(result.data()).length} characters")
    // English for an English reader; another Spanish reads Spain's; an unknown language falls back to English.
    assertEquals("Taurine", names(asker("ru_ru", "en_us"))["redbull:taurine"])
    assertEquals(names(asker("es_es"))["redbull:taurine"], names(asker("es_mx"))["redbull:taurine"])
    assertEquals("Taurine", names(asker("xx_yy"))["redbull:taurine"])
    // A flavour in the reader's language finds its can.
    assertEquals("redbull:original", cans(RedBullExpansion.Recipes.run(call("can" to "оригинальный вкус", asker = asker("en_us", "ru_ru")))).single()["id"])
  }

  @Suppress("UNCHECKED_CAST")
  private fun names(asker: Asker) = RedBullExpansion.Recipes.run(call(asker = asker)).data()["names"] as Map<String, String>
}
