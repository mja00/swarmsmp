package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Logger;

import static dev.mja00.swarmsmps2.helpers.EntityHelpers.giveTeamAndCreateIfNeeded;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID)
public class MobSpawnEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;

    @SubscribeEvent
    public static void setTeamOnMobSpawn(LivingSpawnEvent.SpecialSpawn event) {
        // Get the entity that spawned
        LivingEntity entity = event.getEntityLiving();
        EntityType<?> entityType = entity.getType();

        // Switch case to set the team of the mob
        if (EntityType.CREEPER.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "construct");
        } else if (EntityType.ZOMBIE.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        } else if (EntityType.SKELETON.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        } else if (EntityType.SPIDER.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "swarm");
        } else if (EntityType.CAVE_SPIDER.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "swarm");
        } else if (EntityType.HUSK.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        } else if (EntityType.ZOMBIE_VILLAGER.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        } else if (EntityType.STRAY.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        } else if (EntityType.DROWNED.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        } else if (EntityType.SILVERFISH.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "swarm");
        } else if (EntityType.BEE.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "swarm");
        }
    }

    @SubscribeEvent
    public static void onSkeletonHorseSpawn(LivingSpawnEvent.SpecialSpawn event) {
        EntityType<?> entityType = event.getEntityLiving().getType();
        if (EntityType.SKELETON_HORSE.equals(entityType)) {
            // Get the biome the horse spawned in
            Biome biome = event.getWorld().getBiome(event.getEntityLiving().blockPosition()).value();
            Biome taiga = ForgeRegistries.BIOMES.getValue(new ResourceLocation("swarmsmp:taiga"));
            if (taiga == null) {
                LOGGER.error("Could not find taiga biome. Will attempt to find vanilla's taiga biome.");

            }
            taiga = ForgeRegistries.BIOMES.getValue(new ResourceLocation("minecraft:taiga"));
            if (taiga == null ) {
                LOGGER.error("Could not find vanilla taiga biome. Will not stop spawn.");
                return;
            }
            if (biome.getRegistryName() == null || taiga.getRegistryName() == null) {
                LOGGER.error("Could not find registry name for biome. Will not stop spawn.");
                return;
            }
            if (!biome.getRegistryName().toString().equals(taiga.getRegistryName().toString())) {
                // Stop spawn
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
