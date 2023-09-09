package dev.mja00.swarmsmps2.mixin;

import dev.mja00.swarmsmps2.SSMPS2Config;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl implements ServerPlayerConnection, ServerGamePacketListener {

    @Shadow public ServerPlayer player;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getLastActionTime()J", ordinal = 0))
    public long getLastActionTime(ServerPlayer instance) {
        if (!SSMPS2Config.SERVER.opsBypassIdleKick.get()) {
            return instance.getLastActionTime();
        }
        if (instance.gameMode.isSurvival()) {
            return instance.getLastActionTime();
        } else {
            return 0;
        }
    }
}
