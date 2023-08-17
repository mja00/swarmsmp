package dev.mja00.swarmsmps2.objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class MobKillObject {
    public final String mobName;
    public final String playerUUID;
    private final long timestamp;

    public MobKillObject(String mobName, String playerUUID, long timestamp) {
        this.mobName = mobName;
        this.playerUUID = playerUUID;
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getMobName() {
        return this.mobName;
    }

    public UUID getPlayerUUID() {
        return UUID.fromString(this.playerUUID);
    }

    public String getActualMobName() {
        EntityType<?> mob = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(this.mobName));
        if (mob == null) {
            return "Unknown";
        }
        return mob.getDescription().getString();
    }

    public String humanizeTimestamp() {
        // We want the string returned to be like "1 day ago" or "2 hours ago"
        // Get the current time in seconds
        int currentUnix = (int) (System.currentTimeMillis() / 1000L);
        int loggedUnix = (int) (this.timestamp / 1000L);
        // Get the difference between the two
        int difference = currentUnix - loggedUnix;
        // Now we'll want to convert the difference into a human readable string
        // We'll want to check if it's less than a minute
        if (difference < 60) {
            return "less than a minute ago";
        } else if (difference < 3600) {
            // It's less than an hour
            int minutes = difference / 60;
            return minutes + " minutes ago";
        } else if (difference < 86400) {
            // It's less than a day
            int hours = difference / 3600;
            return hours + " hours ago";
        } else {
            // It's more than a day
            int days = difference / 86400;
            return days + " days ago";
        }
    }
}
