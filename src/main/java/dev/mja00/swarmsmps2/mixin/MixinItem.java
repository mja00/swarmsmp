package dev.mja00.swarmsmps2.mixin;

import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinItem {

    @Inject(method = "isFoil", at = @At("HEAD"), cancellable = true)
    private void isFoil(ItemStack pStack, CallbackInfoReturnable<Boolean> cir) {
        if (!pStack.isEnchanted()) {
            cir.setReturnValue(false);
            return;
        }
        // Get enchantments
        ListTag enchantments = pStack.getEnchantmentTags();
        // Check if any are over level
        for (int i = 0; i < enchantments.size(); i++) {
            if (enchantments.getCompound(i).getInt("lvl") > 1) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }
}
