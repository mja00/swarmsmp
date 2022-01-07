package dev.mja00.swarmsmp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmp.SSMPS2Config;
import dev.mja00.swarmsmp.events.ChatEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class BetterMeCommand {

    static Logger LOGGER = LogManager.getLogger("CHAT");

    public BetterMeCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("me").then(Commands.argument("action", StringArgumentType.greedyString()).executes((command) -> {
            return sendAction(command.getSource(), StringArgumentType.getString(command, "action"));
        })));
    }

    private static int sendAction(CommandSource source, String action) throws CommandSyntaxException {
        int range = SSMPS2Config.SERVER.talkRange.get();
        ServerPlayerEntity player = source.asPlayer();
        String playerName = ChatEvents.playerName(player).getString();
        // Get all the players
        List<ServerPlayerEntity> players = source.getWorld().getPlayers();

        // Log it
        LOGGER.info("[Me] " + playerName + " " + action);

        // Send the action to all players
        for (ServerPlayerEntity p : players) {
            if (p.getDistance(player) <= range) {
                p.sendMessage(new StringTextComponent(playerName + " " + action), player.getUniqueID());
            }
        }

        return 0;
    }
}
