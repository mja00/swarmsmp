package dev.mja00.swarmsmps2.network;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.network.packets.ModListPacket;
import dev.mja00.swarmsmps2.network.packets.SaoModePacket;
import dev.mja00.swarmsmps2.network.packets.SendingStonePacket;
import dev.mja00.swarmsmps2.network.packets.ToastPacket;
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

    public static final SimpleChannel SAO_MODE_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SwarmsmpS2.MODID, "sao_mode"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    public static final SimpleChannel SENDING_STONE_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SwarmsmpS2.MODID, "sending_stone"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static final SimpleChannel TOAST_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SwarmsmpS2.MODID, "toast"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        int id = 0;
        MOD_LIST_CHANNEL.registerMessage(id++, ModListPacket.class, ModListPacket::encode, ModListPacket::decode, ModListPacket::handle);
        SAO_MODE_CHANNEL.registerMessage(id++, SaoModePacket.class, SaoModePacket::encode, SaoModePacket::decode, SaoModePacket::handle);
        SENDING_STONE_CHANNEL.registerMessage(id++, SendingStonePacket.class, SendingStonePacket::encode, SendingStonePacket::decode, SendingStonePacket::handle);
        TOAST_CHANNEL.registerMessage(id++, ToastPacket.class, ToastPacket::encode, ToastPacket::decode, ToastPacket::handle);
    }
}
