package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.players.PlayerList;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class WhosOnlineCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final List<String> FACTIONS = Arrays.asList("swarm", "construct", "undead", "natureborn");
    static final HashMap<String, ChatFormatting> FACTION_COLORS = new HashMap<String, ChatFormatting>() {{
        put("swarm", ChatFormatting.RED);
        put("construct", ChatFormatting.GOLD);
        put("undead", ChatFormatting.BLUE);
        put("natureborn", ChatFormatting.GREEN);
    }};

    // Our custom suggestions provider for faction names
    public static final SuggestionProvider<CommandSourceStack> FACTION_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(FACTIONS.stream(), builder);
    };

    public WhosOnlineCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("whosonline").executes((command) -> whosOnline(command.getSource())));
    }

    private int whosOnline(CommandSourceStack source) {
        // This'll get all currently online players and see what team they're on, then print out the count of players per team
        PlayerList players = source.getServer().getPlayerList();
        HashMap<String, Integer> teamCounts = new HashMap<>();
        for (String team : FACTIONS) {
            teamCounts.put(team, 0);
        }
        players.getPlayers().forEach((player) -> {
            if (player.getTeam() == null) { return; }
            String team = player.getTeam().getName();
            // If the player is in a team we don't care about just skip them
            if (!FACTIONS.contains(team)) { return; }
            teamCounts.put(team, teamCounts.get(team) + 1);
        });
        // Loop the hashmap and print out the team name and count
        teamCounts.forEach((team, count) -> {
            source.sendSuccess(new TextComponent(titleCaseString(team) + ": " + count).withStyle(FACTION_COLORS.get(team)), false);
        });
        return 1;
    }

    private String titleCaseString(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
