package ru.mairwunnx.redbull.neoforge

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.animal.cow.AbstractCow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.loot.IGlobalLootModifier
import net.neoforged.neoforge.common.loot.LootModifier
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import ru.mairwunnx.redbull.core.Bile
import ru.mairwunnx.redbull.core.CANS
import java.util.Optional
import net.minecraft.core.Holder

/**
 * Where ingredients and cans come from besides crafting: bull bile from cows a player kills, and cans in
 * chests by the biome the chest stands in, whatever structure it belongs to. Two global loot modifiers,
 * switched on by `data/redbull/loot_modifiers/`; the chances come from `core`.
 */
object Loot {
  private val SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID)

  // One codec each: the game finds a modifier's type by its codec.
  private val BULL_BILE: MapCodec<BullBile> = Modifier.codec(::BullBile)
  private val CANS_CODEC: MapCodec<Cans> = Modifier.codec(::Cans)

  init {
    SERIALIZERS.register("bull_bile") { -> BULL_BILE }
    SERIALIZERS.register("cans") { -> CANS_CODEC }
  }

  fun register(bus: IEventBus) = SERIALIZERS.register(bus)

  abstract class Modifier(condition: Optional<Holder<LootItemCondition>>, priority: Int) : LootModifier(condition, priority) {
    companion object {
      fun <T : LootModifier> codec(create: (Optional<Holder<LootItemCondition>>, Int) -> T): MapCodec<T> =
        RecordCodecBuilder.mapCodec { instance -> codecStart(instance).apply(instance, create) }
    }
  }

  /** An adult cow or mooshroom a player killed drops bile by core's chance, better with Looting. */
  class BullBile(condition: Optional<Holder<LootItemCondition>>, priority: Int) : Modifier(condition, priority) {
    override fun doApply(loot: ObjectArrayList<ItemStack>, context: LootContext): ObjectArrayList<ItemStack> {
      val cow = context.getOptional(LootContextParams.THIS_ENTITY) as? AbstractCow ?: return loot
      if (cow.isBaby || context.getOptional(LootContextParams.DAMAGE_SOURCE) == null) return loot
      val killer = context.getOptional(LootContextParams.LAST_DAMAGE_PLAYER) ?: return loot
      val looting = context.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.LOOTING)
        .map { EnchantmentHelper.getItemEnchantmentLevel(it, killer.mainHandItem) }.orElse(0)
      if (context.random.nextDouble() < Bile.chance(looting) * Settings.bile.get()) loot += ItemStack(Items.bile.get())
      return loot
    }

    override fun codec(): MapCodec<out IGlobalLootModifier> = BULL_BILE
  }

  /** A chest of the game's own tables may hold the cans of the biome it stands in, each by its own chance. */
  class Cans(condition: Optional<Holder<LootItemCondition>>, priority: Int) : Modifier(condition, priority) {
    override fun doApply(loot: ObjectArrayList<ItemStack>, context: LootContext): ObjectArrayList<ItemStack> {
      if (!context.queriedLootTableId.path.startsWith("chests/")) return loot
      val origin = context.getOptional(LootContextParams.ORIGIN) ?: return loot
      val biome = context.level.getBiome(BlockPos.containing(origin)).unwrapKey().orElse(null)?.identifier()?.toString()
        ?: return loot
      for (can in CANS) {
        val chests = can.chests ?: continue
        if (biome in chests.biomes && context.random.nextDouble() < chests.chance * Settings.chests.get()) loot += Items.can(can)
      }
      return loot
    }

    override fun codec(): MapCodec<out IGlobalLootModifier> = CANS_CODEC
  }
}
