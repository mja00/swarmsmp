package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
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
    public static void frickSkeletonHorses(EntityJoinWorldEvent event) {
        if (event.loadedFromDisk()) return;
        EntityType<?> entityType = event.getEntity().getType();
        if (EntityType.SKELETON_HORSE.equals(entityType)) {
            Entity entity = event.getEntity();
            // Get the biome it spawned in
            BlockPos pos = entity.blockPosition();
            // Get the biome
            Biome biome = entity.level.getBiome(pos).value();
            // If it's a taiga biome or swarmsmp:fog_wastes biome, allow the spawn
            ResourceLocation biomeName = biome.getRegistryName();
            ResourceLocation taiga = new ResourceLocation("minecraft", "taiga");
            ResourceLocation fogWastes = new ResourceLocation("swarmsmp", "fog_wastes");
            if (biomeName != null && (biomeName.equals(taiga) || biomeName.equals(fogWastes))) {
                LOGGER.info("Skeleton horse spawned in a taiga biome or fog wastes biome, allowing spawn");
            } else {
                LOGGER.info("Skeleton horse spawned in a non-taiga biome or fog wastes biome, cancelling spawn");
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onDrownedConversion(LivingConversionEvent.Post event) {
        EntityType<?> entityType = event.getOutcome().getType();
        if (EntityType.DROWNED.equals(entityType)) {
            giveTeamAndCreateIfNeeded(event.getOutcome(), "undead");
        }
    }
}
