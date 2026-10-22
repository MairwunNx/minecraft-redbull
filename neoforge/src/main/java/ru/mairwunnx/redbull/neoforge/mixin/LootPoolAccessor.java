package ru.mairwunnx.redbull.neoforge.mixin;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A pool's entries, to add cans beside the fishing junk's own. NeoForge lets a loaded table gain or lose whole pools,
 * not entries, and a pool of its own would add a can to a catch instead of taking a junk item's place.
 */
@Mixin(LootPool.class)
public interface LootPoolAccessor {
  @Accessor("entries")
  List<LootPoolEntryContainer> redbull$entries();

  @Mutable
  @Accessor("entries")
  void redbull$setEntries(List<LootPoolEntryContainer> entries);
}
