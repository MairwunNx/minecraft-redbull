package ru.mairwunnx.redbull.neoforge.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mairwunnx.redbull.neoforge.Trader;

/** A wandering trader adds its cans after the game's trade sets; NeoForge has no event for its offers. */
@Mixin(WanderingTrader.class)
abstract class WanderingTraderMixin {
  @Inject(method = "updateTrades", at = @At("TAIL"))
  private void redbull$cans(ServerLevel level, CallbackInfo info) {
    Trader.stock((WanderingTrader) (Object) this);
  }
}
