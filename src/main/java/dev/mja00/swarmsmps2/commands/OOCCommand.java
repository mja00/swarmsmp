package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmps2.MC2DiscordCompat;
import dev.mja00.swarmsmps2.events.ChatEvents;
import ml.denisd3d.mc2discord.core.entities.Player;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class OOCCommand {

    static Logger LOGGER = LogManager.getLogger("CHAT");

    public OOCCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ooc").then(Commands.argument("message", MessageArgument.message()).executes((command) -> {
            return sendGlobalMessage(command.getSource(), MessageArgument.getMessage(command, "message"));
        })));
    }

    private int sendGlobalMessage(CommandSourceStack source, Component message) throws CommandSyntaxException {
        TextComponent messageComponent = (TextComponent) message;
        LOGGER.info("[OOC] " + source.getTextName() + ": " + message.getString());
        // Get all players in the server
        List<ServerPlayer> players = source.getLevel().players();
        String senderName = ChatEvents.getPlayerName(source.getPlayerOrException()).getString();
        String messageString = message.getString();
        // Send message to all players
        for (ServerPlayer player : players) {
            MutableComponent chatMessage = new TextComponent("[OOC] <").withStyle(Style.EMPTY
                    .applyFormat(ChatFormatting.RED));
            chatMessage.append(new TextComponent(senderName).withStyle(Style.EMPTY
                    .applyFormat(ChatFormatting.RED)));
            chatMessage.append(new TextComponent("> ").withStyle(Style.EMPTY
                    .applyFormat(ChatFormatting.RED)));
            chatMessage.append(new TextComponent(messageString).withStyle(Style.EMPTY
                    .applyFormat(ChatFormatting.WHITE)));

            player.sendMessage(chatMessage, source.getPlayerOrException().getUUID());
        }
        // Send our Discord message
        if (ModList.get().isLoaded("mc2discord")) {
            Player player = new Player(senderName, senderName, source.getPlayerOrException().getUUID());
            MC2DiscordCompat.sendOOCMessage(messageString, player);
        }
        return 1;
    }
}
