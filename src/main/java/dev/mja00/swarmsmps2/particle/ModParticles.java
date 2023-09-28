package dev.mja00.swarmsmps2.particle;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SwarmsmpS2.MODID);

    public static final RegistryObject<SimpleParticleType> TRANS =
            PARTICLE_TYPES.register("flower_trans", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> ROSES =
            PARTICLE_TYPES.register("flower_roses", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> COG =
            PARTICLE_TYPES.register("cog", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> AZALEA_FLOWER =
            PARTICLE_TYPES.register("azalea_flower", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> FLOWER_BEE =
            PARTICLE_TYPES.register("flower_bee", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
