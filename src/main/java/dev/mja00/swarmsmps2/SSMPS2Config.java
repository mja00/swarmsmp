package dev.mja00.swarmsmps2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SSMPS2Config {

    static final Logger LOGGER = SwarmsmpS2.LOGGER;
    // This part is for startup time logging
    public static final File DOT_MINECRAFT = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).toFile().getParentFile();
    public static final File TIMES_FILE = new File(DOT_MINECRAFT, "config/ssmps2/startup_times.json");

    public static class Client {
        // Client config options
        public final ForgeConfigSpec.BooleanValue timerOnTop;
        public final ForgeConfigSpec.IntValue fadeOutTime;
        public final ForgeConfigSpec.IntValue fadeInTime;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Startup Settings").push("startup");

            timerOnTop = builder
                    .comment("Should the timer be on top of the screen?")
                    .define("timerOnTop", true);

            fadeOutTime = builder
                    .comment("How long should the timer take to fade out? (in ticks)")
                    .defineInRange("fadeOutTime", 1000, 0, 10000);

            fadeInTime = builder
                    .comment("How long should the timer take to fade in? (in ticks)")
                    .defineInRange("fadeInTime", 500, 0, 10000);
        }
    }

    public static class Server {
        // Communication settings
        public final ForgeConfigSpec.IntValue talkRange;
        public final ForgeConfigSpec.IntValue whisperSpeed;
        public final ForgeConfigSpec.BooleanValue whisperEnabled;
        // Memory loss settings
        public final ForgeConfigSpec.IntValue memoryLossTime;
        public final ForgeConfigSpec.IntValue memoryLossTimeMultiplier;
        public final ForgeConfigSpec.IntValue memoryLossAmplifier;
        // API Settings
        public final ForgeConfigSpec.BooleanValue enableAPI;
        public final ForgeConfigSpec.ConfigValue<String> apiKey;
        public final ForgeConfigSpec.ConfigValue<String> apiBaseURL;
        public final ForgeConfigSpec.IntValue firstTimeout;
        public final ForgeConfigSpec.IntValue secondTimeout;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> bypassedPlayers;

        // Duel Settings
        public final ForgeConfigSpec.BooleanValue enableDuels;
        public final ForgeConfigSpec.BooleanValue allowDuelingPlayerToAttackNonDueling;

        // Join command settings
        public final ForgeConfigSpec.BooleanValue enableJoinCommand;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> swarmCommands;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> constructCommands;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> undeadCommands;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> naturebornCommands;

        // NamelessMC API Settings
        public final ForgeConfigSpec.BooleanValue enableVerifyCommand;
        public final ForgeConfigSpec.ConfigValue<String> verificationFAQURL;

        // Faction spawnpoints
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> swarmSpawnpoint;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> constructSpawnpoint;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> undeadSpawnpoint;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> naturebornSpawnpoint;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> defaultSpawnpoint;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> debug1Spawnpoint;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> debug2Spawnpoint;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> debug3Spawnpoint;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> debug4Spawnpoint;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> debug5Spawnpoint;
        public final ForgeConfigSpec.BooleanValue enableSpawnpoints;


        public final ForgeConfigSpec.ConfigValue<List<? extends String>> ignoredCommands;
        public final ForgeConfigSpec.BooleanValue fallbackServer;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Chat Settings").push("chat");

            talkRange = builder
                    .comment("The range at which players can talk to each other")
                    .defineInRange("talkRange", 20, 1, 100);

            whisperSpeed = builder
                    .comment("The speed at which whispers travel. ms per block")
                    .defineInRange("whisperSpeed", 100, 10, 1000);

            whisperEnabled = builder
                    .comment("Enable whispers")
                    .define("whisperEnabled", false);

            builder.pop();
            builder.comment("Memory Loss Settings").push("memoryLoss");

            memoryLossTime = builder
                    .comment("The time in minutes that a player's memory is lost")
                    .defineInRange("memoryLossTime", 5, 1, 60);

            memoryLossTimeMultiplier = builder
                    .comment("The multiplier for the memory loss time. Affects potion effect time. Default makes 10s of potion.")
                    .defineInRange("memoryLossTimeMultiplier", 2, 1, 100);

            memoryLossAmplifier = builder
                    .comment("The amplifier for the memory loss effect. Affects potion effect strength")
                    .defineInRange("memoryLossAmplifier", 2, 0, 100);

            builder.pop();
            builder.comment("API Settings").push("api");

            enableAPI = builder
                    .comment("Enable the API. Disabling the API will turn off the whitelist check.")
                    .define("enableAPI", true);

            apiKey = builder
                    .comment("The API key")
                    .define("apiKey", "YOUR_API_KEY");

            apiBaseURL = builder
                    .comment("The base URL for the API")
                    .define("apiBaseURL", "http://localhost:5000/api/");

            firstTimeout = builder
                    .comment("The first timeout for the API")
                    .defineInRange("firstTimeout", 3, 1, 10);

            secondTimeout = builder
                    .comment("The second timeout for the API")
                    .defineInRange("secondTimeout", 30, 1, 30);

            bypassedPlayers = builder
                    .comment("A list of player UUIDs that bypass the whitelist check entirely.")
                    .defineList("bypassedPlayers", List.of(), o -> o instanceof String);

            builder.pop();
            builder.comment("Duel Settings").push("duel");

            enableDuels = builder
                    .comment("Enable duels")
                    .define("enableDuels", true);

            allowDuelingPlayerToAttackNonDueling = builder
                    .comment("Allow dueling player to attack non-dueling player")
                    .define("allowDuelingPlayerToAttackNonDueling", false);

            builder.pop();
            builder.comment("Join Settings").push("join");

            enableJoinCommand = builder
                    .comment("Enable the join command")
                    .define("enableJoinCommand", true);

            swarmCommands = builder
                    .comment("Commands to run when a new Swarm character joins for the first time")
                    .defineList("swarmCommands", List.of("/say swarm"), o -> o instanceof String);

            constructCommands = builder
                    .comment("Commands to run when a new Construct character joins for the first time")
                    .defineList("constructCommands", List.of("/say construct"), o -> o instanceof String);

            undeadCommands = builder
                    .comment("Commands to run when a new Undead character joins for the first time")
                    .defineList("undeadCommands", List.of("/say undead"), o -> o instanceof String);

            naturebornCommands = builder
                    .comment("Commands to run when a new Natureborn character joins for the first time")
                    .defineList("naturebornCommands", List.of("/say natureborn"), o -> o instanceof String);

            builder.pop();
            builder.comment("Other Settings").push("other");

            ignoredCommands = builder
                    .comment("Commands that will not be sent to the API")
                    .defineList("ignoredCommands", List.of("/help", "/gamemode", "/ooc"), o -> o instanceof String);

            fallbackServer = builder
                    .comment("Whether or not this server is a fallback server")
                    .define("fallbackServer", false);

            builder.pop();
            builder.comment("Verification Settings").push("verification");

            enableVerifyCommand = builder
                    .comment("Enable the verify command")
                    .define("enableVerifyCommand", true);

            verificationFAQURL = builder
                    .comment("The URL for the verification FAQ")
                    .define("verificationFAQURL", "https://swarmsmp.com/forum/topic/8-how-to-verify-your-minecraft-account/");

            builder.pop();
            builder.comment("Faction Spawnpoints").push("spawnpoints");

            enableSpawnpoints = builder
                    .comment("Enable faction spawnpoints")
                    .define("enableSpawnpoints", true);

            swarmSpawnpoint = builder
                    .comment("The Swarm's spawnpoint. Format: [x, y, z]")
                    .defineList("swarmSpawnpoint", List.of(0, 0, 0), o -> o instanceof Integer);

            constructSpawnpoint = builder
                    .comment("The Construct's spawnpoint. Format: [x, y, z]")
                    .defineList("constructSpawnpoint", List.of(0, 0, 0), o -> o instanceof Integer);

            undeadSpawnpoint = builder
                    .comment("The Undead's spawnpoint. Format: [x, y, z]")
                    .defineList("undeadSpawnpoint", List.of(0, 0, 0), o -> o instanceof Integer);

            naturebornSpawnpoint = builder
                    .comment("The Natureborn's spawnpoint. Format: [x, y, z]")
                    .defineList("naturebornSpawnpoint", List.of(0, 0, 0), o -> o instanceof Integer);

            debug1Spawnpoint = builder
                    .comment("The debug1's spawnpoint. Format: [x, y, z]")
                    .defineList("debug1Spawnpoint", List.of(0, 0, 0), o -> o instanceof Integer);

            debug2Spawnpoint = builder
                    .comment("The debug2's spawnpoint. Format: [x, y, z]")
                    .defineList("debug2Spawnpoint", List.of(0, 0, 0), o -> o instanceof Integer);

            debug3Spawnpoint = builder
                    .comment("The debug3's spawnpoint. Format: [x, y, z]")
                    .defineList("debug3Spawnpoint", List.of(0, 0, 0), o -> o instanceof Integer);

            debug4Spawnpoint = builder
                    .comment("The debug4's spawnpoint. Format: [x, y, z]")
                    .defineList("debug4Spawnpoint", List.of(0, 0, 0), o -> o instanceof Integer);

            debug5Spawnpoint = builder
                    .comment("The debug5's spawnpoint. Format: [x, y, z]")
                    .defineList("debug5Spawnpoint", List.of(0, 0, 0), o -> o instanceof Integer);

            defaultSpawnpoint = builder
                    .comment("The default spawnpoint. Format: [x, y, z]")
                    .defineList("defaultSpawnpoint", List.of(0, 0, 0), o -> o instanceof Integer);

            builder.pop();
        }
    }

    public static final ForgeConfigSpec serverSpec;
    public static final ForgeConfigSpec clientSpec;
    public static final SSMPS2Config.Server SERVER;
    public static final SSMPS2Config.Client CLIENT;

    static {
        final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();

        // Client
        final Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(Client::new);
        clientSpec = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        LOGGER.debug("Config has been loaded: {}", configEvent.getConfig().getFileName());
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        LOGGER.debug("Config has been reloaded: {}", configEvent.getConfig().getFileName());
    }

    public static List<? extends Integer> getSpawnpointForFaction(String faction) {
        return switch (faction) {
            case "swarm" -> SSMPS2Config.SERVER.swarmSpawnpoint.get();
            case "undead" -> SSMPS2Config.SERVER.undeadSpawnpoint.get();
            case "construct" -> SSMPS2Config.SERVER.constructSpawnpoint.get();
            case "natureborn" -> SSMPS2Config.SERVER.naturebornSpawnpoint.get();
            case "debug1" -> SSMPS2Config.SERVER.debug1Spawnpoint.get();
            case "debug2" -> SSMPS2Config.SERVER.debug2Spawnpoint.get();
            case "debug3" -> SSMPS2Config.SERVER.debug3Spawnpoint.get();
            case "debug4" -> SSMPS2Config.SERVER.debug4Spawnpoint.get();
            case "debug5" -> SSMPS2Config.SERVER.debug5Spawnpoint.get();
            default -> SSMPS2Config.SERVER.defaultSpawnpoint.get();
        };
    }

    public static long getTimeEstimates() {
        try {
            TIMES_FILE.getParentFile().mkdirs();
            if (!TIMES_FILE.exists()) {
                TIMES_FILE.createNewFile();
            }

            JsonReader jr = new JsonReader(new FileReader(TIMES_FILE));
            JsonElement jp = JsonParser.parseReader(jr);
            if (jp.isJsonObject()) {
                JsonObject jo = jp.getAsJsonObject();
                if (jo.has("times") && jo.get("times").isJsonArray()) {
                    JsonArray ja = jo.get("times").getAsJsonArray();
                    if (ja.size() > 0) {
                        long sum = 0;
                        for (int i = 0; i < ja.size(); i++) {
                            sum += ja.get(i).getAsLong();
                        }
                        sum /= ja.size();

                        return sum;
                    }
                }
            }
            jr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static void addStartupTime(long startupTime) {
        try {
            TIMES_FILE.getParentFile().mkdirs();
            if (!TIMES_FILE.exists()) {
                TIMES_FILE.createNewFile();
            }

            long[] times = new long[0];
            JsonReader jr = new JsonReader(new FileReader(TIMES_FILE));
            JsonElement jp = JsonParser.parseReader(jr);

            if (jp.isJsonObject()) {
                JsonObject jo = jp.getAsJsonObject();
                if (jo.has("times") && jo.get("times").isJsonArray()) {
                    JsonArray ja = jo.get("times").getAsJsonArray();
                    times = new long[ja.size()];
                    for (int i = 0; i < ja.size(); i++) {
                        times[i] = ja.get(i).getAsLong();
                    }
                }
            }

            jr.close();

            // Write the times
            JsonWriter jw = new JsonWriter(new FileWriter(TIMES_FILE));
            jw.setIndent("  ");
            jw.beginObject();

            jw.name("times");
            jw.beginArray();
            // Only keep 3 times
            if (times.length > 2) {
                for (int i = times.length - 2; i < times.length; i++) {
                    jw.value(times[i]);
                }
            } else {
                for (long time : times) {
                    jw.value(time);
                }
            }
            jw.value(startupTime);
            jw.endArray();

            jw.endObject();
            jw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
