package dev.mja00.swarmsmps2.mixin;

import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public class MixinDispenseItemBehavior {

   @Inject(method = "registerBehavior", at = @At("HEAD"), cancellable = true)
    private static void registerBehavior(ItemLike pItem, DispenseItemBehavior pBehavior, CallbackInfo ci) {
       // If the item is bone meal, cancel the dispenser behavior
       if (pItem.equals(Items.BONE_MEAL)) {
          ci.cancel();
       }
    }
}
