package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.item.ModItems;
import dev.mja00.swarmsmps2.item.custom.FlowerCrownItem;
import dev.mja00.swarmsmps2.item.custom.MailItem;
import dev.mja00.swarmsmps2.particle.ModParticles;
import dev.mja00.swarmsmps2.particle.custom.LeafParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventClientBusEvents {

    private static final Logger LOGGER = LogManager.getLogger("SSMP/ClientEvents");

    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Client setup");
        registerItemProperty(ModItems.FLOWER_CROWN.get(), new ResourceLocation(SwarmsmpS2.MODID, "supporter"),
                (stack, world, entity, s) -> FlowerCrownItem.getItemTextureIndex(stack));

        ItemProperties.register(ModItems.MAIL.get(), new ResourceLocation(SwarmsmpS2.MODID, "mail"),
                (stack, world, entity, s) -> MailItem.getItemTextureIndex(stack));
    }

    public static void registerItemProperty(Item item, ResourceLocation name, ClampedItemPropertyFunction property) {
        ItemProperties.register(item, name, property);
    }

    @SubscribeEvent
    public static void registerParticleFactories(final ParticleFactoryRegisterEvent event) {
        Minecraft.getInstance().particleEngine.register(ModParticles.AZALEA_FLOWER.get(), LeafParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.FLOWER_BEE.get(), LeafParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.COG.get(), LeafParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.ROSES.get(), LeafParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.TRANS.get(), LeafParticle.Provider::new);
    }
}
