package dev.mja00.swarmsmps2.mixin;

import com.mojang.authlib.GameProfile;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.helpers.SiteAPIHelper;
import dev.mja00.swarmsmps2.objects.JoinInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList {

    @Shadow @Final private List<ServerPlayer> players;
    @Shadow @Final protected int maxPlayers;
    @Unique
    private static final String swarmsmp_s2$translationKey = SwarmsmpS2.translationKey;
    @Unique
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);

    @Unique
    private static final Logger swarmsmp_s2$LOGGER = LoggerFactory.getLogger("SSMPS2/MixinPlayerList");

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    public void canPlayerLogin(SocketAddress pSocketAddress, GameProfile pGameProfile, CallbackInfoReturnable<Component> cir) {
        if (!SSMPS2Config.SERVER.enableAPI.get()) {
            // Skip our entire mixin
            return;
        }
        // Get the player's UUID as we'll be working with that a lot
        UUID playerUUID = pGameProfile.getId();
        swarmsmp_s2$LOGGER.info("Player " + pGameProfile.getName() + " with UUID " + playerUUID + " is attempting to join");
        // If they're in our bypass list they're good to go
        if (SSMPS2Config.SERVER.bypassedPlayers.get().contains(playerUUID.toString())) {
            cir.setReturnValue(null);
            cir.cancel();
            return;
        }
        // Setup a counter for how long the thread has been running
        long threadStart = System.currentTimeMillis();
        Thread thread = swarmsmp_s2$getThread(pGameProfile, cir, playerUUID, this.players.size(), this.maxPlayers);
        thread.start();
        // Just sit and wait for the thread to finish
        while (thread.isAlive()) {
            // If the thread has been running for more than 5 seconds, kick them with a timeout error
            if (System.currentTimeMillis() - threadStart > (SSMPS2Config.SERVER.firstTimeout.get() + 2) * 1000L) {
                swarmsmp_s2$LOGGER.error("Thread for player " + pGameProfile.getName() + " with UUID " + playerUUID + " has timed out");
                cir.setReturnValue(new TranslatableComponent(swarmsmp_s2$translationKey + "connection.timeout"));
                cir.cancel();
                // Kill thread
                thread.interrupt();
                return;
            }
        }
        // They're fully let in so just let vanilla do its checks now
    }

    @NotNull
    @Unique
    private static Thread swarmsmp_s2$getThread(GameProfile pGameProfile, CallbackInfoReturnable<Component> cir, UUID playerUUID, int currentPlayers, int maxPlayers) {
        Thread thread = new Thread(net.minecraftforge.fml.util.thread.SidedThreadGroups.SERVER, "SSMP Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()) {
            public void run() {
                swarmsmp_s2$LOGGER.debug("Thread spun up for player " + pGameProfile.getName() + " with UUID " + playerUUID);
                SiteAPIHelper apiHelper = new SiteAPIHelper(SSMPS2Config.SERVER.apiKey.get(), SSMPS2Config.SERVER.apiBaseURL.get());
                JoinInfo joinInfo;
                try {
                    joinInfo = apiHelper.getJoinInfo(playerUUID.toString().replace("-", ""));
                    if (Thread.currentThread().isInterrupted()) {
                        // Just exit our thread here since upstream asked up to stop
                        return;
                    }
                } catch (SiteAPIHelper.APIRequestFailedException e) {
                    swarmsmp_s2$LOGGER.error("Failed to get join info for player " + pGameProfile.getName() + " with UUID " + playerUUID + " with error " + e.getMessage());
                    cir.setReturnValue(new TranslatableComponent(swarmsmp_s2$translationKey + "connection.error"));
                    cir.cancel();
                    return;
                }
                if (!joinInfo.getAllow()) {
                    // Not allowed to join
                    if (SSMPS2Config.SERVER.fallbackServer.get()) {
                        String errorMsg = joinInfo.getMessage();
                        // If it's either "You are not whitelisted." or "You are banned from the server for: " then we want to block their connection, otherwise let them in
                        if (Objects.equals(errorMsg, "You are not whitelisted.") || errorMsg.startsWith("You are banned from the server for: ")) {
                            cir.setReturnValue(new TranslatableComponent(swarmsmp_s2$translationKey + "connection.disconnected", new TextComponent(errorMsg).withStyle(ChatFormatting.AQUA)));
                            cir.cancel();
                            return;
                        }
                        cir.setReturnValue(null);
                        cir.cancel();
                        return;
                    }
                    cir.setReturnValue(new TranslatableComponent(swarmsmp_s2$translationKey + "connection.disconnected", new TextComponent(joinInfo.getMessage()).withStyle(ChatFormatting.AQUA)));
                    cir.cancel();
                    return;
                }
                if (!Objects.equals(joinInfo.getMessage(), "Bypass")) {
                    // We'll do some player limit checks
                    int reservedSlots = SSMPS2Config.SERVER.reservedSlots.get();
                    if (currentPlayers >= maxPlayers - reservedSlots) {
                        cir.setReturnValue(new TranslatableComponent(swarmsmp_s2$translationKey + "connection.full").withStyle(ChatFormatting.AQUA));
                        cir.cancel();
                    }
                }
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(swarmsmp_s2$LOGGER));
        return thread;
    }
}
