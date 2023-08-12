package dev.mja00.swarmsmps2.config;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class HandleServerData {

    public static final Logger LOGGER = LogManager.getLogger("SSMPS2");

    private static net.minecraft.client.multiplayer.ServerData convertToServerData(ServerDataMod serverDataMod) {
        ServerData serverData = new ServerData(serverDataMod.getName(), serverDataMod.getIp(), false);
        serverData.setResourcePackStatus(serverDataMod.getResourcePolicy());
        LOGGER.info("Adding server: " + serverDataMod.getName() + " with IP: " + serverDataMod.getIp() + " and resource pack policy: " + serverDataMod.getResourcePolicy());
        return serverData;
    }

    public static void saveServerData() {
        net.minecraft.client.multiplayer.ServerList serverList = new net.minecraft.client.multiplayer.ServerList(net.minecraft.client.Minecraft.getInstance());
        List<ServerDataMod> ourServers = Arrays.asList(
                new ServerDataMod("Play", "play.swarmsmp.com", "prompt", true),
                new ServerDataMod("Fallback", "fallback.swarmsmp.com", "prompt", true),
                new ServerDataMod("Live", "live.swarmsmp.com", "prompt", true)
        );
        // We create 3 servers here


        // Check if a servers.dat file already exists
        if (!Files.exists(net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("servers.dat"))) {
            LOGGER.debug("No servers.dat file found, creating one");
            // It doesn't so we'll just add the servers we created above
            for (ServerDataMod serverDataMod : ourServers) {
                serverList.add(convertToServerData(serverDataMod));
            }

        } else {
            // We've already got a file so we need to do some logic
            // Load our file first
            serverList.load();
            ourServers.forEach((server) -> {
                if (server.isForced()) {
                    for (int i = 0; i < serverList.size(); i++) {
                        net.minecraft.client.multiplayer.ServerData existingServer = serverList.get(i);
                        if (existingServer.ip.equals(server.getIp())) {
                            // We've found a match so just skip it
                            return;
                        }
                    }
                    // We didn't find a match so add it
                    serverList.add(convertToServerData(server));
                }
            });
        }
        // Write our file
        serverList.save();
    }
}
