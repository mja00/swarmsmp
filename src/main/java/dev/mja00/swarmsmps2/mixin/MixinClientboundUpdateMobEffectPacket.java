package dev.mja00.swarmsmps2.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundUpdateMobEffectPacket.class)
public abstract class MixinClientboundUpdateMobEffectPacket implements Packet<ClientGamePacketListener> {
    @Mutable
    @Shadow @Final private int effectDurationTicks;

    @Inject(method = "<init>(ILnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("RETURN"))
    private void init(int pEntityId, MobEffectInstance pEffectInstance, CallbackInfo ci) {
        this.effectDurationTicks = pEffectInstance.getDuration();
    }

    @Inject(method = "isSuperLongDuration", at = @At("HEAD"), cancellable = true)
    private void isSuperLongDuration(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.effectDurationTicks == 2147483647);
    }
}
