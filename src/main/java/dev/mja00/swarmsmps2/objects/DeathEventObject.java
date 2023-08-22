package dev.mja00.swarmsmps2.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeathEventObject {
    private final String playerUUID;
    private final BlockPos pos;
    private final long timestamp;
    private List<ItemStack> items;
    static Logger LOGGER = SwarmsmpS2.LOGGER;
    private final int id;

    public DeathEventObject(String playerUUID, BlockPos pos, long timestamp, List<ItemStack> items) {
        this.playerUUID = playerUUID;
        this.pos = pos;
        this.timestamp = timestamp;
        this.items = items;
        this.id = 0;
    }

    public DeathEventObject(String playerUUID, BlockPos pos, long timestamp, String items, int id) {
        this.playerUUID = playerUUID;
        this.pos = pos;
        this.timestamp = timestamp;
        this.setItemsFromJsonString(items);
        this.id = id;
    }

    public String getPlayerUUIDAsString() {
        return this.playerUUID;
    }

    public UUID getPlayerUUID() {
        return UUID.fromString(this.playerUUID);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public List<ItemStack> getItems() {
        return this.items;
    }

    public int getId() {
        return this.id;
    }

    public String getItemsAsJsonString() {
        // We want a json object that looks like this:
        // { "items": [ { "name": "minecraft:air", "count": 1, "nbt": "" } ] }
        // We'll use a string builder to build this
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"items\": [");
        for (ItemStack item : this.items) {
            sb.append("{ \"name\": \"");
            sb.append(item.getItem().getRegistryName().toString());
            sb.append("\", \"count\": ");
            sb.append(item.getCount());
            sb.append(", \"nbt\": \"");
            if (item.getTag() != null)
                // The tags need to be escaped as we're putting them into a json string
                sb.append(item.getTag().toString().replace("\"", "\\\""));
            sb.append("\" },");
        }
        // Remove the last comma
        sb.deleteCharAt(sb.length() - 1);
        sb.append("] }");
        return sb.toString();
    }

    public void setItemsFromJsonString(String json) {
        // This converts our previously generated string back into a List of ItemStacks
        // We'll want to clear the current items list
        if (this.items != null) {
            this.items.clear();
        } else {
            // If the list is null, create a new one
            this.items = new ArrayList<>();
        }
        // We're given the following json string:
        // { "items": [ { "name": "minecraft:air", "count": 1, "nbt": "" } ] }
        // We'll want to parse this string and create a list of ItemStacks
        JsonObject obj;
        try {
            obj = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            LOGGER.error("Error parsing json string: " + json);
            LOGGER.error(e.getMessage());
            LOGGER.error(e.fillInStackTrace());
            return;
        }

        // Get the items array
        JsonArray items = obj.getAsJsonArray("items");
        // Loop the items
        for (Object item : items) {
            // Make sure our item is a jsonObject itself
            if (!(item instanceof JsonObject itemObj)) {
                LOGGER.error("Error parsing json string: " + json);
                LOGGER.error("Item is not a JsonObject");
                return;
            }
            // Get the item as a JsonObject
            // Get the name of the item, this name is the registry name
            String name = itemObj.get("name").getAsString();
            // Get the count of the item
            int count = itemObj.get("count").getAsInt();
            // Get the nbt of the item, this itself is JSON too but we'll not deal with it
            String nbt = itemObj.get("nbt").getAsString();
            CompoundTag tempTag;
            try {
                tempTag = NbtUtils.snbtToStructure(nbt);
            } catch (CommandSyntaxException e) {
                // We'll just return blank if we can't parse the nbt
                tempTag = new CompoundTag();
            }
            // Create our new ItemStack using this
            Item tempItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
            ItemStack tempStack = new ItemStack(tempItem, count);
            tempStack.setTag(tempTag);
            // Add it to our list
            this.items.add(tempStack);
        }
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

    @Nullable
    public ItemStack getOffhand() {
        // Get the last item in the items list
        ItemStack lastItem = this.items.get(this.items.size() - 1);
        // If it's air, return null
        if (lastItem.getItem().getRegistryName().toString().equals("minecraft:air")) {
            return null;
        }
        // Otherwise return the item
        return lastItem;
    }

    public List<ItemStack> getArmor() {
        // Get the 2nd to last through 5th to last items in the items list
        List<ItemStack> armor = new ArrayList<>();
        for (int i = this.items.size() - 2; i > this.items.size() - 6; i--) {
            ItemStack item = this.items.get(i);
            // Otherwise add it to the list
            armor.add(item);
        }
        return armor;
    }

    public List<ItemStack> getInventory() {
        // Get the first through 27th items in the items list
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            ItemStack item = this.items.get(i);
            // Otherwise add it to the list
            inventory.add(item);
        }
        return inventory;
    }

}
