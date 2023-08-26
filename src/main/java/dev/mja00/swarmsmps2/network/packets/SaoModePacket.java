package dev.mja00.swarmsmps2.network.packets;

import dev.mja00.swarmsmps2.SSMPS2Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

// This packet will literally just be used to toggle a client side boolean
// This will be used to toggle the SAO mode
public class SaoModePacket {
    private final boolean saoMode;
    public static final Logger LOGGER = LogManager.getLogger("SSMPS2/SaoModePacket");

    public SaoModePacket(boolean saoMode) {
        this.saoMode = saoMode;
    }

    public static void encode(SaoModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.saoMode);
    }

    public static SaoModePacket decode(FriendlyByteBuf buffer) {
        return new SaoModePacket(buffer.readBoolean());
    }

    public static void handle(SaoModePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                LOGGER.info("SAO mode is now " + packet.saoMode);
                SSMPS2Config.CLIENT.saoMode.set(packet.saoMode);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
