package dev.mja00.swarmsmps2.helpers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
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
            LOGGER.info("Teleported player " + player.getName().getString() + " to default spawn point due to not having a team!");
            return;
        }

        List<? extends Integer> spawnPoint = SSMPS2Config.getSpawnpointForFaction(playerTeam.getName());

        // Sanity check that spawnPoint is 3 elements long
        if (spawnPoint.size() != 3) {
            LOGGER.error("Spawn point for team " + playerTeam.getName() + " is not 3 elements long");
            player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
            LOGGER.info("Teleported player " + player.getName().getString() + " to default spawn point due to spawn point not being 3 elements long!");
            return;
        }

        // Now we just teleport them to the spawn point
        player.teleportTo(spawnPoint.get(0), spawnPoint.get(1), spawnPoint.get(2));
        LOGGER.info("Teleported player " + player.getName().getString() + " to " + playerTeam.getName() + " spawn point!");
    }

    public static ItemStack getPlayerHead(ServerPlayer player) {
        // Create a new item stack with the head of the player's skin at the time
        CompoundTag headData = new CompoundTag();
        CompoundTag skullOwner = new CompoundTag();
        CompoundTag properties = new CompoundTag();
        // Convert the player's UUID into an int array from most to least significant
        long mostSigBits = player.getUUID().getMostSignificantBits();
        long leastSigBits = player.getUUID().getLeastSignificantBits();
        int[] uuidIntArray = new int[] {
                (int) (mostSigBits >> 32),
                (int) mostSigBits,
                (int) (leastSigBits >> 32),
                (int) leastSigBits
        };
        skullOwner.putIntArray("Id", uuidIntArray);
        HashMap<String, String> textures = new HashMap<>();
        // We want the texture's value from the player's properties
        player.getGameProfile().getProperties().get("textures").forEach(property -> {
            textures.put(property.getName(), property.getValue());
        });
        // Now we've got a json object encoded as b64 that we need to pull some stuff from
        String textureValue = textures.get("textures");
        String decodedTextureValue = new String(java.util.Base64.getDecoder().decode(textureValue));
        // Now we've got a json object that we need to pull some stuff from
        JsonObject obj = JsonParser.parseString(decodedTextureValue).getAsJsonObject();
        String skinUrl = obj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        String skinUrlObj = "{textures:{SKIN:{url:\"" + skinUrl + "\"}}}";
        // Re-encode the json object as b64
        String encodedSkinUrlObj = java.util.Base64.getEncoder().encodeToString(skinUrlObj.getBytes());
        // Now we do something kinda gross looking ngl
        CompoundTag url = new CompoundTag();
        url.putString("Value", encodedSkinUrlObj);
        ListTag texturesList = new ListTag();
        texturesList.add(url);
        properties.put("textures", texturesList);
        skullOwner.put("Properties", properties);
        headData.put("SkullOwner", skullOwner);
        ItemStack headItem = new ItemStack(Items.PLAYER_HEAD, 1);
        headItem.setTag(headData);
        return headItem;
    }

    public static void addParticlesAroundSelf(ParticleOptions particleOptions, Level level, Player player, int count, int verticalOffset) {
        for (int i = 0; i < count; ++i) {
            double d0 = level.getRandom().nextGaussian() * 0.02D;
            double d1 = level.getRandom().nextGaussian() * 0.02D;
            double d2 = level.getRandom().nextGaussian() * 0.02D;
            double randomY = level.getRandom().nextInt(verticalOffset) + player.getY();
            level.addParticle(particleOptions, player.getRandomX(1), randomY, player.getRandomZ(1), d0, d1, d2);
        }
    }

    public static void addParticlesAroundSelfServer(ParticleOptions particleOptions, ServerLevel level, Player player, int count, int verticalOffset) {
        for (int i = 0; i < count; ++i) {
            double d0 = level.getRandom().nextGaussian() * 0.02D;
            double d1 = level.getRandom().nextGaussian() * 0.02D;
            double d2 = level.getRandom().nextGaussian() * 0.02D;
            double randomY = level.getRandom().nextInt(verticalOffset) + player.getY();
            level.sendParticles(particleOptions, player.getRandomX(1), randomY, player.getRandomZ(1), 1, d0, d1, d2, 0.0D);
        }
    }

    public static void addParticlesAroundSelf(ParticleOptions particleOptions, Level level, Player player, int count) {
        addParticlesAroundSelf(particleOptions, level, player, count, 2);
    }

    public static void addParticlesAroundSelfServer(ParticleOptions particleOptions, ServerLevel level, Player player, int count) {
        addParticlesAroundSelfServer(particleOptions, level, player, count, 2);
    }

    public static boolean playerHasTag(CompoundTag persistentData, String tag) {
        return persistentData.contains(SwarmsmpS2.MODID + ":" + tag);
    }

    public static void addParticlesAroundSelfInCuboidServer(ParticleOptions particleOptions, ServerLevel level, Player player, int count, int verticalOffset, int horizontalOffset) {
        for (int i = 0; i < count; ++i) {
            double d0 = level.getRandom().nextGaussian() * 1.2D;
            double d1 = level.getRandom().nextGaussian() * 1.2D;
            double d2 = level.getRandom().nextGaussian() * 1.2D;
            double randomY = level.getRandom().nextInt(verticalOffset) + player.getY();
            level.sendParticles(particleOptions, player.getRandomX(1), randomY, player.getRandomZ(1), 1, d0, d1, d2, 0.0D);
        }
    }
}
