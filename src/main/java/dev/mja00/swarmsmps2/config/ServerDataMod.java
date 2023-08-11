package dev.mja00.swarmsmps2.config;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ServerDataMod {
    private final String name;
    private final String ip;
    private final String resources;
    private final boolean forced;

    public ServerDataMod(String name, String ip, String resources, boolean forced) {
        this.name = name;
        this.ip = ip;
        this.resources = resources;
        this.forced = forced;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public ServerData.ServerPackStatus getResourcePolicy() {
        if(resources != null) {
            switch (resources) {
                case "enabled" -> {
                    return ServerData.ServerPackStatus.ENABLED;
                }
                case "disabled" -> {
                    return ServerData.ServerPackStatus.DISABLED;
                }
                case "prompt" -> {
                    return ServerData.ServerPackStatus.PROMPT;
                }
            }
        }

        return ServerData.ServerPackStatus.PROMPT;
    }

    public boolean isForced() {
        return forced;
    }
}
