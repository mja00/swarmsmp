package dev.mja00.swarmsmp.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilSocialInteractionsService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(YggdrasilSocialInteractionsService.class)
public class RemoveBlockedPlayerCheck {

    @Inject(method = "Lcom/mojang/authlib/yggdrasil/YggdrasilSocialInteractionsService;isBlockedPlayer(Ljava/util/UUID;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void returnFalseOnCheck(UUID playerID, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("Skipping blocked player check");
        cir.setReturnValue(false);
    }

}
