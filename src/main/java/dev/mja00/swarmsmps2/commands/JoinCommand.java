package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class JoinCommand {

    static Logger LOGGER = LogManager.getLogger("JOIN");
    static final String translationKey = SwarmsmpS2.translationKey;

    private static final SuggestionProvider<CommandSourceStack> TRAIT_SUGGESTIONS = (context, builder) -> {
        String[] traits = SSMPS2Config.getTraitNames();
        return SharedSuggestionProvider.suggest(traits, builder);
    };

    public JoinCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("join")
                .requires((command) -> command.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("trait", StringArgumentType.word()).suggests(TRAIT_SUGGESTIONS)
                                .executes((command) -> join(command.getSource(), EntityArgument.getPlayers(command, "targets"), StringArgumentType.getString(command, "trait"))))));
    }

    private int join(CommandSourceStack source, Collection<ServerPlayer> targets, String trait) throws CommandSyntaxException {
        // We'll want to get the commands for the given trait
        String[] commands = SSMPS2Config.getTraitByName(trait);
        // Ensure we have commands to run
        if (commands.length == 0) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.joincommand.error.no_commands"));
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
            // Inform the player they got a trait
            player.sendMessage(new TranslatableComponent(translationKey + "commands.joincommand.success", trait).withStyle(ChatFormatting.GREEN), player.getUUID());
        }
        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.joincommand.admin.success.single", trait, targets.iterator().next().getName().getString()), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.joincommand.admin.success.multiple", trait, targets.size()), true);
        }
        return 1;
    }
}
