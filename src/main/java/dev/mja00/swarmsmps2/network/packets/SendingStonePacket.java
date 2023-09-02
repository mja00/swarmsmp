package dev.mja00.swarmsmps2.network.packets;

import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class SendingStonePacket {
    private final UUID player;
    private final String message;
    private final String itemUUID;
    private final ItemStack item;
    private static final Logger LOGGER = LogManager.getLogger("SSMPS2/SendingStonePacket");

    public SendingStonePacket(UUID player, String message, String itemUUID, ItemStack item) {
        this.player = player;
        this.message = message;
        this.itemUUID = itemUUID;
        this.item = item;
    }

    public static void encode(SendingStonePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.itemUUID);
        buffer.writeUtf(packet.message);
        // We'll write the player's UUID for the sender
        buffer.writeUUID(packet.player);
        buffer.writeItemStack(packet.item, false);
    }

    public static SendingStonePacket decode(FriendlyByteBuf buffer) {
        String itemUUID = buffer.readUtf();
        String message = buffer.readUtf();
        UUID player = buffer.readUUID();
        ItemStack item = buffer.readItem();
        return new SendingStonePacket(player, message, itemUUID, item);
    }

    public static void handle(SendingStonePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
                // Get the player from the UUID
                ServerPlayer sourcePlayer = ctx.get().getSender();
                if (sourcePlayer == null) {
                    LOGGER.error("Sender is null!");
                    return;
                }
                if (sourcePlayer.getServer() == null) {
                    LOGGER.error("Server is null!");
                    return;
                }
                MinecraftServer server = sourcePlayer.getServer();
                // If the sourcePlayer's uuid doesn't match the packet's player uuid, we'll just return (something weird going on)
                if (!sourcePlayer.getUUID().toString().equals(packet.player.toString())) {
                    LOGGER.error("Packet source did not match packet creator. Something weird is going on!");
                    LOGGER.error("Packet source: " + sourcePlayer.getUUID() + " , Packet creator: " + packet.player);
                    return;
                }
                ItemStack sendingStone = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(SwarmsmpS2.MODID, "sending_stone")));
                HashMap<String, Integer> playersWithStones = new HashMap<String, Integer>();
                LOGGER.info("Received message from " + packet.player + " with UUID " + packet.itemUUID + ": " + packet.message);
                // We can now assume the sender is the originating player
                // Now we loop through all the players on the server and look for ones with sending stones in their inventories, we'll save these for the next step
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    // Ignore our source player
//                    if (player == sourcePlayer) {
//                        continue;
//                    }
                    // Get their inventory
                    // If their inventory is empty just skip them
                    if (player.getInventory().isEmpty()) {
                        continue;
                    }
                    // Check if their inventory contains a sending stone

                    if (player.getInventory().contains(sendingStone)) {
                        // Add them to the list of players with stones
                        // Get the itemstack with the same UUID as the one in the packet
                        NonNullList<ItemStack> items = player.getInventory().items;
                        // Loop through their inventory and find the first stone with our wanted UUID
                        items.stream().filter(itemStack -> itemStack.getOrCreateTag().getString(SwarmsmpS2.MODID + ":paired").equals(packet.itemUUID)).findFirst().ifPresent(item -> {
                            // Add the player to the hashmap with whatever index this item is at
                            playersWithStones.put(player.getUUID().toString(), items.indexOf(item));
                        });
                    }
                }
                // Cool we've gone through all the players
                // Do we have any players?
                if (playersWithStones.isEmpty()) {
                    sourcePlayer.displayClientMessage(new TranslatableComponent(SwarmsmpS2.translationKey + "message.item.sending_stone.no_players").withStyle(ChatFormatting.GOLD), true);
                } else {
                    sourcePlayer.displayClientMessage(new TranslatableComponent(SwarmsmpS2.translationKey + "message.item.sending_stone.sent").withStyle(ChatFormatting.GREEN), true);
                    // Now we'll loop through the players with our stones and send them a message
                    for (Map.Entry<String, Integer> entry : playersWithStones.entrySet()) {
                        // Play a sound for them, and send them a message
                        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(entry.getKey()));
                        if (player == null) {
                            continue;
                        }
                        player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
                        player.displayClientMessage(new TranslatableComponent(SwarmsmpS2.translationKey + "message.item.sending_stone.received_preamble").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                        player.sendMessage(new TranslatableComponent(SwarmsmpS2.translationKey + "message.item.sending_stone.received", packet.message).withStyle(ChatFormatting.LIGHT_PURPLE), sourcePlayer.getUUID());
                        // Get the item at the slot we saved earlier
                        ItemStack item = player.getInventory().items.get(entry.getValue());
                        // Damage that bih
                        item.hurtAndBreak(1, player, (p) -> {
                            p.broadcastBreakEvent(p.getUsedItemHand());
                        });
                    }
                    // Message has been sent out, now we set a cooldown on the source player's stone for an hour from now
                    // We'll do this by adding a cooldown tag to the stone
                    sourcePlayer.getMainHandItem().getOrCreateTag().putLong(SwarmsmpS2.MODID + ":cooldown", System.currentTimeMillis() + SSMPS2Config.SERVER.sendingStoneCooldown.get());
                    // Take 1 durability from the stone
                    sourcePlayer.getMainHandItem().hurtAndBreak(1, sourcePlayer, (player) -> {
                        player.broadcastBreakEvent(player.getUsedItemHand());
                    });
                }
            });
        });
        ctx.get().setPacketHandled(true);

    }
}
