package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
            giveTeamAndCreateIfNeeded(entity, "bugkin");
        } else if (EntityType.CAVE_SPIDER.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "bugkin");
        } else if (EntityType.HUSK.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        } else if (EntityType.ZOMBIE_VILLAGER.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        } else if (EntityType.STRAY.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        } else if (EntityType.DROWNED.equals(entityType)) {
            giveTeamAndCreateIfNeeded(entity, "undead");
        }
    }
}
