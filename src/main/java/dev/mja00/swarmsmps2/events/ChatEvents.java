package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID)
public class ChatEvents {

    private static final Logger LOGGER = LogManager.getLogger("CHAT");

    // Gets the player's name and returns it as a string
    public static Component getPlayerName(Player player) {
        if (player.hasCustomName()) {
            return player.getCustomName();
        } else {
            return player.getDisplayName();
        }
    }

    private static void sendChatMessageToPlayer(Player player, Player sender, String message) {
        // Set the sender's name
        String senderName = getPlayerName(sender).getString();

        // Need a formattable string
        MutableComponent chatMessage = new TextComponent("<").withStyle(ChatFormatting.GOLD)
                .append(new TextComponent(senderName).withStyle(ChatFormatting.YELLOW))
                .append(new TextComponent("> ").withStyle(ChatFormatting.GOLD))
                .append(new TextComponent(message).withStyle(ChatFormatting.WHITE));

        // Send the message to the player
        player.sendMessage(chatMessage, sender.getUUID());
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String message = event.getMessage();
        int range = SSMPS2Config.SERVER.talkRange.get();

        // Get the player's world
        Level workingLevel = event.getPlayer().getLevel();
        List<? extends Player> players = workingLevel.players();
        Player sendingPlayer = workingLevel.getPlayerByUUID(event.getPlayer().getUUID());
        if (sendingPlayer == null) {
            event.setCanceled(true);
            return;
        }

        // Log the chat message
        LOGGER.info("<" + sendingPlayer.getDisplayName().getString() + "> " + message);

        for (Player player : players) {
            Player targetPlayer = workingLevel.getPlayerByUUID(player.getUUID());
            if (targetPlayer == null) {
                event.setCanceled(true);
                return;
            }

            // Check if the sendingPlayer and the targetPlayer are the same player
            if (sendingPlayer == targetPlayer) {
                sendChatMessageToPlayer(targetPlayer, sendingPlayer, message);
            } else {
                if (sendingPlayer.distanceTo(targetPlayer) <= range) {
                    sendChatMessageToPlayer(targetPlayer, sendingPlayer, message);
                }
            }
        }
        event.setCanceled(true);
    }
}
