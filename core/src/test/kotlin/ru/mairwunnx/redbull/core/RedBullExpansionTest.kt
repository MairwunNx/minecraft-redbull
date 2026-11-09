package ru.mairwunnx.redbull.core

import chat.allay.expansion.AllayApi
import chat.allay.expansion.AllayExpansion
import chat.allay.expansion.AllayExpansions
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

@Suppress("DEPRECATION")
class RedBullExpansionTest {
  /** Red Bull's expansion as Allay's host reads it: API 2's declarations as tools, and its answers as plain values. */
  private val allay: AllayExpansion by lazy {
    var found: AllayExpansion? = null
    AllayExpansions.bind(object : AllayExpansions.Host {
      override fun register(expansion: AllayExpansion) { found = expansion }
      override fun unregister(id: String) = Unit
      override fun apiVersion(): String = AllayApi.VERSION
    })
    try {
      RedBullExpansion.register()
    } finally {
      RedBullExpansion.unregister()
      AllayExpansions.bind(null)
    }
    found!!
  }

  private val recipes get() = tool("recipes")

  private fun tool(name: String) = allay.tools().single { it.name() == name }

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

  private fun run(vararg args: Pair<String, Any>): ToolResult = recipes.run(call(*args))

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
    assertEquals("26 cans", result.summary())
    assertTrue(json(allay.promptExpansion()).length < 2000)
  }

  @Test
  fun `a can by id, name, or flavour`() {
    assertEquals("redbull:cherry", cans(run("can" to "redbull:cherry")).single()["id"])
    assertEquals("redbull:cherry", cans(run("can" to "Red Bull Cherry Edition")).single()["id"])
    val sakura = run("can" to "cherry-sakura")
    assertEquals(listOf("redbull:cherry", "redbull:spring"), cans(sakura).map { it["id"] })
    assertEquals("Red Bull Cherry Edition, Red Bull Spring Edition", sakura.summary())
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
    assertEquals("redbull", allay.id())
    assertEquals("Red Bull", allay.name())
    assertTrue(AllayApi.satisfies(AllayApi.VERSION, allay.requiresApi()))
    assertEquals(listOf("recipes", "caffeine", "collection"), allay.tools().map { it.name() })
    allay.tools().forEach {
      assertTrue(it.description().length <= 1000, it.name())
      assertEquals(chat.allay.expansion.AllayTool.Access.READ, it.access(), it.name())
    }
    // The caffeine and the collection are the asker's own, never for everyone online to read.
    assertEquals(listOf(true, false, false), allay.tools().map { it.inPublic() })
    assertEquals("""{"type":"object","properties":{}}""", tool("caffeine").parameters().toJson())
    val tool = recipes
    assertEquals("""{"type":"object","properties":{"can":{"type":"string","description":"One can: its id like redbull:cherry, or its name or flavour like Cherry Edition"}}}""", tool.parameters().toJson())
    // Allay finds the object through API 2's services file.
    val services = RedBullExpansion::class.java.getResource("/META-INF/services/chat.allay.expansion.v2.Expansion")!!.readText().trim()
    assertEquals(RedBullExpansion::class.java.name, services)
  }

  @Test
  fun `a can in full keeps its effects and chests as before`() {
    val ruby = cans(run("can" to "ruby")).single()
    @Suppress("UNCHECKED_CAST")
    val chests = ruby["chests"] as Map<String, Any>
    assertEquals(listOf("biomes", "chance"), chests.keys.toList())
    @Suppress("UNCHECKED_CAST")
    val effect = (ruby["effects"] as List<Map<String, Any>>).first()
    assertEquals(listOf("effect", "level", "seconds"), effect.keys.toList())
    assertEquals(listOf("id", "name", "flavour", "chest_only", "effects", "wings_seconds", "sugarfree", "chests"), ruby.keys.toList())
    assertEquals(listOf("ingredients", "grid", "names", "cans", "elsewhere"), run().data().keys.toList())
    assertEquals(listOf("ingredients", "grid", "names", "cans"), run("can" to "ruby").data().keys.toList())
  }

  @Test
  fun `names come in the language Allay answers in, not the game's`() {
    // The owner's game is in English, and Allay answers him in Russian: taurine is «Таурин», never a guess.
    val result = recipes.run(call(asker = asker("en_us", "ru_ru")))
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
    assertEquals("redbull:original", cans(recipes.run(call("can" to "оригинальный вкус", asker = asker("en_us", "ru_ru")))).single()["id"])
  }

  @Suppress("UNCHECKED_CAST")
  private fun names(asker: Asker) = recipes.run(call(asker = asker)).data()["names"] as Map<String, String>

  /** A platform with one player online, [asker()]'s UUID, with [caffeine], [held] cans, and [allTime]. */
  private fun online(caffeine: Caffeine, held: Set<String> = emptySet(), allTime: AllTime? = null) {
    RedBullInfo.players = object : RedBullPlayers {
      override fun caffeine(player: UUID) = caffeine.takeIf { player == UUID(0, 1) }
      override fun collection(player: UUID) = held.takeIf { player == UUID(0, 1) }
      override fun allTime(player: UUID) = allTime.takeIf { player == UUID(0, 1) }
    }
  }

  @Test
  fun `the caffeine tool tells the level, what more cans would do, and what's coming`() {
    val clock = 100_000L
    online(Caffeine(64.0, clock, sugar = 2, sugarUntil = clock + 3600))
    Crashes.forget(UUID(0, 1))
    try {
      val result = tool("caffeine").run(call())
      val data = result.data()
      assertEquals(listOf("level", "limit", "stage", "room_left", "safe_cans", "seconds_left", "sleepless", "next_can_crashes", "crash_in_seconds", "sugar_pit"), data.keys.toList())
      assertEquals(64, data["level"])
      assertEquals(100, data["limit"])
      assertEquals("jitters", data["stage"])
      assertEquals(36, data["room_left"])
      // 64 and one more can is 94: alive, in the crash. Two would kill.
      assertEquals(1, data["safe_cans"])
      assertEquals(true, data["next_can_crashes"])
      assertEquals(64 * 16, data["seconds_left"])
      assertEquals(true, data["sleepless"])
      assertEquals(null, data["crash_in_seconds"])
      @Suppress("UNCHECKED_CAST")
      val pit = data["sugar_pit"] as Map<String, Any>
      // The pit waits for the caffeine, which outlasts the buffs here. Two cans of sugar make the second depth.
      assertEquals(mapOf("depth" to 2, "in_seconds" to 64 * 16, "hunger_seconds" to sugarPit(2)[0].seconds, "slowness_seconds" to sugarPit(2)[1].seconds), pit)
      assertEquals("caffeine 64", result.summary())
      Crashes.coming(UUID(0, 1), 90)
      assertEquals(90, tool("caffeine").run(call()).data()["crash_in_seconds"])
    } finally {
      Crashes.forget(UUID(0, 1))
      RedBullInfo.players = null
    }
  }

  @Test
  fun `a calm player has room for three cans and no pit without sugar`() {
    online(Caffeine(0.0, 0))
    try {
      val data = tool("caffeine").run(call()).data()
      assertEquals("calm", data["stage"])
      assertEquals(3, data["safe_cans"])
      assertEquals(false, data["next_can_crashes"])
      assertEquals(false, data["sleepless"])
      assertEquals(null, data["sugar_pit"])
    } finally {
      RedBullInfo.players = null
    }
  }

  @Test
  fun `the asker's tools refuse without a player in the world`() {
    RedBullInfo.players = null
    assertTrue("isn't running" in assertFailsWith<ToolException> { tool("caffeine").run(call()) }.message!!)
    online(Caffeine())
    try {
      val stranger = object : Asker by asker() {
        override fun uuid(): UUID = UUID(0, 2)
      }
      assertEquals("the asker isn't a player in the world", assertFailsWith<ToolException> { tool("caffeine").run(call(asker = stranger)) }.message)
      assertEquals("the asker isn't a player in the world", assertFailsWith<ToolException> { tool("collection").run(call(asker = stranger)) }.message)
    } finally {
      RedBullInfo.players = null
    }
  }

  @Test
  fun `the collection tool lists the cans held and where to find the rest`() {
    online(Caffeine(), setOf("original", "cherry"), AllTime(12, 960, 1, 340, "redbull:cherry"))
    try {
      val result = tool("collection").run(call(asker = asker("en_us", "ru_ru")))
      val data = result.data()
      assertEquals(listOf("held", "total", "cans", "missing", "all_time"), data.keys.toList())
      assertEquals(2, data["held"])
      assertEquals(CANS.size, data["total"])
      assertEquals(listOf("redbull:original", "redbull:cherry"), data["cans"])
      @Suppress("UNCHECKED_CAST")
      val missing = data["missing"] as List<Map<String, Any?>>
      assertEquals(CANS.size - 2, missing.size)
      val ruby = missing.single { it["id"] == "redbull:ruby" }
      assertEquals(listOf("id", "name", "flavour", "crafted", "chests"), ruby.keys.toList())
      assertEquals(false, ruby["crafted"])
      @Suppress("UNCHECKED_CAST")
      assertEquals(listOf("biomes", "chance"), (ruby["chests"] as Map<String, Any>).keys.toList())
      val zero = missing.single { it["id"] == "redbull:zero" }
      assertEquals(true, zero["crafted"])
      assertEquals(null, zero["chests"])
      assertTrue(missing.none { it["flavour"] == it["name"] })
      assertEquals(mapOf("cans" to 12L, "caffeine_mg" to 960L, "overdoses" to 1L, "flight_seconds" to 340L, "favourite" to "redbull:cherry"), data["all_time"])
      assertEquals("2 of ${CANS.size} cans", result.summary())
      assertTrue(json(data).length < 8000, "${json(data).length} characters")
    } finally {
      RedBullInfo.players = null
    }
  }

  @Test
  fun `the prompt stays within Allay's limit and names every tool`() {
    val prompt = allay.promptExpansion()
    assertTrue(prompt.length <= 2000, "${prompt.length} characters")
    listOf("x_redbull_recipes", "x_redbull_caffeine", "x_redbull_collection", "Slow Falling", "bull mode", "sugar pit", "/redbull").forEach { assertTrue(it in prompt, it) }
    assertTrue(';' !in prompt)
  }

  @Test
  fun `the items name and describe every Red Bull item, with what Paper hides from Allay`() {
    try {
      RedBullInfo.paper = false
      val mods = allay.items()
      // The mods register every item and their recipes are data: names and descriptions only.
      assertEquals(CANS.size * 2 + 4, mods.size)
      assertTrue(mods.all { it.recipes().isEmpty() && it.base() == null })
      RedBullInfo.paper = true
      val paper = allay.items()
      assertEquals(CANS.size * 2 + 5, paper.size)
      assertEquals(paper.size, paper.map { it.id() }.toSet().size)
      for (item in paper) {
        assertTrue(item.description()!!.length <= 300, "${item.id()}: ${item.description()!!.length}")
        assertTrue(listOf("en_us", "ru_ru", "de_de", "es_es", "pt_br", "es_mx", "pt_pt").all { it in item.names() }, item.id())
        assertTrue(';' !in item.description()!!, item.id())
        assertTrue(item.base() != null, item.id())
      }
      val crude = paper.single { it.id() == "redbull:crude_taurine" }
      assertEquals("Сырой таурин", crude.names()["ru_ru"])
      val brew = crude.recipes().single()
      assertEquals(chat.allay.expansion.Recipe.Type.BREWING, brew.type())
      assertEquals(listOf("redbull:crude_taurine", "redbull:taurine"), paper.filter { it.recipes().isNotEmpty() }.map { it.id() })
      assertEquals(chat.allay.expansion.Recipe.Type.FURNACE, paper.single { it.id() == "redbull:taurine" }.recipes().single().type())
      assertEquals("ominous_bottle", paper.single { it.id() == "redbull:bull_bile" }.base())
      val cherry = paper.single { it.id() == "redbull:cherry" }
      assertEquals("Red Bull Cherry Edition", cherry.names()["en_us"])
      assertTrue("30 caffeine" in cherry.description()!!, cherry.description())
      assertTrue("Speed II for 720 s" in cherry.description()!!, cherry.description())
      assertEquals("Crumpled Can", paper.single { it.id() == "redbull:crumpled_cherry" }.names()["en_us"])
    } finally {
      RedBullInfo.paper = false
    }
  }
}
