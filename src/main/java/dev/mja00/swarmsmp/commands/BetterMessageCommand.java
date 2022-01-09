package dev.mja00.swarmsmp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.mja00.swarmsmp.SSMPS2;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class BetterMessageCommand {

    static Logger LOGGER = SSMPS2.LOGGER;
    private static final float msPerBlock = 100.0F;
    static final UUID DUMMY = Util.DUMMY_UUID;

    public static class MessageRequest {
        public final String id;
        public ITextComponent message;
        public MinecraftServer server;
        public ServerPlayerEntity sender;
        public ServerPlayerEntity receiver;
        public long sendTime;

        public MessageRequest(String s) { id = s; }
    }

    public static final HashMap<String, MessageRequest> MessageQueue = new HashMap<>();

    public static MessageRequest create(MinecraftServer server, ServerPlayerEntity sender, ServerPlayerEntity receiver, ITextComponent message, long timeToWait) {
        String key;

        do {
            key = String.format("%08X", new Random().nextInt());
        } while (MessageQueue.containsKey(key));

        MessageRequest request = new MessageRequest(key);
        request.server = server;
        request.sender = sender;
        request.receiver = receiver;
        request.message = message;
        request.sendTime = System.currentTimeMillis() + timeToWait;
        MessageQueue.put(key, request);
        return request;
    }

    public BetterMessageCommand(CommandDispatcher<CommandSource> dispatcher) {
        LiteralCommandNode<CommandSource> literalcommandnode = dispatcher.register(Commands.literal("msg").then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("message", MessageArgument.message()).executes((command) -> {
            return sendPrivateMessage(command.getSource(), EntityArgument.getPlayers(command, "targets"), MessageArgument.getMessage(command, "message"));
        }))));
        dispatcher.register(Commands.literal("tell").redirect(literalcommandnode));
        dispatcher.register(Commands.literal("w").redirect(literalcommandnode));
    }

    private static int sendPrivateMessage(CommandSource source, Collection<ServerPlayerEntity> recipients, ITextComponent message) {
        // Make sure there is only one recipient
        if (recipients.size() != 1) {
            source.sendErrorMessage(new TranslationTextComponent("commands.message.players.too_many").mergeStyle(TextFormatting.RED));
            return 0;
        }
        ServerPlayerEntity recipient = recipients.iterator().next();

        UUID uuid = source.getEntity() == null ? Util.DUMMY_UUID : source.getEntity().getUniqueID();
        Entity entity = source.getEntity();

        // Check if the sender is a ServerPlayerEntity, if not assume it's a console
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity sender = (ServerPlayerEntity) entity;
            // Check if the recipient and the sender are in the same dimension
            if (sender.getEntityWorld().equals(recipient.getEntityWorld())) {
                // We can then simulate distance to the recipient
                double distance = sender.getDistance(recipient);
                // Calculate the time it will take to send the message
                double time = distance * msPerBlock;

                // Queue the message for sending
                MessageRequest request = create(sender.getServer(), sender, recipient, message, (long) time);
                // Send the message to the sender, and for coolness we'll play the bat takeoff sound effect
                sender.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.message.sent", recipient.getDisplayName(), message).mergeStyle(TextFormatting.GREEN), DUMMY);
                BlockPos pos = sender.getPosition();
                source.getWorld().playSound(null, pos, SoundEvents.ENTITY_PARROT_FLY, SoundCategory.PLAYERS, 1.0F, 1.0F);

                // We also want to log this interaction to the console as well, so we can watch people :)
                logMessages(sender, recipient, message);

            } else {
                // Inform the sender that the message was not sent
                source.sendErrorMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.message.players.not_same_dimension").mergeStyle(TextFormatting.RED));
                return 0;
            }
        } else {
            // This is the console so we're just going to use the sendFeedback method
            source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.message.sent", recipient.getDisplayName(), message), true);
            recipient.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.message.received", source.getDisplayName(), message).mergeStyle(TextFormatting.AQUA), uuid);
        }

        return 1;
    }

    private static void logMessages(ServerPlayerEntity sender, ServerPlayerEntity recipient, ITextComponent message) {
        // We need both players raw names
        String senderName = sender.getName().getUnformattedComponentText();
        String recipientName = recipient.getName().getUnformattedComponentText();

        // We also need the message as a string
        String messageString = message.getUnformattedComponentText();

        // Now we log it
        LOGGER.info("[Whisper] {} -> {}: {}", senderName, recipientName, messageString);
    }
}
