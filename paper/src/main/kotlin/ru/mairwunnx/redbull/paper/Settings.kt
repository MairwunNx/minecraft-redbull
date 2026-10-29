package ru.mairwunnx.redbull.paper

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import ru.mairwunnx.redbull.core.SugarPitTexts

/** `config.yml`. Every field has a default, so a config that sets only a few keys still loads. */
@Serializable
data class Settings(
  @SerialName("resource_pack") val resourcePack: ResourcePack = ResourcePack(),
  val overdose: Boolean = true,
  val wings: Boolean = true,
  /** `arcade` or `feelings`; anything else is `arcade`. */
  @SerialName("sugar_pit_texts") val sugarPitTextsId: String = SugarPitTexts.ARCADE.id,
  val loot: Loot = Loot(),
) {
  val sugarPitTexts get() = SugarPitTexts.of(sugarPitTextsId) ?: SugarPitTexts.ARCADE

  @Serializable
  data class ResourcePack(
    val enabled: Boolean = true,
    /** Empty: the pack attached to this version's GitHub release. */
    val url: String = "",
    val required: Boolean = false,
  )

  @Serializable
  data class Loot(
    val bile: Double = 1.0,
    val chests: Double = 1.0,
  )

  companion object {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    fun parse(text: String): Settings = yaml.decodeFromString(serializer(), text)

    /** A flat JSON language file; JSON is YAML, so kaml reads it too. */
    fun parseTexts(text: String): Map<String, String> =
      yaml.decodeFromString(MapSerializer(String.serializer(), String.serializer()), text)
  }
}
