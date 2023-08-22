package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.helpers.EntityHelpers;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.Logger;

public class HeadCommand {

    private static final String translationKey = SwarmsmpS2.translationKey;
    static Logger LOGGER = SwarmsmpS2.LOGGER;

    public HeadCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("head").executes(context -> generateHead(context.getSource())));
    }

    private int generateHead(CommandSourceStack source) throws CommandSyntaxException {
        // Get the player's UUID
        ServerPlayer player = source.getPlayerOrException();
        ItemStack headItem = EntityHelpers.getPlayerHead(player);

        // Make sure the player has room in their inventory
        boolean success = player.getInventory().add(headItem);
        if (!success) {
            player.spawnAtLocation(headItem);
        }
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.head.success").withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

}
