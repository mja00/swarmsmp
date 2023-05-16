package dev.mja00.swarmsmps2.events;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.lang.management.ManagementFactory;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayerEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;

    // Startup time stuff
    public static long expectedTime = 0;
    public static long doneTime = 0;

    static boolean triggered = false;
    public static boolean trueFullscreen;

    static long startupTime;
    static boolean hasBeenMainMenu = false;
    static boolean hasLeftMainMenu = false;

    @SubscribeEvent
    public static void onChatEvent(ClientChatReceivedEvent event) {
        if (event.getMessage() instanceof TranslatableComponent message) {
            // Get the message arguments
            Object[] args = message.getArgs();
            if (message.getKey().equals("multiplayer.player.joined")) {
                TextComponent messageComponent = (TextComponent) args[0];
                String playerName = messageComponent.getSiblings().get(0).getString();
                LOGGER.info("Player joined: " + playerName);
                event.setCanceled(true);
            } else if (message.getKey().equals("multiplayer.player.left")) {
                TextComponent messageComponent = (TextComponent) args[0];
                String playerName = messageComponent.getSiblings().get(0).getString();
                LOGGER.info("Player left: " + playerName);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onGuiDraw(ScreenEvent.DrawScreenEvent event) {
        if (!hasLeftMainMenu && event.getScreen() instanceof TitleScreen) {
            hasBeenMainMenu = true;
            long minutes = (startupTime / 1000) / 60;
            long seconds = (startupTime / 1000) % 60;
            long ex_mins = (expectedTime / 1000) / 60;
            long ex_secs = (expectedTime / 1000) % 60;

            float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
            if (guiScale <= 0) guiScale = 1; // Just in case

            String txt = "Startup took " + minutes + "m " + seconds + "s | Last 3 avg: " + ex_mins + "m " + ex_secs + "s";
            float pX = (float) Minecraft.getInstance().getWindow().getWidth() / 2 / guiScale - (float) Minecraft.getInstance().font.width(txt) /2;
            float pY = (float) Minecraft.getInstance().getWindow().getHeight() / guiScale - 20;
            int color;
            if (startupTime < 90_000) {
                // This is "good"
                color = Color.GREEN.getRGB();
            } else if (startupTime < 120_000) {
                // This is "ok"
                color = Color.YELLOW.getRGB();
            } else {
                // This is "bad"
                color = Color.RED.getRGB();
            }
            Minecraft.getInstance().font.drawShadow(new PoseStack(), txt, pX, pY, color);
        } else if (hasBeenMainMenu) {
            hasLeftMainMenu = true;
        }
    }

    @SubscribeEvent
    public static void onGuiOpen(ScreenOpenEvent event) {
        if (!triggered && event.getScreen() instanceof TitleScreen) {
            triggered = true;

            Minecraft.getInstance().options.fullscreen = trueFullscreen;
            if (Minecraft.getInstance().options.fullscreen && !Minecraft.getInstance().getWindow().isFullscreen()) {
                Minecraft.getInstance().getWindow().toggleFullScreen();
                Minecraft.getInstance().options.fullscreen = Minecraft.getInstance().getWindow().isFullscreen();
            }

            startupTime = ManagementFactory.getRuntimeMXBean().getUptime();
            LOGGER.info("Startup took " + startupTime + "ms");

            doneTime = startupTime;
            SSMPS2Config.addStartupTime(startupTime);
        }
    }
}
