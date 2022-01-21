package dev.mja00.swarmsmp.events;

import dev.mja00.swarmsmp.SSMPS2;
import dev.mja00.swarmsmp.SSMPS2Config;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod.EventBusSubscriber(modid = SSMPS2.MOD_ID)
public class ChatEvents {

    static Logger LOGGER = LogManager.getLogger("CHAT");

    public static ITextComponent playerName(PlayerEntity player) {
        if (player.hasCustomName()) {
            return player.getCustomName();
        } else {
            return player.getDisplayName();
        }
    }

    private static void sendChatMessageToPlayer(PlayerEntity player, PlayerEntity sender, String message) {
        // Get the sender's name
        String senderName = playerName(sender).getString();

        IFormattableTextComponent chatMessage = new StringTextComponent("<").mergeStyle(Style.EMPTY
                .applyFormatting(TextFormatting.GOLD));
        chatMessage.appendSibling(new StringTextComponent(senderName).mergeStyle(Style.EMPTY
                .applyFormatting(TextFormatting.YELLOW)));
        chatMessage.appendSibling(new StringTextComponent("> ").mergeStyle(Style.EMPTY
                .applyFormatting(TextFormatting.GOLD)));
        chatMessage.appendSibling(new StringTextComponent(message).mergeStyle(Style.EMPTY
                .applyFormatting(TextFormatting.WHITE)));

        // Send the message to the player
        player.sendMessage(chatMessage, sender.getUniqueID());

    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String message = event.getMessage();
        int range = SSMPS2Config.SERVER.talkRange.get();

        World workingWorld = event.getPlayer().getEntityWorld();
        MinecraftServer server = workingWorld.getServer();
        List<? extends PlayerEntity> players = workingWorld.getPlayers();
        PlayerEntity sendingPlayer = workingWorld.getPlayerByUuid(event.getPlayer().getUniqueID());
        if (sendingPlayer == null) {
            event.setCanceled(true);
            return;
        }

        // Log chat message
        LOGGER.info("<" + sendingPlayer.getDisplayName().getString() + "> " + message);

        for (PlayerEntity player : players) {
            PlayerEntity targetPlayer = workingWorld.getPlayerByUuid(player.getUniqueID());
            if (targetPlayer == null) {
                event.setCanceled(true);
                return;
            }

            // First check if the targetPlayer is the sendingPlayer
            if (targetPlayer == sendingPlayer) { // Sends the chat back to the player since we're cancelling the event
                sendChatMessageToPlayer(targetPlayer, sendingPlayer, message);
            } else { // Sends the chat to the targetPlayer if they're in range
                if (sendingPlayer.getDistance(targetPlayer) <= range) {
                    sendChatMessageToPlayer(targetPlayer, sendingPlayer, message);
                }
            }
        }
        event.setCanceled(true);
    }
}
