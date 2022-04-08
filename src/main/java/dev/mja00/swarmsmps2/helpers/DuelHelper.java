package dev.mja00.swarmsmps2.helpers;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class DuelHelper {

    private static final String MOD_ID = SwarmsmpS2.MODID;

    public static boolean createDuelBetweenPlayers(ServerPlayer firstPlayer, ServerPlayer secondPlayer, boolean isServerDuel) {
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

        if (isServerDuel) {
            // Set both players to server_duel true
            firstPlayerData.putBoolean(SwarmsmpS2.MODID + ":server_duel", true);
            secondPlayerData.putBoolean(SwarmsmpS2.MODID + ":server_duel", true);
        }

        // Set both players glowing true
        firstPlayer.setGlowingTag(true);
        secondPlayer.setGlowingTag(true);

        // Inform both players of the duel
        firstPlayer.sendMessage(new TranslatableComponent(SwarmsmpS2.translationKey + "commands.duel.started", secondPlayer.getDisplayName()).withStyle(ChatFormatting.GREEN), Util.NIL_UUID);
        secondPlayer.sendMessage(new TranslatableComponent(SwarmsmpS2.translationKey + "commands.duel.started", firstPlayer.getDisplayName()).withStyle(ChatFormatting.GREEN), Util.NIL_UUID);

        return true;
    }

    public static void endDuelBetweenPlayers(ServerPlayer initiatingPlayer, ServerPlayer secondPlayer) {
        // Reset stats
        resetStats(initiatingPlayer);
        resetStats(secondPlayer);

        // Remove tags
        removeDuelTags(initiatingPlayer);
        removeDuelTags(secondPlayer);

        // Inform both players of the duel end
        initiatingPlayer.sendMessage(new TranslatableComponent(SwarmsmpS2.translationKey + "dueling.forfeit").withStyle(ChatFormatting.GREEN), Util.NIL_UUID);
        secondPlayer.sendMessage(new TranslatableComponent(SwarmsmpS2.translationKey + "dueling.forfeit.opponent", initiatingPlayer.getDisplayName()).withStyle(ChatFormatting.GREEN), Util.NIL_UUID);
    }

    private static void removeDuelTags(Player player) {
        // Get their data
        CompoundTag persistentData = player.getPersistentData();
        persistentData.remove(MOD_ID + ":dueling");
        persistentData.remove(MOD_ID + ":duel_target");
        persistentData.remove(MOD_ID + ":duel_food");
        persistentData.remove(MOD_ID + ":duel_health");
        persistentData.remove(MOD_ID + ":duel_saturation");
        persistentData.remove(MOD_ID + ":server_duel");
        if (player.isCurrentlyGlowing()) { player.setGlowingTag(false); }
    }

    private static void resetStats(Player player) {
        // Get their previous values
        CompoundTag persistentData = player.getPersistentData();
        float prevHealth = persistentData.getFloat(MOD_ID + ":duel_health");
        int prevFood = persistentData.getInt(MOD_ID + ":duel_food");

        // Set their health and food to their previous values
        player.setHealth(prevHealth);
        player.getFoodData().setFoodLevel(prevFood);
    }
}
