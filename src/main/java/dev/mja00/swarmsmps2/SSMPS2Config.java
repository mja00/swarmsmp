package dev.mja00.swarmsmps2;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

public class SSMPS2Config {

    static final Logger LOGGER = SwarmsmpS2.LOGGER;

    public static class Server {
        public final ForgeConfigSpec.IntValue talkRange;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Chat Settings").push("chat");

            talkRange = builder
                    .comment("The range at which players can talk to each other")
                    .defineInRange("talkRange", 20, 1, 100);

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
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        LOGGER.debug("Config has been loaded: {}", configEvent.getConfig().getFileName());
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        LOGGER.debug("Config has been reloaded: {}", configEvent.getConfig().getFileName());
    }
}
