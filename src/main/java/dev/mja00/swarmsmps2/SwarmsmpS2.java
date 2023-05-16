package dev.mja00.swarmsmps2;

import com.google.gson.JsonObject;
import dev.mja00.swarmsmps2.events.PlayerEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("swarmsmps2")
public class SwarmsmpS2 {

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger("SSMPS2");
    static final Random rnd = new Random();

    // Mod ID
    public static final String MODID = "swarmsmps2";
    public static final String translationKey = MODID + ".";

    static {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            PlayerEvents.expectedTime = SSMPS2Config.getTimeEstimates();
        });
    }

    public SwarmsmpS2() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SSMPS2Config.serverSpec);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            PlayerEvents.trueFullscreen = Minecraft.getInstance().options.fullscreen;
            Minecraft.getInstance().options.fullscreen = false;
        });
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SSMPS2Config.clientSpec);
        eventBus.register(SSMPS2Config.class);

        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Check the mod list for other mods
        ModList modList = ModList.get();
        // Create a list of banned mods
        String[] bannedMods = {"xaerominimapfair", "xaerominimap", "xaeroworldmap", "journeymap"};
        // Check if any of those mods are installed
        for (String mod : bannedMods) {
            if (modList.isLoaded(mod)) {
                //LOGGER.info("Banned mod detected: " + mod);
                // Crash the client with a vague but obvious error message
                // Pick a random block from the registry
                List<Item> items = ForgeRegistries.ITEMS.getValues().stream().toList();
                Item item = items.get(rnd.nextInt(items.size()));
                throw new RuntimeException("error while calculating the delta offsets for block " + item.getRegistryName());
            }
        }

        String[] cheatMods = {"cheatutils", "atianxray", "entity_xray", "xray"};
        for (String mod : cheatMods) {
            if (modList.isLoaded(mod)) {
                // Create a json object containing some information
                JsonObject json = new JsonObject();
                json.addProperty("mod", mod);
                json.addProperty("current_time", System.currentTimeMillis());
                // Encode the json object as a base64 string
                String base64 = new String(java.util.Base64.getEncoder().encode(json.toString().getBytes()));
                throw new RuntimeException("error while stitching atlas for textures at " + base64);
            }
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code
        LOGGER.info("We're preiniting");
        LOGGER.info("If you see this post <@124956694768779264> in the ssmp-help-desk channel");
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        // some example code to dispatch IMC to another mod
        InterModComms.sendTo("swarmsmp", "fuckyou", () -> {
            LOGGER.info("Shit talking Scion's mod");
            return "ur literally garbage";
        });
    }

    private void processIMC(final InterModProcessEvent event) {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m -> m.messageSupplier().get()).
                collect(Collectors.toList()));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("Literally only mja00 will see this");
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // register a new block here
            LOGGER.info("HELLO from Register Block");
        }
    }


}
