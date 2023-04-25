package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;

public class JoinCommand {

    static Logger LOGGER = LogManager.getLogger("JOIN");
    static final String translationKey = SwarmsmpS2.translationKey;

    public JoinCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("joincommand")
                .requires((command) -> command.hasPermission(2))
                .then(Commands.literal("swarm").then(Commands.argument("target", EntityArgument.players())
                        .executes((command) -> joinFaction(command.getSource(), EntityArgument.getPlayers(command, "target"), "swarm"))))
                .then(Commands.literal("construct").then(Commands.argument("target", EntityArgument.players())
                        .executes((command) -> joinFaction(command.getSource(), EntityArgument.getPlayers(command, "target"), "construct"))))
                .then(Commands.literal("undead").then(Commands.argument("target", EntityArgument.players())
                        .executes((command) -> joinFaction(command.getSource(), EntityArgument.getPlayers(command, "target"), "undead"))))
                .then(Commands.literal("natureborn").then(Commands.argument("target", EntityArgument.players())
                        .executes((command) -> joinFaction(command.getSource(), EntityArgument.getPlayers(command, "target"), "natureborn")))));
    }

    private int joinFaction(CommandSourceStack source, Collection<ServerPlayer> targets, String faction) throws CommandSyntaxException {
        // Ensure we have 1 target
        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.joincommand.error.multiple_targets"));
            return 0;
        }
        // Initialize an empty list of commands to run
        List<? extends String> commands = switch (faction) {
            case "swarm" -> SSMPS2Config.SERVER.swarmCommands.get();
            case "construct" -> SSMPS2Config.SERVER.constructCommands.get();
            case "undead" -> SSMPS2Config.SERVER.undeadCommands.get();
            case "natureborn" -> SSMPS2Config.SERVER.naturebornCommands.get();
            default -> null;
            // We'll want to create a switch case for each faction and get the config value for that faction, assigning it to commands
        };
        // Ensure we have commands to run
        if (commands == null) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.joincommand.error.no_commands"));
            return 0;
        }
        // Get the player
        ServerPlayer player = targets.iterator().next();
        String playerName = player.getName().getString();
        // Get a console source stack
        CommandSourceStack consoleSource = source.getServer().createCommandSourceStack();
        Commands commandsClass = source.getServer().getCommands();
        // Run each command
        for (String command : commands) {
            // Replace the player's name in the command with the player's name
            String replacedCommand = command.replace("%player%", playerName);
            // Run the command
            commandsClass.performCommand(consoleSource, replacedCommand);
        }
        return 1;
    }
}
