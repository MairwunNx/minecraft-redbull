package ru.mairwunnx.redbull.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.mairwunnx.redbull.fabric.Drinks;

/** Milk takes some caffeine away. Fabric has no event for a finished drink, so this listens where every one ends. */
@Mixin(ItemStack.class)
abstract class MilkMixin {
  @Inject(method = "finishUsingItem", at = @At("HEAD"))
  private void redbull$milk(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> info) {
    if (entity instanceof ServerPlayer player && ((ItemStack) (Object) this).is(Items.MILK_BUCKET)) {
      Drinks.INSTANCE.milk(player);
    }
  }
}
