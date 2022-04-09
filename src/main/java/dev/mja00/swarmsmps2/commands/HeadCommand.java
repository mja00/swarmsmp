package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class HeadCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    private static final String translationKey = SwarmsmpS2.translationKey;
    static final UUID DUMMY = Util.NIL_UUID;
    private static final String MOD_ID = SwarmsmpS2.MODID;

    public HeadCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("head").executes(context -> {
            return generateHead(context.getSource());
        }));
    }

    private int generateHead(CommandSourceStack source) throws CommandSyntaxException {
        // Get the player's UUID
        ServerPlayer player = source.getPlayerOrException();
        // Create a new item stack with the head of the player's UUID
        CompoundTag headData = new CompoundTag();
        headData.putString("SkullOwner", player.getName().getString());
        ItemStack headItem = new ItemStack(Items.PLAYER_HEAD, 1);
        headItem.setTag(headData);

        // Make sure the player has room in their inventory
        boolean success = player.getInventory().add(headItem);
        if (!success) {
            player.spawnAtLocation(headItem);

        }
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.head.success").withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

}
