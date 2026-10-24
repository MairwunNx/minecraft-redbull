package ru.mairwunnx.redbull.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import ru.mairwunnx.redbull.modcore.Drinks;

/**
 * Milk takes some caffeine away. Fabric has no event for a finished drink, so this wraps the method every one ends
 * in: after the milk took every effect, as NeoForge's event comes, so the Caffeine effect is back at once.
 */
@Mixin(ItemStack.class)
abstract class MilkMixin {
  @WrapMethod(method = "finishUsingItem")
  private ItemStack redbull$milk(Level level, LivingEntity entity, Operation<ItemStack> original) {
    // The drink empties the stack, so look at what it was before.
    boolean milk = ((ItemStack) (Object) this).is(Items.MILK_BUCKET);
    ItemStack left = original.call(level, entity);
    if (milk && entity instanceof ServerPlayer player) Drinks.INSTANCE.milk(player);
    return left;
  }
}
