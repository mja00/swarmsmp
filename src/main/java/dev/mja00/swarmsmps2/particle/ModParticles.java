package dev.mja00.swarmsmps2.particle;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SwarmsmpS2.MODID);

    public static final RegistryObject<SimpleParticleType> AZALEA_FLOWER =
            PARTICLES.register("azalea_flower", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> FLOWER_BEE =
            PARTICLES.register("flower_bee", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> COG =
            PARTICLES.register("cog", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> ROSES =
            PARTICLES.register("flower_roses", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> TRANS =
            PARTICLES.register("flower_trans", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
