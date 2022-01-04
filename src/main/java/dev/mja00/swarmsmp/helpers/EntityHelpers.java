package dev.mja00.swarmsmp.helpers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

public class EntityHelpers {

    public static void giveTeamAndCreateIfNeeded(LivingEntity entity, String teamName) {
        // Get the scoreboard
        Scoreboard scoreboard = entity.getEntityWorld().getScoreboard();
        ScorePlayerTeam team = scoreboard.getTeam(teamName);

        // Check if team exists
        if (team == null) {
            team = scoreboard.createTeam(teamName);
        }

        // Set the entity's team
        scoreboard.addPlayerToTeam(entity.getCachedUniqueIdString(), team);

    }
}
