package dev.mja00.swarmsmp.events;

import dev.mja00.swarmsmp.SSMPS2;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = SSMPS2.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DuelingEvents {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String translationKey = SSMPS2.MOD_ID + ".";

    public static void onDuelLost(PlayerEntity player) {
        LOGGER.info("Player " + player.getDisplayName().getString() + " has lost the duel");
        player.getPersistentData().remove(SSMPS2.MOD_ID + ":dueling");
        player.setHealth(20);
        if (!player.getEntityWorld().isRemote) {
            player.sendMessage(new TranslationTextComponent(translationKey + "dueling.died").mergeStyle(TextFormatting.RED), player.getUniqueID());
        }
    }

    public static void onDuelLostToNonPlayer(PlayerEntity player) {
        LOGGER.info("Player " + player.getDisplayName().getString() + " has lost the duel");
        player.getPersistentData().remove(SSMPS2.MOD_ID + ":dueling");
        if (!player.getEntityWorld().isRemote) {
            player.sendMessage(new TranslationTextComponent(translationKey + "dueling.died.non-player").mergeStyle(TextFormatting.RED), player.getUniqueID());
        }
    }

    public static void onDuelWon(PlayerEntity player) {
        LOGGER.info("Player " + player.getDisplayName().getString() + " has won the duel");
        player.getPersistentData().remove(SSMPS2.MOD_ID + ":dueling");
        player.setHealth(20);
        if (!player.getEntityWorld().isRemote) {
            player.sendMessage(new TranslationTextComponent(translationKey + "dueling.won").mergeStyle(TextFormatting.GREEN), player.getUniqueID());
        }
    }

    @SubscribeEvent
    public static void showActionBarDuelWhenDueling(TickEvent.PlayerTickEvent event) {
        boolean is_dueling = event.player.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":dueling");
        if(is_dueling) {
            event.player.sendStatusMessage(new TranslationTextComponent(translationKey + "dueling.is_dueling"), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingHurtEvent event) {
        // Check to see if it's a player
        if (event.getEntityLiving() instanceof PlayerEntity && event.getSource().getTrueSource() instanceof PlayerEntity) {
            // Get the player
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            // Get the attacker
            PlayerEntity attacker =(PlayerEntity) event.getSource().getTrueSource();

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
                }
            }
            // Second case, the player is dueling and the attacker isn't
            else if (player_is_dueling) {
                // Check to see if the damage would kill the player
                if (player.getHealth() - event.getAmount() <= 0) {
                    // Player is dead, so the attacker wins but since they're not dueling, they aren't notified
                    // Just remove the dueling tag and let the player die
                    player.getPersistentData().remove(SSMPS2.MOD_ID + ":dueling");
                }
            }
            // Third case, the attacker is dueling and the player isn't
            else if (attacker_is_dueling) {
                // Ignore the damage
                event.setCanceled(true);
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
                    // Player is dead, but died to a non-player, so the player loses, and we still kill them but let them know they lost
                    onDuelLostToNonPlayer(player);
                }
            }
        }
    }
}
