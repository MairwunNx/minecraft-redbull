package ru.mairwunnx.redbull.fabric.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.redbull.fabric.Wings;

/**
 * Every tick of a glide, where the game keeps it going: Wings trail feathers behind it. The game calls this only for
 * an entity in flight, so nothing runs for anyone on the ground.
 */
@Mixin(LivingEntity.class)
abstract class GlideMixin {
  @Inject(method = "updateFallFlying", at = @At("HEAD"))
  private void redbull$glide(CallbackInfo info) {
    LivingEntity self = (LivingEntity) (Object) this;
    if (!self.level().isClientSide()) Wings.glide(self);
  }
}
