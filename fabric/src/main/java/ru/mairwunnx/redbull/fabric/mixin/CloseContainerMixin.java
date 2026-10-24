package ru.mairwunnx.redbull.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.redbull.modcore.Collected;

/**
 * Whatever a player took from a chest or anything else with slots is in their inventory once they close it, their
 * own inventory included. Fabric has no event for a closed container.
 */
@Mixin(ServerPlayer.class)
abstract class CloseContainerMixin {
  @Inject(method = "doCloseContainer", at = @At("HEAD"))
  private void redbull$collect(CallbackInfo info) {
    ServerPlayer player = (ServerPlayer) (Object) this;
    Collected.record(player, player.getInventory());
  }
}
