package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RaidCommand {

    private static Logger LOGGER = LogManager.getLogger("SSMPS2/RaidCommand");
    static final String translationKey = SwarmsmpS2.translationKey;
    static final List<String> FACTIONS = Arrays.asList("swarm", "construct", "undead", "natureborn");
    // Our custom suggestions provider for faction names
    public static final SuggestionProvider<CommandSourceStack> FACTION_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(FACTIONS.stream(), builder);
    };

    public static class Raid {
        public final String id;
        public MinecraftServer server;
        public String source;
        public String target;
        public long created;
        public long expires;

        public Raid(String s) { id = s; }
    }

    public static final HashMap<String, Raid> RAIDS = new HashMap<String, Raid>();

    public static Raid createRaid(MinecraftServer server, String source, String target) {
        String key = source + "->" + target;
        if (RAIDS.containsKey(key)) {
            return RAIDS.get(key);
        }
        Raid raid = new Raid(source + "->" + target);
        raid.server = server;
        raid.source = source;
        raid.target = target;
        raid.created = System.currentTimeMillis();
        // It needs to expire 72 hours later
        raid.expires = raid.created + (72 * 60 * 60 * 1000);
        RAIDS.put(key, raid);
        return raid;
    }

    public RaidCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("raid")
                .then(Commands.literal("declare")
                        .then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTION_SUGGESTIONS)
                                .then(Commands.argument("player_count", IntegerArgumentType.integer(1, 30))
                                        .executes((command) -> declareRaid(
                                                command.getSource(),
                                                StringArgumentType.getString(command, "faction"),
                                                IntegerArgumentType.getInteger(command, "player_count"))
                                        ))))
                .then(Commands.literal("get")
                        .executes((command) -> getRaid(command.getSource()))
                        .requires((command) -> command.hasPermission(2))
                        .then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTION_SUGGESTIONS)
                                .executes((command) -> getRaidForFaction(command.getSource(), StringArgumentType.getString(command, "faction"))))));
    }

    private int getRaid(CommandSourceStack source) throws CommandSyntaxException {
        // We want to get the player's team, this'll let us know what faction they're in
        // We can then look through the list of raids and see if there's any raids their faction has started
        ServerPlayer player = source.getPlayerOrException();
        if (player.getTeam() == null) {
            // They're not on a team
            source.sendFailure(new TranslatableComponent(translationKey + "commands.raid.no_team"));
            return 0;
        }
        String team = player.getTeam().getName();
        // Scan through the hashmaps
        return getRaidForFaction(source, team);
    }

    private int getRaidForFaction(CommandSourceStack source, String team) {
        boolean foundRaid = false;
        for (Map.Entry<String, Raid> raid : RAIDS.entrySet()) {
            // Check if the key starts with their faction
            if (raid.getKey().startsWith(team)) {
                // This is their faction
                // Get the time left
                long timeLeft = raid.getValue().expires - System.currentTimeMillis();
                String timeLeftString = convertTimeLeftToTimeStamp(timeLeft);
                source.sendSuccess(new TranslatableComponent(translationKey + "commands.raid.get", raid.getValue().target, timeLeftString).withStyle(ChatFormatting.AQUA), false);
                foundRaid = true;
            }
        }
        // If we got here, they don't have a raid
        if (!foundRaid) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.raid.no_raid"));
            return 0;
        }
        return 1;
    }

    private String convertTimeLeftToTimeStamp(long timeLeft) {
        // We just want a HH:MM:SS left, we could have over 24 hours
        long hours = timeLeft / (60 * 60 * 1000);
        long minutes = (timeLeft % (60 * 60 * 1000)) / (60 * 1000);
        long seconds = ((timeLeft % (60 * 60 * 1000)) % (60 * 1000)) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private int declareRaid(CommandSourceStack source, String targetTeam, int playerCount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (player.getTeam() == null) {
            // They're not on a team
            source.sendFailure(new TranslatableComponent(translationKey + "commands.raid.no_team"));
            return 0;
        }
        String team = player.getTeam().getName();
        // Check if they're declaring a raid on their own team
        if (team.equals(targetTeam)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.raid.raid_own_team"));
            return 0;
        }
        // Check if they're already in a raid against this faction
        if (RAIDS.containsKey(team + "->" + targetTeam)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.raid.already_raid"));
            return 0;
        }
        // Check if the player's of the target team is under playerCount, unless OP, then they bypass :)
        int targetTeamCount = getPlayersOfTeam(source.getServer(), targetTeam);
        if (playerCount > targetTeamCount && !source.hasPermission(2)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.raid.not_enough_players", targetTeam));
            return 0;
        }
        // We're good to start the raid against them
        Raid raid = createRaid(source.getServer(), team, targetTeam);
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.raid.declare", targetTeam, playerCount).withStyle(ChatFormatting.GREEN), false);
        String loggedMessage = String.format("%s (%s) has declared a raid against %s with %d players. At the time %s had %d players online.", player.getName().getString(), team, targetTeam, playerCount, targetTeam, targetTeamCount);
        LOGGER.info(loggedMessage);
        return 1;
    }

    private int getPlayersOfTeam(MinecraftServer server, String team) {
        return server.getPlayerList().getPlayers().stream().filter((player) -> Objects.requireNonNull(player.getTeam()).getName().equals(team)).toArray().length;
    }
}
