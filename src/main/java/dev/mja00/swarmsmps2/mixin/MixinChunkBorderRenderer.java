package dev.mja00.swarmsmps2.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkBorderRenderer.class)
public abstract class MixinChunkBorderRenderer {
    public MixinChunkBorderRenderer() {

    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMaxBuildHeight()I"))
    public int getMaxBuildHeight(ClientLevel instance) {
        return 1024;
    }

}
