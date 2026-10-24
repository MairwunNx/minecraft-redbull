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
  private fun call(vararg args: Pair<String, Any>) = object : ToolCall {
    override fun asker(): Asker = error("the tool reads no asker")
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
    assertEquals(AllayApi.VERSION, expansion.requiresApi())
    expansion.tools().forEach {
      assertTrue(it.name().matches(Regex("[a-z0-9_]{1,32}")))
      assertTrue(it.description().length <= 1000)
    }
  }
}
