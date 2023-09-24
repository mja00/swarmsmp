package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class ProficiencyCommand {

    static Logger LOGGER = LogManager.getLogger("PROFICIENCY");
    static final String translationKey = SwarmsmpS2.translationKey;

    private static final SuggestionProvider<CommandSourceStack> PROFICIENCY_SUGGESTIONS = (context, builder) -> {
        String[] proficiencies = SSMPS2Config.getProficiencies();
        return net.minecraft.commands.SharedSuggestionProvider.suggest(proficiencies, builder);
    };

    public ProficiencyCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("proficiency")
                .requires((command) -> command.hasPermission(2))
                .then(Commands.argument("players", EntityArgument.players())
                .then(Commands.argument("proficiency1", StringArgumentType.word()).suggests(PROFICIENCY_SUGGESTIONS)
                .then(Commands.argument("proficiency2", StringArgumentType.word()).suggests(PROFICIENCY_SUGGESTIONS)
                .then(Commands.argument("proficiency3", StringArgumentType.word()).suggests(PROFICIENCY_SUGGESTIONS)
                .executes((command) -> assignProficiencies(command.getSource(), StringArgumentType.getString(command, "proficiency1"), StringArgumentType.getString(command, "proficiency2"), StringArgumentType.getString(command, "proficiency3"), EntityArgument.getPlayers(command, "players"))))))));
    }

    private int assignProficiencies(CommandSourceStack source, String proficiency1, String proficiency2, String proficiency3, Collection<ServerPlayer> targets) {
        CommandSourceStack consoleSource = source.getServer().createCommandSourceStack();
        // Essentially each proficiency is an alias for origin commands
        Commands commandsClass = source.getServer().getCommands();
        String[] commands = new String[3];
        commands[0] = "origin set %player% origins:proficiency_1 ssmp:" + proficiency1;
        commands[1] = "origin set %player% origins:proficiency_2 ssmp:" + proficiency2;
        commands[2] = "origin set %player% origins:proficiency_3 ssmp:" + proficiency3;
        for (ServerPlayer player: targets) {
            for (String command : commands) {
                String replacedCommand = command.replace("%player%", player.getName().getString());
                commandsClass.performCommand(consoleSource, replacedCommand);
            }
            player.sendMessage(new TranslatableComponent(translationKey + "commands.proficiencycommand.success", proficiency1, proficiency2, proficiency3), Util.NIL_UUID);
        }
        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.proficiencycommand.success.single", targets.iterator().next().getName().getString(), proficiency1, proficiency2, proficiency3), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.proficiencycommand.success.multiple", targets.size(), proficiency1, proficiency2, proficiency3), true);
        }
        return targets.size();
    }
}
