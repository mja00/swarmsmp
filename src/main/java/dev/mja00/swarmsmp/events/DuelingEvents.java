package dev.mja00.swarmsmp.events;

import dev.mja00.swarmsmp.SSMPS2;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = SSMPS2.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DuelingEvents {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String translationKey = SSMPS2.MOD_ID + ".";

    private static void onDuelLost(PlayerEntity player) {
        LOGGER.info("Player " + player.getDisplayName().getString() + " has lost the duel");

        // Get previous values
        float prevHealth = player.getPersistentData().getFloat(SSMPS2.MOD_ID + ":duel_health");
        int prevFood = player.getPersistentData().getInt(SSMPS2.MOD_ID + ":duel_food");
        // Set those values back
        player.setHealth(prevHealth);
        player.getFoodStats().setFoodLevel(prevFood);

        player.setGlowing(false);

        if (!player.getEntityWorld().isRemote) {
            player.sendMessage(new TranslationTextComponent(translationKey + "dueling.died").mergeStyle(TextFormatting.RED), player.getUniqueID());
        }
        removeDuelTags(player);
    }

    private static void onDuelLostToNonPlayer(PlayerEntity player) {
        LOGGER.info("Player " + player.getDisplayName().getString() + " has lost the duel");
        if (player.isGlowing()) {player.setGlowing(false);}
        if (!player.getEntityWorld().isRemote) {
            player.sendMessage(new TranslationTextComponent(translationKey + "dueling.died.non-player").mergeStyle(TextFormatting.RED), player.getUniqueID());
        }
        removeDuelTags(player);
    }

    private static void onDuelWon(PlayerEntity player) {
        LOGGER.info("Player " + player.getDisplayName().getString() + " has won the duel");
        // Get previous values
        float prevHealth = player.getPersistentData().getFloat(SSMPS2.MOD_ID + ":duel_health");
        int prevFood = player.getPersistentData().getInt(SSMPS2.MOD_ID + ":duel_food");
        // Set those values back
        player.setHealth(prevHealth);
        player.getFoodStats().setFoodLevel(prevFood);

        player.setGlowing(false);

        if (!player.getEntityWorld().isRemote) {
            player.sendMessage(new TranslationTextComponent(translationKey + "dueling.won").mergeStyle(TextFormatting.GREEN), player.getUniqueID());
        }
        removeDuelTags(player);
    }

    private static void removeDuelTags(PlayerEntity player) {
        player.getPersistentData().remove(SSMPS2.MOD_ID + ":dueling");
        player.getPersistentData().remove(SSMPS2.MOD_ID + ":duel_target");
        player.getPersistentData().remove(SSMPS2.MOD_ID + ":duel_food");
        player.getPersistentData().remove(SSMPS2.MOD_ID + ":duel_health");
        player.getPersistentData().remove(SSMPS2.MOD_ID + ":duel_saturation");
    }

    @SubscribeEvent
    public static void showActionBarDuelWhenDueling(TickEvent.PlayerTickEvent event) {
        boolean is_dueling = event.player.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":dueling");
        if(is_dueling) {
            event.player.sendStatusMessage(new TranslationTextComponent(translationKey + "dueling.is_dueling").mergeStyle(TextFormatting.AQUA), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof PlayerEntity)) { return; }
        PlayerEntity player = event.getPlayer();
        PlayerEntity target = (PlayerEntity) event.getTarget();
        // Check to see if the target is not dueling and the player is dueling cancel the event
        if (!target.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":dueling") && player.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":dueling")) {
            if (!player.getEntityWorld().isRemote) {
                player.sendMessage(new TranslationTextComponent(translationKey + "dueling.attack.cancelled").mergeStyle(TextFormatting.RED), player.getUniqueID());
            }
            event.setCanceled(true);
        }



    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingHurtEvent event) {
        // Check to see if it's a player
        if (event.getEntityLiving() instanceof PlayerEntity && event.getSource().getTrueSource() instanceof PlayerEntity) {
            // Get the player
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            // Get the attacker
            PlayerEntity attacker = (PlayerEntity) event.getSource().getTrueSource();

            // Check to see if the player is currently dueling
            boolean player_is_dueling = player.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":dueling");
            boolean attacker_is_dueling = attacker.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":dueling");
            // First case, both players are dueling
            if (player_is_dueling && attacker_is_dueling) {
                // Check to see if the damage would kill the player
                if (player.getHealth() - event.getAmount() <= 0) {
                    // Player is dead, so the attacker wins
                    event.setCanceled(true);
                    onDuelLost(player);
                    onDuelWon(attacker);
                    informServerOfDuelFinish(attacker, player);
                }
            }
            // Second case, the player is dueling and the attacker isn't
            else if (player_is_dueling) {
                // Check to see if the damage would kill the player
                if (player.getHealth() - event.getAmount() <= 0) {
                    // Player is dead, so the attacker wins but since they're not dueling, they aren't notified
                    // Just remove the dueling tags and let the player die
                    removeDuelTags(player);

                    // Inform attacker that they won just in case there was one
                    informDuelWinnerIfExists(player);
                }
            }
        }
        // Check to make sure it's still a player
        else if (event.getEntityLiving() instanceof PlayerEntity) {
            // Get the player
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            // Check to see if the player is currently dueling
            boolean player_is_dueling = player.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":dueling");
            if (player_is_dueling) {
                // Check to see if the damage would kill the player
                if (player.getHealth() - event.getAmount() <= 0) {
                    // Player is dead, but died to a non-player, so the player loses, however we'll let the live
                    // and inform the attacker that they won just in case there was one
                    event.setCanceled(true);
                    onDuelLostToNonPlayer(player);
                    informDuelWinnerIfExists(player);
                }
            }
        }
    }

    private static void informDuelWinnerIfExists(PlayerEntity player) {
        // Get the duel_target of the player
        UUID duel_target = player.getPersistentData().getUniqueId(SSMPS2.MOD_ID + ":duel_target");
        // Get the player that the player was dueling with
        PlayerEntity duel_target_player = player.getEntityWorld().getPlayerByUuid(duel_target);
        // Check to see if the duel_target_player is still online
        if (duel_target_player != null) {
            onDuelWon(duel_target_player);

        }
    }

    private static void informServerOfDuelFinish(PlayerEntity player1, PlayerEntity player2) {
        // Check to see if both players have the server_duel tag
        boolean player1_has_server_duel_tag = player1.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":server_duel");
        boolean player2_has_server_duel_tag = player2.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":server_duel");
        // If both players have the server_duel tag, then we'll send the duel finish packet to the server
        if (player1_has_server_duel_tag && player2_has_server_duel_tag) {
            // Loop through all the players and inform them of the duel finish
            for (PlayerEntity player : player1.getEntityWorld().getPlayers()) {
                // Check to see if the player is online
                if (player != null) {
                    // Send the player a message
                    player.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "duel.server.finish", player1.getDisplayName(), player2.getDisplayName()).mergeStyle(TextFormatting.GOLD), player.getUniqueID());
                }
            }
        }
    }
}
