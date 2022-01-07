package dev.mja00.swarmsmp;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

public class SSMPS2Config {

    static final Logger LOGGER = SSMPS2.LOGGER;

    public static class Server {
        public final IntValue talkRange;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Chat Settings").push("chat");

            talkRange = builder
                    .comment("The range at which players can talk to each other")
                    .defineInRange("talkRange", 20, 1, Integer.MAX_VALUE);

            builder.pop();
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
    public static void onLoad(final ModConfig.Loading event) {
        LOGGER.info("Config has been loaded: {}", event.getConfig().getFileName());
    }

    @SubscribeEvent
    public static void onReload(final ModConfig.Reloading event) {
        LOGGER.info("Config has been changed: {}", event.getConfig().getFileName());
    }

}
