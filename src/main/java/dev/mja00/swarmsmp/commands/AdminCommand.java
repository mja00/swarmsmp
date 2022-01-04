package dev.mja00.swarmsmp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.mja00.swarmsmp.SSMPS2;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    public AdminCommand(CommandDispatcher<CommandSource> dispatcher) {

        dispatcher.register(Commands.literal("admin").requires((command) -> {
            return command.hasPermissionLevel(2);
        }).then(Commands.literal("set_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word()).executes((command) -> {
                return SetTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"));
        })))).then(Commands.literal("remove_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tags", StringArgumentType.word()).executes((command) -> {
            return RemoveTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tags"));
        })))).then(Commands.literal("check_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word()).executes((command) -> {
            return CheckTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"));
        })))).then(Commands.literal("start_duel").then(Commands.argument("first_player", EntityArgument.players()).then(Commands.argument("second_player", EntityArgument.players()).executes((command) -> {
            return StartDuel(command.getSource(), EntityArgument.getPlayers(command, "first_player"), EntityArgument.getPlayers(command, "second_player"));
        })))));

    }

    private int StartDuel(CommandSource source, Collection<ServerPlayerEntity> firstPlayers, Collection<ServerPlayerEntity> secondPlayers) {
        // Make sure firstPlayers and secondPlayers is only one player
        if (firstPlayers.size() != 1 || secondPlayers.size() != 1) {
            source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.admin.start_duel.error.players"), true);
            return 0;
        }

        ServerPlayerEntity firstPlayer = firstPlayers.iterator().next();
        CompoundNBT firstPlayerData = firstPlayer.getPersistentData();
        ServerPlayerEntity secondPlayer = secondPlayers.iterator().next();
        CompoundNBT secondPlayerData = secondPlayer.getPersistentData();

        // Make sure firstPlayer and secondPlayer are not the same
        if (firstPlayer.getUniqueID().equals(secondPlayer.getUniqueID())) {
            source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.admin.start_duel.error.same_player"), true);
            return 0;
        }

        // Make sure firstPlayer and secondPlayer are not already in a duel
        if (firstPlayerData.getBoolean(SSMPS2.MOD_ID + ":dueling") || secondPlayerData.getBoolean(SSMPS2.MOD_ID + ":dueling")) {
            source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.admin.start_duel.error.already_in_duel"), true);
            return 0;
        }

        boolean success = createDuelBetweenPlayers(firstPlayer, secondPlayer);

        if (success) {
            // Inform all the players on the server that a duel has started
            for (ServerPlayerEntity player : source.getServer().getPlayerList().getPlayers()) {
                player.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.admin.start_duel.inform_server", firstPlayer.getDisplayName(), secondPlayer.getDisplayName()).mergeStyle(TextFormatting.AQUA), player.getUniqueID());
            }
        }

        return 1;
    }

    private int SetTag(CommandSource source, Collection<ServerPlayerEntity> targets, String tag) {
        for (ServerPlayerEntity target : targets) {
            target.getPersistentData().putBoolean(SSMPS2.MOD_ID + ":" + tag, true);
        }

        if (targets.size() == 1) {
            source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.admin.set_tag.success.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.admin.set_tag.success.multiple", targets.size()), true);
        }
        return 1;
    }

    private int RemoveTag(CommandSource source, Collection<ServerPlayerEntity> targets, String tag) {
        for (ServerPlayerEntity target : targets) {
            target.getPersistentData().remove(SSMPS2.MOD_ID + ":" + tag);
        }

        if (targets.size() == 1) {
            source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.admin.remove_tag.success.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.admin.remove_tag.success.multiple", targets.size()), true);
        }
        return 1;
    }

    private int CheckTag(CommandSource source, Collection<ServerPlayerEntity> targets, String tag) {
        for (ServerPlayerEntity target : targets) {
            if (target.getPersistentData().contains(SSMPS2.MOD_ID + ":" + tag)) {
                source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.admin.check_tag.success", target.getDisplayName()), true);
            } else {
                source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.admin.check_tag.failed", target.getDisplayName()), true);
            }
        }
        return 1;
    }

    private static boolean createDuelBetweenPlayers(ServerPlayerEntity sourcePlayer, ServerPlayerEntity targetPlayer) {
        CompoundNBT sourcePlayerData = sourcePlayer.getPersistentData();
        CompoundNBT targetPlayerData = targetPlayer.getPersistentData();

        // Set both players dueling tag to true
        sourcePlayerData.putBoolean(SSMPS2.MOD_ID + ":dueling", true);
        targetPlayerData.putBoolean(SSMPS2.MOD_ID + ":dueling", true);

        // Save both players current health
        sourcePlayerData.putFloat(SSMPS2.MOD_ID + ":duel_health", sourcePlayer.getHealth());
        targetPlayerData.putFloat(SSMPS2.MOD_ID + ":duel_health", targetPlayer.getHealth());

        // Save both players current food level
        sourcePlayerData.putInt(SSMPS2.MOD_ID + ":duel_food", sourcePlayer.getFoodStats().getFoodLevel());
        targetPlayerData.putInt(SSMPS2.MOD_ID + ":duel_food", targetPlayer.getFoodStats().getFoodLevel());

        // Save both players current saturation
        sourcePlayerData.putFloat(SSMPS2.MOD_ID + ":duel_saturation", sourcePlayer.getFoodStats().getSaturationLevel());
        targetPlayerData.putFloat(SSMPS2.MOD_ID + ":duel_saturation", targetPlayer.getFoodStats().getSaturationLevel());

        // Save both players duel target
        sourcePlayerData.putUniqueId(SSMPS2.MOD_ID + ":duel_target", targetPlayer.getUniqueID());
        targetPlayerData.putUniqueId(SSMPS2.MOD_ID + ":duel_target", sourcePlayer.getUniqueID());

        // Set both players to server_duel true
        sourcePlayerData.putBoolean(SSMPS2.MOD_ID + ":server_duel", true);
        targetPlayerData.putBoolean(SSMPS2.MOD_ID + ":server_duel", true);

        // Set both players glowing true
        sourcePlayer.setGlowing(true);
        targetPlayer.setGlowing(true);

        // Inform both players the duel has started
        sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.started", targetPlayer.getDisplayName()).mergeStyle(TextFormatting.GREEN), sourcePlayer.getUniqueID());
        targetPlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.started", sourcePlayer.getDisplayName()).mergeStyle(TextFormatting.GREEN), targetPlayer.getUniqueID());

        return true;
    }
}
