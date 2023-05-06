package dev.mja00.swarmsmps2;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class SSMPS2Config {

    static final Logger LOGGER = SwarmsmpS2.LOGGER;

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


        public final ForgeConfigSpec.ConfigValue<List<? extends String>> ignoredCommands;

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
                    .comment("The multiplier for the memory loss time. Affects potion effect time")
                    .defineInRange("memoryLossTimeMultiplier", 2, 1, 100);

            memoryLossAmplifier = builder
                    .comment("The amplifier for the memory loss effect. Affects potion effect strength")
                    .defineInRange("memoryLossAmplifier", 2, 0, 100);

            builder.pop();
            builder.comment("API Settings").push("api");

            enableAPI = builder
                    .comment("Enable the API")
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

            builder.pop();
            builder.comment("Verification Settings").push("verification");

            enableVerifyCommand = builder
                    .comment("Enable the verify command")
                    .define("enableVerifyCommand", true);

            verificationFAQURL = builder
                    .comment("The URL for the verification FAQ")
                    .define("verificationFAQURL", "https://swarmsmp.com/forum/topic/8-how-to-verify-your-minecraft-account/");
        }
    }

    public static final ForgeConfigSpec serverSpec;
    public static final SSMPS2Config.Server SERVER;

    static {
        final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        LOGGER.debug("Config has been loaded: {}", configEvent.getConfig().getFileName());
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        LOGGER.debug("Config has been reloaded: {}", configEvent.getConfig().getFileName());
    }
}
