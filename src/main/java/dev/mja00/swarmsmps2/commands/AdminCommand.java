package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.helpers.DuelHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Stream;

import static dev.mja00.swarmsmps2.SSMPS2Config.getSpawnpointForFaction;

public class AdminCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;
    static final String translationKey = SwarmsmpS2.translationKey;
    static final List<String> FACTIONS = Arrays.asList("swarm", "construct", "undead", "natureborn", "default", "debug1" , "debug2", "debug3", "debug4", "debug5");

    // Our custom suggestions provider for faction names
    public static final SuggestionProvider<CommandSourceStack> FACTION_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(FACTIONS.stream(), builder);
    };

    public AdminCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("admin")
                .requires((command) -> command.hasPermission(2))
                .then(Commands.literal("set_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word())
                        .executes((command) -> setTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"))))))
                .then(Commands.literal("remove_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word())
                        .executes((command) -> removeTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"))))))
                .then(Commands.literal("check_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word())
                        .executes((command) -> checkTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"))))))
                .then(Commands.literal("start_duel").then(Commands.argument("first_player", EntityArgument.players()).then(Commands.argument("second_player", EntityArgument.players())
                        .executes((command) -> startDuel(command.getSource(), EntityArgument.getPlayers(command, "first_player"), EntityArgument.getPlayers(command, "second_player"))))))
                .then(Commands.literal("get_head").then(Commands.argument("target", EntityArgument.players())
                        .executes((command) -> giveHeadOfPlayer(command.getSource(), EntityArgument.getPlayers(command, "target")))))
                .then(Commands.literal("give_head").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("head_target", EntityArgument.players())
                        .executes(command -> giveHeadOfPlayerToPlayer(command.getSource(), EntityArgument.getPlayers(command, "target"), EntityArgument.getPlayers(command, "head_target"))))))
                .then(Commands.literal("end_duel").then(Commands.argument("player", EntityArgument.players())
                        .executes((command) -> endDuel(command.getSource(), EntityArgument.getPlayers(command, "player")))))
                .then(Commands.literal("deaths")
                        .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> getPlayerDeathCount(command.getSource(), EntityArgument.getPlayers(command, "player")))))
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.players()).then(Commands.argument("count", IntegerArgumentType.integer())
                                .executes((command) -> setPlayerDeathCount(command.getSource(), EntityArgument.getPlayers(command, "player"), IntegerArgumentType.getInteger(command, "count"))))))
                        .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> resetPlayerDeathCount(command.getSource(), EntityArgument.getPlayers(command, "player"))))))
                .then(Commands.literal("factions")
                        .then(Commands.literal("spawn").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTION_SUGGESTIONS)
                                .executes((command) -> teleportToFactionSpawn(command.getSource(), StringArgumentType.getString(command, "faction")))
                                .then(Commands.argument("player", EntityArgument.players())
                                    .executes((command) -> teleportPlayersToFactionSpawn(command.getSource(), StringArgumentType.getString(command, "faction"), EntityArgument.getPlayers(command, "player")))))))
                .then(Commands.literal("items")
                        .then(Commands.literal("get_tags")
                                .executes((command) -> getTagsForItem(command.getSource()))))
                .then(Commands.literal("config")
                        .then(Commands.literal("edit")
                                .then(Commands.literal("bypass")
                                        .then(Commands.literal("add").then(Commands.argument("uuid", StringArgumentType.word())
                                                .executes((command) -> addUUIDToBypass(command.getSource(), StringArgumentType.getString(command, "uuid")))))
                                        .then(Commands.literal("remove").then(Commands.argument("uuid", StringArgumentType.word())
                                                .executes((command) -> removeUUIDFromBypass(command.getSource(), StringArgumentType.getString(command, "uuid"))))))
                                .then(Commands.literal("spawnpoint")
                                        .then(Commands.literal("set").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTION_SUGGESTIONS).then(Commands.argument("location", Vec3Argument.vec3())
                                                .executes((command) -> setFactionSpawnpoint(command.getSource(), StringArgumentType.getString(command, "faction"), Vec3Argument.getCoordinates(command, "location"))))))
                                        .then(Commands.literal("get").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTION_SUGGESTIONS)
                                                .executes((command) -> getFactionSpawnpoint(command.getSource(), StringArgumentType.getString(command, "faction")))))))
                        .then(Commands.literal("reload")
                                .executes((command) -> reloadConfigFile(command.getSource())))));
    }

    private int addUUIDToBypass(CommandSourceStack source, String uuid) {
        // Get our current list of bypassed UUIDs from the config
        List<? extends String> bypassedUUIDs = SSMPS2Config.SERVER.bypassedPlayers.get();
        // Check if the UUID is already in the list
        if (bypassedUUIDs.contains(uuid)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.config.bypass.error.already_bypassed", uuid));
            return 0;
        }
        // We'll need to make a new list to add the UUID to
        List<String> newBypassedUUIDs = new ArrayList<>(bypassedUUIDs);
        newBypassedUUIDs.add(uuid);
        // Update the config
        SSMPS2Config.SERVER.bypassedPlayers.set(newBypassedUUIDs);
        SSMPS2Config.SERVER.bypassedPlayers.save();
        // Inform the user
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.config.bypass.add", uuid), true);
        return 1;
    }

    private int removeUUIDFromBypass(CommandSourceStack source, String uuid) {
        // Get our current list
        List<? extends String> bypassedUUIDs = SSMPS2Config.SERVER.bypassedPlayers.get();
        // Check if the UUID is in the list
        if (!bypassedUUIDs.contains(uuid)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.config.bypass.error.not_bypassed", uuid));
            return 0;
        }
        // We'll need to make a new list to remove the UUID from
        List<String> newBypassedUUIDs = new ArrayList<>(bypassedUUIDs);
        newBypassedUUIDs.remove(uuid);
        // Update the config
        SSMPS2Config.SERVER.bypassedPlayers.set(newBypassedUUIDs);
        SSMPS2Config.SERVER.bypassedPlayers.save();
        // Inform the user
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.config.bypass.remove", uuid), true);
        return 1;
    }

    private int getFactionSpawnpoint(CommandSourceStack source, String spawnpoint) {
        // Check if the faction is in the valid list
        if (!FACTIONS.contains(spawnpoint)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn.error.invalid_faction", spawnpoint));
            return 0;
        }
        // Get the spawn point for the faction
        List<? extends Integer> spawnPoint = getSpawnpointForFaction(spawnpoint);

        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.config.spawnpoint.get", spawnpoint, spawnPoint.get(0), spawnPoint.get(1), spawnPoint.get(2)), false);
        return 1;
    }

    private int setFactionSpawnpoint(CommandSourceStack source, String faction, Coordinates pPosition) throws CommandSyntaxException{
        // Check if the faction is in the valid list
        if (!FACTIONS.contains(faction)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn.error.invalid_faction", faction));
            return 0;
        }
        Vec3 vec3 = pPosition.getPosition(source);
        // Create a list to store the spawn point
        List<Integer> spawnPoint = new ArrayList<>();
        // Add the coordinates to the list
        spawnPoint.add((int) vec3.x);
        spawnPoint.add((int) vec3.y);
        spawnPoint.add((int) vec3.z);
        SSMPS2Config.setSpawnpointForFaction(faction, spawnPoint);
        // Inform the user
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.config.spawnpoint.set", faction, spawnPoint.get(0), spawnPoint.get(1), spawnPoint.get(2)), true);
        return 1;
    }

    private int getTagsForItem(CommandSourceStack source) throws CommandSyntaxException {
        // Get the player
        ServerPlayer player = source.getPlayerOrException();
        // Get the item in the player's main hand
        ItemStack item = player.getMainHandItem();
        // Check if their hand is empty
        if (item.isEmpty()) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.items.get_tags.empty_hand").withStyle(ChatFormatting.RED));
            return 0;
        }
        // Get the tags for the item they're holding
        Stream<TagKey<Item>> tags = item.getTags();
        // Convert the tags to a list
        List<TagKey<Item>> tagList = tags.toList();
        // Check if the item has any tags
        if (tagList.isEmpty()) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.items.get_tags.no_tags").withStyle(ChatFormatting.RED));
            return 0;
        }
        // Send the tags in a list to the player
        StringBuilder tagsToSend = new StringBuilder();
        for (TagKey<Item> tag : tagList) {
            tagsToSend.append(tag.location()).append(", ");
        }
        // Remove the last comma and space
        tagsToSend.delete(tagsToSend.length() - 2, tagsToSend.length());
        // Send it to the player
        player.sendMessage(new TranslatableComponent(translationKey + "commands.admin.items.get_tags.success", tagsToSend.toString()).withStyle(ChatFormatting.AQUA), Util.NIL_UUID);
        return 1;
    }

    private int giveHeadOfPlayer(CommandSourceStack source, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        int howMuchHeadTheyGot = 0;
        for (ServerPlayer target : targets) {
            CompoundTag headData = new CompoundTag();
            headData.putString("SkullOwner", target.getName().getString());
            ItemStack headItem = new ItemStack(Items.PLAYER_HEAD, 1);
            headItem.setTag(headData);

            boolean success = source.getPlayerOrException().getInventory().add(headItem);
            if (!success) {
                source.getPlayerOrException().spawnAtLocation(headItem);
            }
            howMuchHeadTheyGot++;
        }
        if (howMuchHeadTheyGot == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.head.success").withStyle(ChatFormatting.AQUA), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.head.success.multiple", howMuchHeadTheyGot)
                            .withStyle(ChatFormatting.AQUA)
                            .withStyle(ChatFormatting.BOLD)
                            .withStyle(ChatFormatting.ITALIC)
                            .withStyle(ChatFormatting.UNDERLINE),
                    true);
        }
        return 1;
    }

    private int giveHeadOfPlayerToPlayer(CommandSourceStack source, Collection<ServerPlayer> targets, Collection<ServerPlayer> headTargets) throws CommandSyntaxException {
        int howMuchHeadTheyGot = 0;

        // Make sure headTargets is only one player
        if (headTargets.size() > 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.head.failure.multiple_heads"));
            return 0;
        }

        ServerPlayer headTarget = headTargets.iterator().next();

        for (ServerPlayer target : targets) {
            ItemStack headItem = createHead(target);

            boolean success = headTarget.getInventory().add(headItem);
            if (!success) {
                headTarget.spawnAtLocation(headItem);
            }
            howMuchHeadTheyGot++;
        }
        if (howMuchHeadTheyGot == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.head.sent_success").withStyle(ChatFormatting.AQUA), true);
            headTarget.sendMessage(new TranslatableComponent(translationKey + "commands.admin.head.success").withStyle(ChatFormatting.AQUA), Util.NIL_UUID);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.head.sent_success.multiple", howMuchHeadTheyGot)
                            .withStyle(ChatFormatting.AQUA)
                            .withStyle(ChatFormatting.BOLD)
                            .withStyle(ChatFormatting.ITALIC)
                            .withStyle(ChatFormatting.UNDERLINE),
                    true);
            headTarget.sendMessage(new TranslatableComponent(translationKey + "commands.admin.head.success.multiple", howMuchHeadTheyGot)
                            .withStyle(ChatFormatting.AQUA)
                            .withStyle(ChatFormatting.BOLD)
                            .withStyle(ChatFormatting.ITALIC)
                            .withStyle(ChatFormatting.UNDERLINE),
                    Util.NIL_UUID);
        }
        return 1;
    }

    private ItemStack createHead(ServerPlayer player) {
        CompoundTag headData = new CompoundTag();
        headData.putString("SkullOwner", player.getName().getString());
        ItemStack headItem = new ItemStack(Items.PLAYER_HEAD, 1);
        headItem.setTag(headData);
        return headItem;
    }

    private int setTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag) {
        for (ServerPlayer target : targets) {
            target.getPersistentData().putBoolean(SwarmsmpS2.MODID + ":" + tag, true);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.set_tag.success.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.set_tag.success.multiple", targets.size()), true);
        }
        return 1;
    }

    private int removeTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag) {
        for (ServerPlayer target : targets) {
            target.getPersistentData().remove(SwarmsmpS2.MODID + ":" + tag);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.remove_tag.success.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.remove_tag.success.multiple", targets.size()), true);
        }
        return 1;
    }

    private int checkTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag) {
        for (ServerPlayer target : targets) {
            if (target.getPersistentData().contains(SwarmsmpS2.MODID + ":" + tag)) {
                source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.check_tag.success", target.getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.check_tag.failed", target.getDisplayName()), true);
            }
        }
        return 1;
    }

    private int startDuel(CommandSourceStack source, Collection<ServerPlayer> firstPlayers, Collection<ServerPlayer> secondPlayers) {
        // Make sure both collections are only 1 player
        if (firstPlayers.size() != 1 || secondPlayers.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.error.players"));
            return 0;
        }

        ServerPlayer firstPlayer = firstPlayers.iterator().next();
        ServerPlayer secondPlayer = secondPlayers.iterator().next();

        // Player's data
        CompoundTag firstPlayerData = firstPlayer.getPersistentData();
        CompoundTag secondPlayerData = secondPlayer.getPersistentData();

        // Make sure they're not the same player
        if (firstPlayer.getUUID().equals(secondPlayer.getUUID())) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.start_duel.error.same_player"));
            return 0;
        }

        // Make sure they're both not already in a duel
        boolean firstPlayerInDuel = firstPlayerData.contains(SwarmsmpS2.MODID + ":dueling");
        boolean secondPlayerInDuel = secondPlayerData.contains(SwarmsmpS2.MODID + ":dueling");
        if (firstPlayerInDuel || secondPlayerInDuel) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.start_duel.error.already_in_duel"));
            return 0;
        }

        boolean success = DuelHelper.createDuelBetweenPlayers(firstPlayer, secondPlayer, true);

        if (success) {
            // Inform all players of the duel
            List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
            players.forEach(player -> {
                player.sendMessage(new TranslatableComponent(
                        translationKey + "commands.admin.start_duel.inform_server",
                        firstPlayer.getDisplayName(),
                        secondPlayer.getDisplayName()
                ).withStyle(ChatFormatting.AQUA), DUMMY);
            });
        }

        return 1;
    }

    private int endDuel(CommandSourceStack source, Collection<ServerPlayer> player) {
        // Ensure the collection is just 1 player
        if (player.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.error.players"));
            return 0;
        }
        // Check if they're in a duel
        ServerPlayer playerInDuel = player.iterator().next();
        boolean isInDuel = playerInDuel.getPersistentData().contains(SwarmsmpS2.MODID + ":dueling");
        if (!isInDuel) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.end_duel.error.not_in_duel"));
            return 0;
        }

        // Player must be in a duel so we have our first player
        // We'll need to check their "duel_target" persistent data to get the second player
        UUID secondPlayerUUID = playerInDuel.getPersistentData().getUUID(SwarmsmpS2.MODID + ":duel_target");
        // Get a player from this UUID
        ServerPlayer secondPlayer = source.getServer().getPlayerList().getPlayer(secondPlayerUUID);
        // End the duel
        DuelHelper.endDuelBetweenPlayers(playerInDuel, secondPlayer, true);

        return 1;
    }

    private int getPlayerDeathCount(CommandSourceStack source, Collection<ServerPlayer> players) {
        LOGGER.debug("Command init");
        if (players.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.error.players"));
            return 0;
        }

        ServerPlayer playerToCheck = players.iterator().next();
        LOGGER.debug("Checking death count for " + playerToCheck.getDisplayName().getString());
        // Get their persistent data
        CompoundTag playerData = playerToCheck.getPersistentData();
        LOGGER.debug("Got persistent data");
        // Get their death count
        int deathCount = playerData.getInt(SwarmsmpS2.MODID + ":death_count");
        LOGGER.debug("Death count for " + playerToCheck.getDisplayName().getString() + " is " + deathCount);

        // Tell the initiator of the command the death count
        if (deathCount == 0) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.get_deaths.none", playerToCheck.getDisplayName()), false);
        } else if (deathCount == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.get_deaths", playerToCheck.getDisplayName(), deathCount), false);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.get_deaths.multiple", playerToCheck.getDisplayName(), deathCount), false);
        }

        return 1;
    }

    private int setPlayerDeathCount(CommandSourceStack source, Collection<ServerPlayer> players, int deathCount) {
        if (players.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.error.players"));
            return 0;
        }

        ServerPlayer playerToSet = players.iterator().next();
        // Get their persistent data
        CompoundTag playerData = playerToSet.getPersistentData();
        // Set their death count
        playerData.putInt(SwarmsmpS2.MODID + ":death_count", deathCount);

        // Tell the initiator of the command the death count
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.set_deaths", playerToSet.getDisplayName(), deathCount), false);
        return 1;
    }

    private int resetPlayerDeathCount(CommandSourceStack source, Collection<ServerPlayer> players) {
        if (players.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.error.players"));
            return 0;
        }

        ServerPlayer playerToReset = players.iterator().next();
        // Get their persistent data
        CompoundTag playerData = playerToReset.getPersistentData();
        // Reset their death count
        playerData.putInt(SwarmsmpS2.MODID + ":death_count", 0);

        // Tell the initiator
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.reset_deaths", playerToReset.getDisplayName()), false);
        return 1;
    }

    private int teleportToFactionSpawn(CommandSourceStack source, String faction) throws CommandSyntaxException {
        // Check if the faction is in the valid list
        if (!FACTIONS.contains(faction)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn.error.invalid_faction", faction));
            return 0;
        }
        // Get the invoking player
        ServerPlayer player = source.getPlayerOrException();
        List<? extends Integer> spawnPoint = getSpawnpointForFaction(faction);

        // Now we just teleport them to the spawn point
        player.teleportTo(spawnPoint.get(0), spawnPoint.get(1), spawnPoint.get(2));
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn", player.getDisplayName(), faction), true);
        return 1;
    }

    private int teleportPlayersToFactionSpawn(CommandSourceStack source, String faction, Collection<ServerPlayer> players) {
        // Check if the faction is in the valid list
        if (!FACTIONS.contains(faction)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn.error.invalid_faction", faction));
            return 0;
        }
        // Get the spawn point for the faction
        List<? extends Integer> spawnPoint = getSpawnpointForFaction(faction);
        // Loop through the players and teleport them
        players.forEach(player -> {
            player.teleportTo(spawnPoint.get(0), spawnPoint.get(1), spawnPoint.get(2));
        });

        if (players.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn", players.iterator().next().getDisplayName(), faction), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn.multiple", players.size(), faction), true);
        }
        return 1;
    }

    private int reloadConfigFile(CommandSourceStack source) {
        // Send an OnConfigChangeEvent to the event bus
        SSMPS2Config.serverSpec.afterReload();
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.reload_config"), true);
        return 1;
    }
}
