package dev.mja00.swarmsmps2.sounds;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSoundEvent {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SwarmsmpS2.MODID);

    public static final RegistryObject<SoundEvent> LINK_START =
            registerSoundEvent("link_start");

    public static final RegistryObject<SoundEvent> PARTY_HORN =
            registerSoundEvent("party_horn");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> new SoundEvent(new ResourceLocation(SwarmsmpS2.MODID, name)));
    }

    public static void register(IEventBus eventBus){
        SOUND_EVENTS.register(eventBus);
    }
}
