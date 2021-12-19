package dev.mja00.swarmsmp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.mja00.swarmsmp.SSMPS2;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class AdminCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    public AdminCommand(CommandDispatcher<CommandSource> dispatcher) {

        dispatcher.register(Commands.literal("admin").requires((command) -> {
            return command.hasPermissionLevel(2);
        }).then(Commands.literal("set_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word()).executes((command) -> {
                return SetTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"));
        })))).then(Commands.literal("remove_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tags", StringArgumentType.word()).executes((command) -> {
            return RemoveTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tags"));
        })))).then(Commands.literal("check_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word()).executes((command) -> {
            return CheckTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"));
        })))));

    }

    private int SetTag(CommandSource source, Collection<ServerPlayerEntity> targets, String tag) {
        for (ServerPlayerEntity target : targets) {
            target.getPersistentData().putBoolean(SSMPS2.MOD_ID + ":" + tag, true);
        }

        if (targets.size() == 1) {
            source.sendFeedback(new TranslationTextComponent("commands.admin.set_tag.success.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendFeedback(new TranslationTextComponent("commands.admin.set_tag.success.multiple", targets.size()), true);
        }
        return 1;
    }

    private int RemoveTag(CommandSource source, Collection<ServerPlayerEntity> targets, String tag) {
        for (ServerPlayerEntity target : targets) {
            target.getPersistentData().remove(SSMPS2.MOD_ID + ":" + tag);
        }

        if (targets.size() == 1) {
            source.sendFeedback(new TranslationTextComponent("commands.admin.remove_tag.success.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendFeedback(new TranslationTextComponent("commands.admin.remove_tag.success.multiple", targets.size()), true);
        }
        return 1;
    }

    private int CheckTag(CommandSource source, Collection<ServerPlayerEntity> targets, String tag) {
        for (ServerPlayerEntity target : targets) {
            if (target.getPersistentData().contains(SSMPS2.MOD_ID + ":" + tag)) {
                source.sendFeedback(new TranslationTextComponent("commands.admin.check_tag.success", target.getDisplayName()), true);
            } else {
                source.sendFeedback(new TranslationTextComponent("commands.admin.check_tag.failed", target.getDisplayName()), true);
            }
        }
        return 1;
    }
}
