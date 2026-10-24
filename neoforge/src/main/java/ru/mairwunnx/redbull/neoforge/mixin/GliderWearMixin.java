package ru.mairwunnx.redbull.neoforge.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.mairwunnx.redbull.modcore.Wings;

/**
 * Wings carry the flight, so an elytra worn with them keeps every point. The game wears a random glider every second
 * of flight in `updateFallFlying`, and NeoForge has no event for it.
 */
@Mixin(LivingEntity.class)
abstract class GliderWearMixin {
  @WrapWithCondition(
    method = "updateFallFlying",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/item/ItemStack;onGlideDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"
    )
  )
  private boolean redbull$wings(ItemStack glider, LivingEntity wearer, EquipmentSlot slot) {
    return !wearer.hasEffect(Wings.INSTANCE.getEFFECT());
  }
}
