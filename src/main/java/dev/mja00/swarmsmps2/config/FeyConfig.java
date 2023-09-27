package dev.mja00.swarmsmps2.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import dev.mja00.swarmsmps2.objects.PlayerObject;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class FeyConfig {

    public static final File DOT_MINECRAFT = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).toFile().getParentFile();
    public static final File FEY_FILE  = new File(DOT_MINECRAFT, "config/ssmps2/fey.json");
    static final Logger LOGGER = LogManager.getLogger("SSMP/FeyConfig");

    // Initialize the config file
    public static void init() throws IOException {
        // Get a reader for the file
        JsonElement reader = ConfigHelper.getJsonReader(FEY_FILE);
        if (reader == null) throw new IOException("Failed to create reader for fey file");
        // If it's an empty file we'll want to write a new one
        if (reader.isJsonNull()) {
            LOGGER.warn("Fey file is empty, writing new one");
            JsonWriter writer = ConfigHelper.getJsonWriter(FEY_FILE);
            // We'll just write a blank {"feys": []} for now
            if (writer == null) throw new IOException("Failed to create writer for fey file");
            writer.beginObject();
            writer.name("feys");
            writer.beginArray();
            writer.endArray();
            writer.endObject();
            writer.close();
        }
        // Otherwise we're good now :)
    }

    // We want a method for writing a new fey to the main file
    public static void writeNewFey(PlayerObject fey) throws Exception {
        // We're given the entire fey object, but we gotta do some fanangling to get it into the file
        JsonElement reader = ConfigHelper.getJsonReader(FEY_FILE);
        if (reader == null) throw new IOException("Failed to create reader for fey file");
        // We'll be appending to the file but we wanna do sanity checks first
        if (!reader.isJsonObject()) throw new Exception("Fey file is not a json object");
        // We'll get the array of feys
        JsonElement feys = reader.getAsJsonObject().get("feys");
        if (!feys.isJsonArray()) throw new Exception("Fey file does not contain a feys array");
        JsonArray feysArray = feys.getAsJsonArray();
        // Now we'll add the new fey to the array
        feysArray.add(fey.toJson());
        // Now we'll write the new array to the file
        JsonWriter writer = ConfigHelper.getJsonWriter(FEY_FILE);
        if (writer == null) throw new IOException("Failed to create writer for fey file");
        writeFeyArrayToFile(feysArray, writer);
    }

    public static PlayerObject getFeyFromPlayerUUID(String playerUUID) {
        JsonElement reader = ConfigHelper.getJsonReader(FEY_FILE);
        if (reader == null) return null;
        if (!reader.isJsonObject()) return null;
        JsonElement feys = reader.getAsJsonObject().get("feys");
        if (!feys.isJsonArray()) return null;
        JsonArray feysArray = feys.getAsJsonArray();
        for (JsonElement feyElement : feysArray) {
            // We'll need to get all of our values from the object
            // Get the object and verify it's an object
            if (!feyElement.isJsonObject()) return null;
            String uuid = feyElement.getAsJsonObject().get("playerUUID").getAsString();
            long timestamp = feyElement.getAsJsonObject().get("timestamp").getAsLong();
            int playerFood = feyElement.getAsJsonObject().get("playerFood").getAsInt();
            float playerSaturation = feyElement.getAsJsonObject().get("playerSaturation").getAsFloat();
            float playerHealth = feyElement.getAsJsonObject().get("playerHealth").getAsFloat();
            String items = feyElement.getAsJsonObject().get("items").getAsString();
            String armor = feyElement.getAsJsonObject().get("armor").getAsString();
            String offhand = feyElement.getAsJsonObject().get("offhand").getAsString();
            // Our lastPos is an object with x, y, and z
            JsonElement lastPos = feyElement.getAsJsonObject().get("lastPos");
            BlockPos pos = new BlockPos(lastPos.getAsJsonObject().get("x").getAsInt(), lastPos.getAsJsonObject().get("y").getAsInt(), lastPos.getAsJsonObject().get("z").getAsInt());
            PlayerObject fey = new PlayerObject(uuid, pos, timestamp, playerFood, playerHealth, playerSaturation, items, armor, offhand);
            if (fey.getPlayerUUIDAsString().equals(playerUUID)) return fey;
        }
        return null;
    }

    public static boolean doesFeyExist(String playerUUID) {
        return getFeyFromPlayerUUID(playerUUID) != null;
    }

    public static void removeFey(String playerUUID) throws Exception{
        JsonElement reader = ConfigHelper.getJsonReader(FEY_FILE);
        if (reader == null) return;
        if (!reader.isJsonObject()) return;
        JsonElement feys = reader.getAsJsonObject().get("feys");
        if (!feys.isJsonArray()) return;
        JsonArray feysArray = feys.getAsJsonArray();
        for (JsonElement feyElement : feysArray) {
            // We'll need to get all of our values from the object
            // Get the object and verify it's an object
            if (!feyElement.isJsonObject()) return;
            String uuid = feyElement.getAsJsonObject().get("playerUUID").getAsString();
            if (uuid.equals(playerUUID)) {
                feysArray.remove(feyElement);
                break;
            }
        }
        // Now we'll write the new array to the file
        JsonWriter writer = ConfigHelper.getJsonWriter(FEY_FILE);
        if (writer == null) throw new IOException("Failed to create writer for fey file");
        writeFeyArrayToFile(feysArray, writer);
    }

    private static void writeFeyArrayToFile(JsonArray feysArray, JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("feys");
        writer.beginArray();
        for (JsonElement feyElement : feysArray) {
            writer.jsonValue(feyElement.toString());
        }
        writer.endArray();
        writer.endObject();
        writer.close();
    }
}
