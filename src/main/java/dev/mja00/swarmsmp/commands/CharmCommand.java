package dev.mja00.swarmsmp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;

public class CharmCommand {
    public CharmCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("charm").then(Commands.literal("effect").then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("charm", StringArgumentType.word()).executes((command) -> {
            return charmEffect(command.getSource(), EntityArgument.getPlayers(command, "targets"), StringArgumentType.getString(command, "charm"));
        })))));
    }

    private int charmEffect(CommandSource source, Collection<ServerPlayerEntity> targets, String charm) throws CommandSyntaxException {
        // Get the source player
        ServerPlayerEntity player = source.asPlayer();

        // Check to make sure there's only one target
        if (targets.size() != 1) {
            source.sendFeedback(new TranslationTextComponent("commands.charm.effect.too_many", 1), true);
            return -1;
        }
        // Get the first target in the collection
        ServerPlayerEntity target = targets.iterator().next();
        TranslationTextComponent message = new TranslationTextComponent("commands.charm.effect.success", charm, target.getDisplayName());
        // Send back a message
        source.sendFeedback(message, true);

        // Send the target a title and subtitle
        target.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE, new TranslationTextComponent("commands.charm.effect.receive", charm), 1, 10, 1));
        target.connection.sendPacket(new STitlePacket(STitlePacket.Type.SUBTITLE, new TranslationTextComponent("commands.charm.effect.receive_subtitle", player.getDisplayName()), 1, 10, 1));
        return 1;
    }
}
