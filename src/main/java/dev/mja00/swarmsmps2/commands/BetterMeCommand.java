package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.events.ChatEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class BetterMeCommand {

    static Logger LOGGER = LogManager.getLogger("CHAT");

    public BetterMeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("me").then(Commands.argument("action", StringArgumentType.greedyString()).executes((command) -> sendAction(command.getSource(), StringArgumentType.getString(command, "action")))));
    }

    private int sendAction(CommandSourceStack source, String action) throws CommandSyntaxException {
        int range = SSMPS2Config.SERVER.talkRange.get();
        ServerPlayer player = source.getPlayerOrException();
        String playerName = ChatEvents.getPlayerName(player).getString();
        // Get all players in range
        List<ServerPlayer> players = source.getLevel().players();

        // Log the message
        LOGGER.info("[ME] " + playerName + " " + action);

        // Send the message to all players in range
        for (ServerPlayer p : players) {
            if (p.distanceTo(player) <= range) {
                p.sendMessage(new TextComponent(playerName + " " + action).withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY), player.getUUID());
            }
        }

        return 1;
    }
}
