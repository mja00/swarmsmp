package dev.mja00.swarmsmps2.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class ToastPacket {
    private final Component message;
    private final String title;
    public static final Logger LOGGER = LogManager.getLogger("SSMPS2/ToastPacket");

    public ToastPacket(Component message, String title) {
        this.message = message;
        this.title = title;
    }

    public static void encode(ToastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeComponent(packet.message);
        buffer.writeUtf(packet.title);
    }

    public static ToastPacket decode(FriendlyByteBuf buffer) {
        return new ToastPacket(buffer.readComponent(), buffer.readUtf());
    }

    public static void handle(ToastPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                SystemToast toast = new SystemToast(SystemToast.SystemToastIds.PERIODIC_NOTIFICATION, new TextComponent(packet.title), packet.message);
                Minecraft.getInstance().getToasts().addToast(toast);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
