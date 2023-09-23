package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class AliasCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;

    public static final SuggestionProvider<CommandSourceStack> ALIAS_SUGGESTIONS = (context, builder) -> {
        String[] aliases = SSMPS2Config.getAliasNames();
        return net.minecraft.commands.SharedSuggestionProvider.suggest(aliases, builder);
    };

    public AliasCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("alias")
                .requires((command) -> command.hasPermission(2))
                .then(Commands.argument("alias", StringArgumentType.word()).suggests(ALIAS_SUGGESTIONS)
                        .executes((command) -> alias(command.getSource(), StringArgumentType.getString(command, "alias")))
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> aliasOnPlayers(command.getSource(), StringArgumentType.getString(command, "alias"), EntityArgument.getPlayers(command, "player"))))));
    }

    private int alias(CommandSourceStack source, String alias) throws CommandSyntaxException {
        String[] commands = SSMPS2Config.getAliasByName(alias);
        if (commands.length == 0) {
            source.sendFailure(new net.minecraft.network.chat.TranslatableComponent(SwarmsmpS2.translationKey + "commands.joincommand.error.no_commands"));
            return 0;
        }
        CommandSourceStack consoleSource = source.getServer().createCommandSourceStack();
        net.minecraft.commands.Commands commandsClass = source.getServer().getCommands();
        for (String command : commands) {
            String replacedCommand = command.replace("%player%", source.getPlayerOrException().getName().toString());
            commandsClass.performCommand(consoleSource, replacedCommand);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int aliasOnPlayers(CommandSourceStack source, String alias, Collection<ServerPlayer> targets) {
        String[] commands = SSMPS2Config.getAliasByName(alias);
        if (commands.length == 0) {
            source.sendFailure(new net.minecraft.network.chat.TranslatableComponent(SwarmsmpS2.translationKey + "commands.joincommand.error.no_commands"));
            return 0;
        }
        // Get a console source stack
        CommandSourceStack consoleSource = source.getServer().createCommandSourceStack();
        Commands commandsClass = source.getServer().getCommands();
        // For each player in the targets
        for (ServerPlayer player : targets) {
            String playerName = player.getName().getString();
            // Run each command
            for (String command : commands) {
                // Replace the player's name in the command with the player's name
                String replacedCommand = command.replace("%player%", playerName);
                // Run the command
                commandsClass.performCommand(consoleSource, replacedCommand);
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
