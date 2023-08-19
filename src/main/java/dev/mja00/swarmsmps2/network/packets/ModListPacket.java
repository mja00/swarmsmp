package dev.mja00.swarmsmps2.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

// This is a packet that is sent to the server with a list of mods currently installed on the server
// We'll use this to check if the player is using a modded client
public class ModListPacket {
    // We'll just use a list of mod ids
    private final String[] modIds;
    public static final Logger LOGGER = LogManager.getLogger("SSMPS2/ModListPacket");

    public ModListPacket(String[] modIds) {
        this.modIds = modIds;
    }

    public String[] getModIds() {
        return this.modIds;
    }

    public static void encode(ModListPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.modIds.length);
        for (String modId : packet.modIds) {
            buffer.writeUtf(modId);
        }
    }

    public static ModListPacket decode(FriendlyByteBuf buffer) {
        int length = buffer.readVarInt();
        String[] modIds = new String[length];
        for (int i = 0; i < length; i++) {
            modIds[i] = buffer.readUtf();
        }
        return new ModListPacket(modIds);
    }

    public static void handle(ModListPacket packet, Supplier<NetworkEvent.Context> ctx) {
        // Make a neat string of the mod list
        String modList = String.join(", ", packet.modIds);
        ServerPlayer player = ctx.get().getSender();
        String name = player != null ? player.getName().getString() : "Unknown";
        LOGGER.info("Mod list received from " + name + ": " + modList);
        ctx.get().setPacketHandled(true);
    }
}
