package dev.mja00.swarmsmp.events;

import dev.mja00.swarmsmp.SSMPS2;
import dev.mja00.swarmsmp.commands.AdminCommand;
import dev.mja00.swarmsmp.commands.CharmCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;

@Mod.EventBusSubscriber(modid = "swarmsmp-s2")
public class ModEvents {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new CharmCommand(event.getDispatcher());
        new AdminCommand(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerCloneEvent(PlayerEvent.Clone event) {
        if(!event.getOriginal().getEntityWorld().isRemote) {
            event.getOriginal().getPersistentData().keySet().forEach(key -> {
                if(key.contains(SSMPS2.MOD_ID)) {
                    event.getPlayer().getPersistentData().put(key, event.getOriginal().getPersistentData().get(key));
                }
            });
        }
    }

}
