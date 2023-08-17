package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class ReplyCommand {
    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;
    static final String translationKey = SwarmsmpS2.translationKey;

    public ReplyCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("reply")
                .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("message", MessageArgument.message())
                    .executes((command) -> sendReply(command.getSource(), StringArgumentType.getString(command, "id"), MessageArgument.getMessage(command, "message"))))));
    }

    private int sendReply(CommandSourceStack source, String id, Component message) throws CommandSyntaxException {
        // We need to look up first if there's even a message request with that id
        // If there is, we'll send the message to the player who requested it
        AdminCommand.AdminMessage replyMsg = AdminCommand.MESSAGES.get(id);
        if (replyMsg == null) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.reply.no_message"));
            return 0;
        }
        // We have a message, so we'll send it to the player
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.message.sent", replyMsg.source.getName(), message), false);
        replyMsg.source.sendMessage(new TranslatableComponent(translationKey + "commands.message.received", source.getPlayerOrException().getName(), message).withStyle(ChatFormatting.AQUA), DUMMY);

        // It was sent so we remove it from the map
        AdminCommand.MESSAGES.remove(id);
        return 1;
    }
}
