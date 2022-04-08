package dev.mja00.swarmsmps2.helpers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class EntityHelpers {

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
}
