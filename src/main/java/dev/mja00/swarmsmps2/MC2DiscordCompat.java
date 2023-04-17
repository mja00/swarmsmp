package dev.mja00.swarmsmps2;

import ml.denisd3d.mc2discord.core.Mc2Discord;
import ml.denisd3d.mc2discord.core.entities.Entity;
import ml.denisd3d.mc2discord.core.entities.Player;

import java.util.Collections;

public class MC2DiscordCompat {
    public static void sendOOCMessage(String message, Player player) {
        Mc2Discord.INSTANCE.messageManager.sendMessageOfType("swarmsmp-ooc",
                message,
                "",
                player.displayName,
                Entity.replace(Mc2Discord.INSTANCE.config.style.avatar_api, Collections.singletonList(player)),
                null,
                false
        );
    }
}
