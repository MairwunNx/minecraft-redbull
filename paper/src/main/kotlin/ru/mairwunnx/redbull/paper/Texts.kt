package ru.mairwunnx.redbull.paper

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

/**
 * Every text players read is a translation key: the resource pack carries the languages, and the
 * client picks its own. The English text rides along as the fallback, for players without the pack.
 */
object Texts {
  private val english: Map<String, String> by lazy {
    val text = Texts::class.java.getResourceAsStream("/assets/redbull/lang/en_us.json")!!.use { it.readBytes().decodeToString() }
    Settings.parseTexts(text)
  }

  fun text(key: String, vararg args: ComponentLike): Component =
    Component.translatable(key, english[key], *args)

  /** A line of an item's name or tooltip: not italic, as names the game gives are not. */
  fun line(key: String, color: NamedTextColor? = null, italic: Boolean = false): Component =
    text(key).color(color).decoration(TextDecoration.ITALIC, italic)
}
