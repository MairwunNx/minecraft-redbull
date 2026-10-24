package ru.mairwunnx.redbull.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.mairwunnx.redbull.modcore.Collected;

/**
 * A picked-up can joins the collection once some of it made it into the inventory. Fabric has no pickup event,
 * and the game empties the stack it moved, so this keeps a copy from before.
 */
@Mixin(ItemEntity.class)
abstract class PickupMixin {
  @WrapOperation(
    method = "playerTouch",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z")
  )
  private boolean redbull$collect(Inventory inventory, ItemStack stack, Operation<Boolean> add) {
    ItemStack before = stack.copy();
    boolean added = add.call(inventory, stack);
    if (inventory.player instanceof ServerPlayer player && stack.getCount() < before.getCount()) {
      Collected.record(player, List.of(before));
    }
    return added;
  }
}
