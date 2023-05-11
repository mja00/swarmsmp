package dev.mja00.swarmsmps2.helpers;

import dev.mja00.swarmsmps2.SSMPS2Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class EntityHelpers {

    static Logger LOGGER = LogManager.getLogger("EntityHelper");

    public static void giveTeamAndCreateIfNeeded(LivingEntity entity, String teamName) {
        // Get the scoreboard
        Scoreboard scoreboard = entity.getLevel().getScoreboard();
        // Get the team
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);

        // If the team doesn't exist, create it
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }

        // Add the player to the team
        scoreboard.addPlayerToTeam(entity.getStringUUID(), team);
    }

    public static void teleportServerPlayerToFactionSpawn(ServerPlayer player) {
        // We need to get the player's faction from their vanilla team
        Team playerTeam = player.getTeam();
        List<? extends Integer> defaultSpawnpoint = SSMPS2Config.SERVER.defaultSpawnpoint.get();
        BlockPos spawn = new BlockPos(defaultSpawnpoint.get(0), defaultSpawnpoint.get(1), defaultSpawnpoint.get(2));
        if (playerTeam == null) {
            // Just teleport them to the default spawn point
            player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
            LOGGER.info("Teleported player " + player.getName().getString() + " to default spawn point");
            return;
        }

        List<? extends Integer> spawnPoint = SSMPS2Config.getSpawnpointForFaction(playerTeam.getName());

        // Sanity check that spawnPoint is 3 elements long
        if (spawnPoint.size() != 3) {
            LOGGER.error("Spawn point for team " + playerTeam.getName() + " is not 3 elements long");
            player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
            LOGGER.info("Teleported player " + player.getName().getString() + " to default spawn point");
            return;
        }

        // Now we just teleport them to the spawn point
        player.teleportTo(spawnPoint.get(0), spawnPoint.get(1), spawnPoint.get(2));
        LOGGER.info("Teleported player " + player.getName().getString() + " to " + playerTeam.getName() + " spawn point");
    }
}
