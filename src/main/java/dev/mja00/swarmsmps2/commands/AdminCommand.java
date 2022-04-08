package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AdminCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;
    static final String translationKey = SwarmsmpS2.translationKey;

    public AdminCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("admin").requires((command) -> {
            return command.hasPermission(2);
        }).then(Commands.literal("set_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word()).executes((command) -> {
            return setTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"));
        })))).then(Commands.literal("remove_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word()).executes((command) -> {
            return removeTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"));
        })))).then(Commands.literal("check_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word()).executes((command) -> {
            return checkTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"));
        })))).then(Commands.literal("start_duel").then(Commands.argument("first_player", EntityArgument.players()).then(Commands.argument("second_player", EntityArgument.players()).executes((command) -> {
            return startDuel(command.getSource(), EntityArgument.getPlayers(command, "first_player"), EntityArgument.getPlayers(command, "second_player"));
        })))));
    }

    private int setTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag){
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

    private int removeTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag){
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

    private int checkTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag){
        for (ServerPlayer target : targets) {
            if (target.getPersistentData().contains(SwarmsmpS2.MODID + ":" + tag)) {
                source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.check_tag.success.single", target.getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.check_tag.success.none", target.getDisplayName()), true);
            }
        }
        return 1;
    }

    private int startDuel(CommandSourceStack source, Collection<ServerPlayer> firstPlayers, Collection<ServerPlayer> secondPlayers){
        // Make sure both collections are only 1 player
        if (firstPlayers.size() != 1 || secondPlayers.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.start_duel.error.players"));
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

        boolean success = createDuelBetweenPlayers(firstPlayer, secondPlayer);

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

    private static boolean createDuelBetweenPlayers(ServerPlayer firstPlayer, ServerPlayer secondPlayer) {
        // Player's data
        CompoundTag firstPlayerData = firstPlayer.getPersistentData();
        CompoundTag secondPlayerData = secondPlayer.getPersistentData();

        // Make sure they're not the same player
        if (firstPlayer.getUUID().equals(secondPlayer.getUUID())) {
            return false;
        }

        // Make sure they're both not already in a duel
        boolean firstPlayerInDuel = firstPlayerData.contains(SwarmsmpS2.MODID + ":dueling");
        boolean secondPlayerInDuel = secondPlayerData.contains(SwarmsmpS2.MODID + ":dueling");
        if (firstPlayerInDuel || secondPlayerInDuel) {
            return false;
        }

        // Set both players to be in a duel
        firstPlayerData.putBoolean(SwarmsmpS2.MODID + ":dueling", true);
        secondPlayerData.putBoolean(SwarmsmpS2.MODID + ":dueling", true);

        // Save both player's health
        firstPlayerData.putFloat(SwarmsmpS2.MODID + ":duel_health", firstPlayer.getHealth());
        secondPlayerData.putFloat(SwarmsmpS2.MODID + ":duel_health", secondPlayer.getHealth());

        // Save both player's current food level
        firstPlayerData.putInt(SwarmsmpS2.MODID + ":duel_food", firstPlayer.getFoodData().getFoodLevel());
        secondPlayerData.putInt(SwarmsmpS2.MODID + ":duel_food", secondPlayer.getFoodData().getFoodLevel());

        // Save both player's duel targets
        firstPlayerData.putUUID(SwarmsmpS2.MODID + ":duel_target", secondPlayer.getUUID());
        secondPlayerData.putUUID(SwarmsmpS2.MODID + ":duel_target", firstPlayer.getUUID());

        // Set both players to server_duel true
        firstPlayerData.putBoolean(SwarmsmpS2.MODID + ":server_duel", true);
        secondPlayerData.putBoolean(SwarmsmpS2.MODID + ":server_duel", true);

        // Set both players glowing true
        firstPlayer.setGlowingTag(true);
        secondPlayer.setGlowingTag(true);

        // Inform both players of the duel
        firstPlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.started", secondPlayer.getDisplayName()).withStyle(ChatFormatting.GREEN), DUMMY);
        secondPlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.started", firstPlayer.getDisplayName()).withStyle(ChatFormatting.GREEN), DUMMY);

        return true;
    }
}
