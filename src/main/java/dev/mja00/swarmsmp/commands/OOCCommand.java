package dev.mja00.swarmsmp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmp.events.ChatEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class OOCCommand {

    static Logger LOGGER = LogManager.getLogger("CHAT");

    public OOCCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("ooc").then(Commands.argument("message", MessageArgument.message()).executes((command) -> {
            return sendGlobalMessage(command.getSource(), MessageArgument.getMessage(command, "message"));
        })));
    }

    private static int sendGlobalMessage(CommandSource source, ITextComponent message) throws CommandSyntaxException {
        LOGGER.info("[OOC] " + source.getName() + ": " + message.getUnformattedComponentText());
        // Get all players in the server
        List<ServerPlayerEntity> players = source.getWorld().getPlayers();
        String senderName = ChatEvents.playerName(source.asPlayer()).getString();
        String messageString = message.getString();
        // Send message to all players
        for (ServerPlayerEntity player : players) {
            IFormattableTextComponent chatMessage = new StringTextComponent("[OOC] <").mergeStyle(Style.EMPTY
                    .applyFormatting(TextFormatting.RED));
            chatMessage.appendSibling(new StringTextComponent(senderName).mergeStyle(Style.EMPTY
                    .applyFormatting(TextFormatting.RED)));
            chatMessage.appendSibling(new StringTextComponent("> ").mergeStyle(Style.EMPTY
                    .applyFormatting(TextFormatting.RED)));
            chatMessage.appendSibling(new StringTextComponent(messageString).mergeStyle(Style.EMPTY
                    .applyFormatting(TextFormatting.WHITE)));

            player.sendMessage(chatMessage, player.getUniqueID());
        }
        return 1;
    }
}
