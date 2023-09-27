package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.particle.ModParticles;
import dev.mja00.swarmsmps2.particle.custom.LeafParticle;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void registerParticleFactories(final ParticleFactoryRegisterEvent event) {
        Minecraft.getInstance().particleEngine.register(ModParticles.AZALEA_FLOWER.get(), LeafParticle.SimpleLeafParticle::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.FLOWER_BEE.get(), LeafParticle.SimpleLeafParticle::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.COG.get(), LeafParticle.SimpleLeafParticle::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.ROSES.get(), LeafParticle.SimpleLeafParticle::new);
        Minecraft.getInstance().particleEngine.register(ModParticles.TRANS.get(), LeafParticle.SimpleLeafParticle::new);
    }
}
