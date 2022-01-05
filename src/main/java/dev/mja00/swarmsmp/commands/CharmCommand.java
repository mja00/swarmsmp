package dev.mja00.swarmsmp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmp.SSMPS2;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.util.text.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class CharmCommand {

    static Logger LOGGER = SSMPS2.LOGGER;

    public CharmCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("charm").then(Commands.literal("effect").then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("charm", StringArgumentType.word()).executes((command) -> {
            return charmEffect(command.getSource(), EntityArgument.getPlayers(command, "targets"), StringArgumentType.getString(command, "charm"));
        })))));
    }

    private int charmEffect(CommandSource source, Collection<ServerPlayerEntity> targets, String charm) throws CommandSyntaxException {
        // Get the source player
        ServerPlayerEntity player = source.asPlayer();
        // Check for the is_mer tag
        boolean is_mer = player.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":is_mer");

        // Check to make sure there's only one target
        if (targets.size() != 1) {
            source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.charm.effect.too_many", 1), true);
            return -1;
        }
        // Get the first target in the collection
        ServerPlayerEntity target = targets.iterator().next();

        // Get distance between the player and the target
        double distance = player.getDistance(target);

        // Check if the player is a mer
        if (is_mer) {
            if (distance <= 16.0D) {

                TranslationTextComponent message = (TranslationTextComponent) new TranslationTextComponent(SSMPS2.translationKey + "commands.charm.effect.success", charm, target.getDisplayName()).mergeStyle(TextFormatting.AQUA);
                // Send back a message
                source.sendFeedback(message, true);

                // Send the target a title and subtitle
                target.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE, new TranslationTextComponent(SSMPS2.translationKey + "commands.charm.effect.receive", charm).mergeStyle(TextFormatting.LIGHT_PURPLE), 1, 10, 1));
                target.connection.sendPacket(new STitlePacket(STitlePacket.Type.SUBTITLE, new TranslationTextComponent(SSMPS2.translationKey + "commands.charm.effect.receive_subtitle", player.getDisplayName()).mergeStyle(TextFormatting.AQUA), 1, 10, 1));
                return 1;
            } else {
                source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.charm.effect.too_far").mergeStyle(TextFormatting.RED), true);
                return -1;
            }
        } else {
            source.sendFeedback(new TranslationTextComponent(SSMPS2.translationKey + "commands.charm.effect.not_mer").mergeStyle(TextFormatting.RED), true);
            return -1;
        }
    }
}
