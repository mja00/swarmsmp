package dev.mja00.swarmsmps2.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigHelper {

    static final Logger LOGGER = LogManager.getLogger("SSMP/Config");

    @Nullable
    public static JsonElement getJsonReader(File file) {
        // We want this jsonReader to be auto-closing
        if (createFileAndDirs(file)) return null;
        try (JsonReader jr = new JsonReader(new FileReader(file))) {
            return JsonParser.parseReader(jr);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean createFileAndDirs(File file) {
        try {
            file.getParentFile().mkdirs();
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    LOGGER.error("Failed to create file: {}", file.getAbsolutePath());
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    @Nullable
    public static JsonWriter getJsonWriter(File file) {
        try {
            if (createFileAndDirs(file)) return null;
            JsonWriter jw = new JsonWriter(new FileWriter(file));
            jw.setIndent("  ");
            return jw;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
