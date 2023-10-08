package dev.mja00.swarmsmps2.mixin;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FriendlyByteBuf.class)
public abstract class MixinFriendlyByteBuf extends ByteBuf implements net.minecraftforge.common.extensions.IForgeFriendlyByteBuf {

    @Inject(method = "writeComponent", at = @At("HEAD"), cancellable = true)
    private void writeComponent(Component pComponent, CallbackInfoReturnable<FriendlyByteBuf> cir) {
        cir.setReturnValue(this.writeUtf(Component.Serializer.toJson(pComponent), 2621440));
    }

    @Inject(method = "readComponent", at = @At("HEAD"), cancellable = true)
    private void readComponent(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(Component.Serializer.fromJson(this.readUtf(2621440)));
    }

    @Shadow public abstract FriendlyByteBuf writeUtf(String pString, int pMaxLength);

    @Shadow public abstract String readUtf(int pMaxLength);
}
