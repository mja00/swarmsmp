package dev.mja00.swarmsmps2.objects;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class BlockEventObject {
    private final String playerUUID;
    private final String blockName;
    private final String event;
    private final int x;
    private final int y;
    private final int z;
    private final long timestamp;
    private final int blockCount;

    public BlockEventObject(String playerUUID, String blockName, String event, int x, int y, int z, long timestamp) {
        this.playerUUID = playerUUID;
        this.blockName = blockName;
        this.event = event;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
        this.blockCount = 1;
    }
    public BlockEventObject(String playerUUID, String blockName, String event, int x, int y, int z, long timestamp, int blockCount) {
        this.playerUUID = playerUUID;
        this.blockName = blockName;
        this.event = event;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
        this.blockCount = blockCount;
    }

    public UUID getPlayerUUID() {
        return UUID.fromString(this.playerUUID);
    }

    public String getBlockName() {
        return this.blockName;
    }

    public MutableComponent getActualBlockName() {
        if (this.event.equals("bucket_use")) {
            return new TextComponent(this.blockName);
        }
        // We want to get the actual name of the block, we have a registry name
        if (this.blockName.equals("unknown")) {
            return new TextComponent("unknown");
        }
        // If there's more than 1 count we're guaranteed to be working with an item
        if (this.blockCount > 1) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(this.blockName));
            if (item == null) {
                return new TextComponent("unknown");
            }
            return new TextComponent(String.valueOf(this.blockCount)).append(" ").append(new ItemStack(item).getHoverName());
        } else {
            // We'll have to do some logic
            // The BLOCKS registry returns air if the object isn't there, so we'll do a simple comparison
            Block air = Blocks.AIR;
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(this.blockName));
            if (block == null) {
                return new TextComponent("unknown");
            }
            if (block.getRegistryName().getPath().equals(air.getRegistryName().getPath())) {
                // Then we're an item
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(this.blockName));
                if (item == null) {
                    return new TextComponent("unknown");
                }
                return new TextComponent(String.valueOf(this.blockCount)).append(" ").append(new ItemStack(item).getHoverName());
            } else {
                return block.getName();
            }
        }
    }

    public String getEvent() {
        return this.event;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public int getBlockCount() {
        return this.blockCount;
    }

    public String getEventPretty() {
        return switch (this.event) {
            case "block_break" -> "broke";
            case "block_place" -> "placed";
            case "farmland_trample" -> "trampled";
            case "bucket_use" -> "used";
            case "block_right_click" -> "opened";
            case "item_add" -> "added";
            case "item_remove" -> "removed";
            default -> "did something with";
        };
    }

    public ChatFormatting getEventColor() {
        return switch (this.event) {
            case "block_break", "farmland_trampled" -> ChatFormatting.RED;
            case "block_place" -> ChatFormatting.GREEN;
            case "bucket_use" -> ChatFormatting.DARK_AQUA;
            case "farmland_trample" -> ChatFormatting.YELLOW;
            case "block_right_click" -> ChatFormatting.LIGHT_PURPLE;
            case "item_add" -> ChatFormatting.DARK_GREEN;
            case "item_remove" -> ChatFormatting.DARK_RED;
            default -> ChatFormatting.WHITE;
        };
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
