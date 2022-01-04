package dev.mja00.swarmsmp.events;

import dev.mja00.swarmsmp.SSMPS2;
import dev.mja00.swarmsmp.commands.AdminCommand;
import dev.mja00.swarmsmp.commands.CharmCommand;
import dev.mja00.swarmsmp.commands.DuelCommand;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;

@Mod.EventBusSubscriber(modid = SSMPS2.MOD_ID)
public class ModEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new CharmCommand(event.getDispatcher());
        new AdminCommand(event.getDispatcher());
        new DuelCommand(event.getDispatcher());

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

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            long now = System.currentTimeMillis();
            long timeTillExpire = 1000L * 60;

            Iterator<DuelCommand.DuelRequest> iterator = DuelCommand.REQUESTS.values().iterator();

            while (iterator.hasNext()) {
                DuelCommand.DuelRequest request = iterator.next();

                if (now > request.created + timeTillExpire) {
                    ServerPlayerEntity source = request.source;
                    ServerPlayerEntity target = request.target;

                    if (source != null) {
                        source.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "duel.timeout", target.getDisplayName()), source.getUniqueID());
                    }

                    if (target != null) {
                        target.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "duel.timeout", source.getDisplayName()), target.getUniqueID());
                    }

                    iterator.remove();
                }
            }
        }
    }
}
