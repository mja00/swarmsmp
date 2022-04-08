package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.UUID;

public class AdminCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;
    static final String translationKey = SwarmsmpS2.translationKey;

    public AdminCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("admin").requires((command) -> {
            return command.hasPermission(2);
        }).then(Commands.literal("set_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word()).executes((command) -> {
            return setTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"));
        })))).then(Commands.literal("remove_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word()).executes((command) -> {
            return removeTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"));
        })))));
    }

    private int setTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag){
        for (ServerPlayer target : targets) {
            target.getPersistentData().putBoolean(SwarmsmpS2.MODID + ":" + tag, true);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.set_tag.success.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.set_tag.success.multiple", targets.size()), true);
        }
        return 1;
    }

    private int removeTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag){
        for (ServerPlayer target : targets) {
            target.getPersistentData().remove(SwarmsmpS2.MODID + ":" + tag);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.remove_tag.success.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.remove_tag.success.multiple", targets.size()), true);
        }
        return 1;
    }
}
