package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.commands.AdminCommand;
import net.minecraft.Util;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID)
public class ModEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new AdminCommand(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerCloneEvent(PlayerEvent.Clone event) {
        if (!event.getOriginal().getLevel().isClientSide) {
            event.getOriginal().getPersistentData().getAllKeys().forEach(key -> {
               if(key.contains(SwarmsmpS2.MODID)) {
                   event.getPlayer().getPersistentData().put(key, Objects.requireNonNull(event.getOriginal().getPersistentData().get(key)));
               }
            });
        }
    }
}
