package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static dev.mja00.swarmsmps2.events.ChatEvents.getPlayerName;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DuelingEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    private static final String translationKey = SwarmsmpS2.translationKey;
    static final UUID DUMMY = Util.NIL_UUID;
    private static final String MOD_ID = SwarmsmpS2.MODID;

    private static void onDuelLost(Player player) {
        LOGGER.info("Player " + getPlayerName(player).getString() + " lost the duel");
        resetStats(player);
        if (!player.getLevel().isClientSide) {
            // Send the player a message
            player.sendMessage(new TranslatableComponent(translationKey + "dueling.died").withStyle(ChatFormatting.RED), DUMMY);
        }
        removeDuelTags(player);
    }

    private static void onDuelLostToNonPlayer(Player player) {
        LOGGER.info("Player " + getPlayerName(player).getString() + " lost the duel to a non-player");
        resetStats(player);
        if (!player.getLevel().isClientSide) {
            // Send the player a message
            player.sendMessage(new TranslatableComponent(translationKey + "dueling.died.non-player").withStyle(ChatFormatting.RED), DUMMY);
        }
        removeDuelTags(player);
    }

    private static void onDuelWon(Player player) {
        LOGGER.info("Player " + getPlayerName(player).getString() + " won the duel");
        resetStats(player);
        if (!player.getLevel().isClientSide) {
            // Send the player a message
            player.sendMessage(new TranslatableComponent(translationKey + "dueling.won").withStyle(ChatFormatting.GREEN), DUMMY);
        }
        removeDuelTags(player);
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

    @SubscribeEvent
    public static void showActionBarOnDuel(TickEvent.PlayerTickEvent event) {
        boolean is_dueling = event.player.getPersistentData().getBoolean(MOD_ID + ":dueling");
        if (is_dueling) {
            // Set the action bar message
            event.player.displayClientMessage(new TranslatableComponent(translationKey + "dueling.is_dueling").withStyle(ChatFormatting.AQUA), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        // If we allow dueling players to attack non-dueling we'll just return early on this
        if (SSMPS2Config.SERVER.allowDuelingPlayerToAttackNonDueling.get() || !SSMPS2Config.SERVER.enableDuels.get()) { return; }
        // Check if the player is attacking anything but a player
        if (!(event.getTarget() instanceof Player target)) { return; }
        Player player = event.getPlayer();
        CompoundTag playerData = player.getPersistentData();
        CompoundTag targetData = target.getPersistentData();

        // Check to make sure both players are dueling
        if (!targetData.getBoolean(MOD_ID + ":dueling") && playerData.getBoolean(MOD_ID + ":dueling")) {
            // Target is not dueling, but player is
            // Inform the player that they cannot attack this player
            if (!player.getLevel().isClientSide) {
                player.sendMessage(new TranslatableComponent(translationKey + "dueling.attack.cancelled").withStyle(ChatFormatting.RED), DUMMY);
            }
            // Cancel the event
            event.setCanceled(true);
        }

    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingHurtEvent event) {
        // If dueling isn't enabled, return early
        if (!SSMPS2Config.SERVER.enableDuels.get()) { return; }
        // Make sure the event is caused by a player
        if (event.getEntityLiving() instanceof Player player && event.getSource().getEntity() instanceof Player attacker) {
            // Get the player's data
            CompoundTag playerData = player.getPersistentData();
            CompoundTag attackerData = attacker.getPersistentData();
            // Get both players' dueling status
            boolean is_dueling = playerData.getBoolean(MOD_ID + ":dueling");
            boolean attacker_dueling = attackerData.getBoolean(MOD_ID + ":dueling");

            // If both players are dueling
            if (is_dueling && attacker_dueling) {
                // See if the damage would kill the player
                if (player.getHealth() - event.getAmount() <= 0) {
                    // The player would die from this, so the attacker wins
                    onDuelLost(player);
                    onDuelWon(attacker);
                    informServerOfDuelFinish(attacker, player);
                    event.setCanceled(true);
                }
            }

            // If only the player is dueling, and the attacker is not dueling
            if (is_dueling && !attacker_dueling) {
                // Would this kill the player?
                if (player.getHealth() - event.getAmount() <= 0) {
                    // The player would die from this, however the attacker isn't dueling, so we don't inform the attacker
                    event.setCanceled(true);
                    onDuelLost(player);

                    // Checks if the player had a duel target and lets them know they won
                    informDuelWinnerIfExists(player);
                }
            }
        } else if (event.getEntityLiving() instanceof Player player) {
            // This is the player dying from a non-playing entity
            // Get the player's data
            CompoundTag playerData = player.getPersistentData();

            // Check if the player is dueling
            if (playerData.getBoolean(MOD_ID + ":dueling")) {
                // Will this kill the player?
                if (player.getHealth() - event.getAmount() <= 0) {
                    // The player would die from this, so the attacker wins
                    event.setCanceled(true);
                    onDuelLostToNonPlayer(player);

                    // Checks if the player had a duel target and lets them know they won
                    informDuelWinnerIfExists(player);
                }
            }
        }
    }

    private static void informDuelWinnerIfExists(Player player) {
        // Get the player's data
        CompoundTag playerData = player.getPersistentData();
        if (playerData.contains(MOD_ID + ":duel_target")) {
            // Get the player's duel target
            UUID targetUUID = playerData.getUUID(MOD_ID + ":duel_target");
            // Get the target
            Player target = player.getLevel().getPlayerByUUID(targetUUID);

            // If the target exists
            if (target != null) {
                // Inform the target that they won
                onDuelWon(target);
            } else {
                // Log that the target doesn't exist
                LOGGER.warn("{} had a duel target that doesn't exist", player.getName().toString());
            }
        } else {
            LOGGER.debug("{} had no duel target", player.getName().toString());
        }
    }

    // If this duel was initiated as a server wide duel(events), we want to inform the entire server of the duel
    private static void informServerOfDuelFinish(Player winner, Player player2) {
        // Get the players' data
        CompoundTag player1Data = winner.getPersistentData();
        CompoundTag player2Data = player2.getPersistentData();
        // Check if both player's have the server_duel tag
        if (player1Data.getBoolean(MOD_ID + ":server_duel") && player2Data.getBoolean(MOD_ID + ":server_duel")) {
            // Inform the server that the duel is over
            // Loop through all players
            for (Player player: winner.getLevel().players()) {
                if (player != null) {
                    player.sendMessage(new TranslatableComponent(translationKey + "duel.server.finish", winner.getDisplayName(), player2.getDisplayName()).withStyle(ChatFormatting.GOLD), DUMMY);
                }
            }
        }

    }
}
