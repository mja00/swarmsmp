package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayerEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;

    @SubscribeEvent
    public static void onChatEvent(ClientChatReceivedEvent event) {
        if (event.getMessage() instanceof TranslatableComponent message) {
            // Get the message arguments
            Object[] args = message.getArgs();
            if (message.getKey().equals("multiplayer.player.joined")) {
                TextComponent messageComponent = (TextComponent) args[0];
                String playerName = messageComponent.getSiblings().get(0).getString();
                LOGGER.info("Player joined: " + playerName);
                event.setCanceled(true);
            } else if (message.getKey().equals("multiplayer.player.left")) {
                TextComponent messageComponent = (TextComponent) args[0];
                String playerName = messageComponent.getSiblings().get(0).getString();
                LOGGER.info("Player left: " + playerName);
                event.setCanceled(true);
            }
        }
    }
}
