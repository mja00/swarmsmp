package dev.mja00.swarmsmps2.mixin;

import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringUtil.class)
public abstract class MixinStringUtil {

    @Inject(method = "formatTickDuration", at = @At("HEAD"), cancellable = true)
    private static void formatTickDuration(int pTicks, CallbackInfoReturnable<String> cir) {
        // We wanna format this as hh:mm:ss but omit the hours if it's less than 1 hour
        int seconds = pTicks / 20;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        if (hours > 0) {
            cir.setReturnValue(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        } else {
            cir.setReturnValue(String.format("%02d:%02d", minutes, seconds));
        }
    }
}
