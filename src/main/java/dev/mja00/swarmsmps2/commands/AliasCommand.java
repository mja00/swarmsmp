package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.apache.logging.log4j.Logger;

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
                        .executes((command) -> alias(command.getSource(), StringArgumentType.getString(command, "alias")))));
    }

    private int alias(CommandSourceStack source, String alias) {
        String[] commands = SSMPS2Config.getAliasByName(alias);
        if (commands.length == 0) {
            source.sendFailure(new net.minecraft.network.chat.TranslatableComponent(SwarmsmpS2.translationKey + "commands.joincommand.error.no_commands"));
            return 0;
        }
        CommandSourceStack consoleSource = source.getServer().createCommandSourceStack();
        net.minecraft.commands.Commands commandsClass = source.getServer().getCommands();
        for (String command : commands) {
            commandsClass.performCommand(consoleSource, command);
        }
        return Command.SINGLE_SUCCESS;
    }
}
