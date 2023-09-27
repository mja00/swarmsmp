package dev.mja00.swarmsmps2.objects;

import com.google.gson.*;
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

import java.util.ArrayList;
import java.util.List;

public class PlayerObject {
    private final String playerUUID;
    private final BlockPos lastPos;
    private final long timestamp;
    private final float playerHealth;
    private final int playerFood;
    private final float playerSaturation;
    private List<ItemStack> items;
    private List<ItemStack> armor;
    private List<ItemStack> offhand;
    static Logger LOGGER = SwarmsmpS2.LOGGER;

    public PlayerObject(String playerUUID, BlockPos lastPos, long timestamp, int playerFood, float playerHealth, float playerSaturation, List<ItemStack> items, List<ItemStack> armor, List<ItemStack> offhand) {
        this.playerUUID = playerUUID;
        this.lastPos = lastPos;
        this.timestamp = timestamp;
        this.playerFood = playerFood;
        this.playerHealth = playerHealth;
        this.playerSaturation = playerSaturation;
        this.items = items;
        this.armor = armor;
        this.offhand = offhand;
    }

    // Create another constructor that takes an item list as a string
    public PlayerObject(String playerUUID, BlockPos lastPos, long timestamp, int playerFood, float playerHealth, float playerSaturation, String items, String armor, String offhand) {
        this.playerUUID = playerUUID;
        this.lastPos = lastPos;
        this.timestamp = timestamp;
        this.playerFood = playerFood;
        this.playerHealth = playerHealth;
        this.playerSaturation = playerSaturation;
        this.setItemsFromJsonString(items);
        this.setArmorFromJson(armor);
        this.setOffhandFromJson(offhand);
    }

    public String getPlayerUUIDAsString() {
        return this.playerUUID;
    }

    public BlockPos getLastPos() {
        return this.lastPos;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public int getPlayerFood() {
        return this.playerFood;
    }

    public float getPlayerHealth() {
        return this.playerHealth;
    }

    public float getPlayerSaturation() {
        return this.playerSaturation;
    }

    public List<ItemStack> getItems() {
        return this.items;
    }

    public List<ItemStack> getArmor() {
        return this.armor;
    }

    public List<ItemStack> getOffhand() {
        return this.offhand;
    }

    public String getItemsAsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"items\": [");
        for (ItemStack item : this.items) {
            convertItemToJson(sb, item);
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]}");
        return sb.toString();
    }

    public String getArmorAsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"armor\": [");
        for (ItemStack item : this.armor) {
            convertItemToJson(sb, item);
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]}");
        return sb.toString();
    }

    public String getOffhandAsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"offhand\": [");
        for (ItemStack item : this.offhand) {
            convertItemToJson(sb, item);
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]}");
        return sb.toString();
    }

    static void convertItemToJson(StringBuilder sb, ItemStack item) {
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
            ItemStack tempStack = createItemStack(itemObj);
            // Add it to our list
            this.items.add(tempStack);
        }
    }

    public void setArmorFromJson(String json) {
        if (this.armor != null) {
            this.armor.clear();
        } else {
            // If the list is null, create a new one
            this.armor = new ArrayList<>();
        }
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
        JsonArray items = obj.getAsJsonArray("armor");
        // Loop the items
        for (Object item : items) {
            // Make sure our item is a jsonObject itself
            if (!(item instanceof JsonObject itemObj)) {
                LOGGER.error("Error parsing json string: " + json);
                LOGGER.error("Item is not a JsonObject");
                return;
            }
            ItemStack tempStack = createItemStack(itemObj);
            // Add it to our list
            this.armor.add(tempStack);
        }
    }

    public void setOffhandFromJson(String json) {
        if (this.offhand != null) {
            this.offhand.clear();
        } else {
            // If the list is null, create a new one
            this.offhand = new ArrayList<>();
        }
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
        JsonArray items = obj.getAsJsonArray("offhand");
        // Loop the items
        for (Object item : items) {
            // Make sure our item is a jsonObject itself
            if (!(item instanceof JsonObject itemObj)) {
                LOGGER.error("Error parsing json string: " + json);
                LOGGER.error("Item is not a JsonObject");
                return;
            }
            ItemStack tempStack = createItemStack(itemObj);
            // Add it to our list
            this.offhand.add(tempStack);
        }
    }

    private ItemStack createItemStack(JsonObject itemObj) {
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
        return tempStack;
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

    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        JsonObject posObj = new JsonObject();
        posObj.addProperty("x", this.lastPos.getX());
        posObj.addProperty("y", this.lastPos.getY());
        posObj.addProperty("z", this.lastPos.getZ());
        obj.addProperty("playerUUID", this.playerUUID);
        obj.add("lastPos", posObj);
        obj.addProperty("timestamp", this.timestamp);
        obj.addProperty("playerFood", this.playerFood);
        obj.addProperty("playerHealth", this.playerHealth);
        obj.addProperty("playerSaturation", this.playerSaturation);
        obj.addProperty("items", this.getItemsAsJson());
        obj.addProperty("armor", this.getArmorAsJson());
        obj.addProperty("offhand", this.getOffhandAsJson());
        return obj;
    }
}
