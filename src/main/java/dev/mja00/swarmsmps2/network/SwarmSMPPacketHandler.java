package dev.mja00.swarmsmps2.network;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.network.packets.ModListPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class SwarmSMPPacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel MOD_LIST_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SwarmsmpS2.MODID, "mod_list"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    public static void init() {
        int id = 0;
        MOD_LIST_CHANNEL.registerMessage(id++, ModListPacket.class, ModListPacket::encode, ModListPacket::decode, ModListPacket::handle);
    }
}
