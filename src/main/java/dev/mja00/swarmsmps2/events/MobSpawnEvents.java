package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.mja00.swarmsmps2.helpers.EntityHelpers.giveTeamAndCreateIfNeeded;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID)
public class MobSpawnEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static List<MobSpawnType> VALID_SPAWN_REASONS = List.of(
            MobSpawnType.SPAWN_EGG,
            MobSpawnType.COMMAND
    );

    @SubscribeEvent
    public static void setTeamOnMobSpawn(LivingSpawnEvent.SpecialSpawn event) {
        // Get the entity that spawned
        LivingEntity entity = event.getEntityLiving();
        EntityType<?> entityType = entity.getType();

        // Make a set for each type of mob
        Set<EntityType<?>> undeadTypes = new HashSet<>(List.of(
                EntityType.ZOMBIE,
                EntityType.SKELETON,
                EntityType.HUSK,
                EntityType.ZOMBIE_VILLAGER,
                EntityType.STRAY,
                EntityType.DROWNED,
                EntityType.SKELETON_HORSE
        ));
        Set<EntityType<?>> swarmTypes = new HashSet<>(List.of(
                EntityType.SPIDER,
                EntityType.CAVE_SPIDER,
                EntityType.SILVERFISH,
                EntityType.BEE
        ));
        Set<EntityType<?>> constructTypes = new HashSet<>(List.of(
                EntityType.CREEPER
        ));
        if (undeadTypes.contains(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        } else if (swarmTypes.contains(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "swarm");
        } else if (constructTypes.contains(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "construct");
        }
    }

    @SubscribeEvent
    public static void onSkeletonHorseSpawn(LivingSpawnEvent.SpecialSpawn event) {
        EntityType<?> entityType = event.getEntityLiving().getType();
        if (EntityType.SKELETON_HORSE.equals(entityType)) {
            if (!VALID_SPAWN_REASONS.contains(event.getSpawnReason())) {
                LOGGER.info("Skeleton horse was not spawned by a valid reason, cancelling spawn. Reason: " + event.getSpawnReason().toString());
                event.setCanceled(true);
            } else {
                LOGGER.info("Skeleton horse spawned! It spawned at X:" + event.getX() + " Y:" + event.getY() + " Z:" + event.getZ() + " with reason " + event.getSpawnReason().toString());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (!shouldSpawn(event)) {
            event.setCanceled(true);
        }
    }

    private static boolean shouldSpawn(LivingSpawnEvent.CheckSpawn event) {
        EntityType<?> entityType = event.getEntityLiving().getType();
        if (EntityType.SKELETON_HORSE.equals(entityType)) {
            if (!VALID_SPAWN_REASONS.contains(event.getSpawnReason())) {
                LOGGER.info("Skeleton horse was not spawned by a valid reason, cancelling spawn. Reason: " + event.getSpawnReason().toString());
                return false;
            } else {
                LOGGER.info("Skeleton horse spawned! It spawned at X:" + event.getX() + " Y:" + event.getY() + " Z:" + event.getZ() + " with reason " + event.getSpawnReason().toString());
                return true;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onDrownedConversion(LivingConversionEvent.Post event) {
        EntityType<?> entityType = event.getOutcome().getType();
        if (EntityType.DROWNED.equals(entityType)) {
            giveTeamAndCreateIfNeeded(event.getOutcome(), "undead");
        }
    }
}
