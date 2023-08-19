package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.network.SwarmSMPPacketHandler;
import dev.mja00.swarmsmps2.network.packets.ModListPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID, value = Dist.CLIENT)
public class ClientEvents {

    public static boolean connected;
    public static final Logger LOGGER = LogManager.getLogger("SSMPS2/ClientEvents");

    @SubscribeEvent
    public static void onPlayerConnect(ClientPlayerNetworkEvent.LoggedInEvent event) {
        // Send mod list to server
        LOGGER.debug("Connected to server");
        ModList modList = ModList.get();
        String[] modNames = modList.getMods().stream().map(IModInfo::getModId).toArray(String[]::new);
        ModListPacket packet = new ModListPacket(modNames);
        SwarmSMPPacketHandler.MOD_LIST_CHANNEL.sendToServer(packet);
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        LOGGER.debug("Disconnected from server");
        connected = false;
    }
}
