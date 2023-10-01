package dev.mja00.swarmsmps2.mixin;

import com.mojang.blaze3d.platform.WindowEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler, net.minecraftforge.client.extensions.IForgeMinecraft {

    @Mutable
    @Final
    @Shadow
    public final Options options;

    @Unique
    public boolean swarmsmp_s2$isSpectatorHighlightEnabled = false;

    @Shadow
    @Nullable
    public LocalPlayer player;

    protected MixinMinecraft(GameConfig pGameConfig, Options options) {
        super("Client");
        this.options = options;
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void handleKeybinds(CallbackInfo ci) {
        // Setup a keybind listener for the specator highlight key
        while (this.options.keySpectatorOutlines.consumeClick()) {
            // Toggle the spectator highlight
            this.swarmsmp_s2$isSpectatorHighlightEnabled = !this.swarmsmp_s2$isSpectatorHighlightEnabled;
        }
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
private void shouldEntityAppearGlowing(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(pEntity.isCurrentlyGlowing() || this.player != null && this.player.isSpectator() && this.swarmsmp_s2$isSpectatorHighlightEnabled && pEntity.getType() == EntityType.PLAYER);
    }
}
