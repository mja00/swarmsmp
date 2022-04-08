package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class BetterMessageCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    private static final float msPerBlock = 100.F;
    static final UUID DUMMY = Util.NIL_UUID;
    private static final String translationKey = SwarmsmpS2.translationKey;

    public static class MessageRequest {
        public final String id;
        public TextComponent message;
        public MinecraftServer server;
        public ServerPlayer sender;
        public ServerPlayer recipient;
        public long sendTime;

        public MessageRequest(String s) { id = s;}
    }

    public static final HashMap<String, MessageRequest> MessageQueue = new HashMap<>();

    public static MessageRequest create(MinecraftServer server, ServerPlayer sender, ServerPlayer recipient, TextComponent message, long timeToWait) {
        String key;
        do {
            key = String.format("%08X", new Random().nextInt());
        } while (MessageQueue.containsKey(key));

        MessageRequest request = new MessageRequest(key);
        request.server = server;
        request.sender = sender;
        request.recipient = recipient;
        request.message = message;
        request.sendTime = System.currentTimeMillis() + timeToWait;
        MessageQueue.put(key, request);
        return request;
    }

    public BetterMessageCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalcommandnode = dispatcher.register(Commands.literal("msg").then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("message", MessageArgument.message()).executes((command) -> {
                return sendPrivateMessage(command.getSource(), EntityArgument.getPlayers(command, "targets"), MessageArgument.getMessage(command, "message"));
        }))));
        dispatcher.register(Commands.literal("tell").redirect(literalcommandnode));
        dispatcher.register(Commands.literal("w").redirect(literalcommandnode));
    }

    private int sendPrivateMessage(CommandSourceStack source, Collection<ServerPlayer> targets, Component message) {
        // Make sure there is only one recipient
        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.message.players.too_many"));
            return 0;
        }
        ServerPlayer recipient = targets.iterator().next();
        UUID senderUUID = source.getEntity() == null ? DUMMY : source.getEntity().getUUID();
        Entity entity = source.getEntity();
        TextComponent messageText = (TextComponent) message.plainCopy();

        // Check if the sender is a ServerPlayer, if not, it's a console
        if (entity instanceof ServerPlayer sender) {
            // Make sure both players are in the same dimension
            if (sender.getLevel().dimensionType().equals(recipient.getLevel().dimensionType())) {
                // Get the distance to the recipient
                double distance = sender.distanceTo(recipient);
                // Calculate the time
                long time = (long) (distance * msPerBlock);

                // Create the request
                MessageRequest request = create(source.getServer(), sender, recipient, messageText, time);

                // Inform the sender
                sender.sendMessage(new TranslatableComponent(translationKey + "commands.message.sent", recipient.getDisplayName(), message).withStyle(ChatFormatting.GREEN), DUMMY);
                BlockPos pos = sender.getOnPos();
                source.getLevel().playSound(null, pos, SoundEvents.PARROT_FLY, SoundSource.PLAYERS, 1.0F, 1.0F);

                logMessages(sender, recipient, messageText);
            } else {
                source.sendFailure(new TranslatableComponent(translationKey + "commands.message.players.not_same_dimension"));
                return 0;
            }
        }

        return 1;
    }

    private void logMessages(ServerPlayer sender, ServerPlayer recipient, TextComponent message) {
        // Both player's raw names
        String senderName = sender.getName().getString();
        String recipientName = recipient.getName().getString();

        // Message as a string
        String messageString = message.getString();

        LOGGER.info(String.format("[Whisper] %s -> %s: %s", senderName, recipientName, messageString));
    }
}
