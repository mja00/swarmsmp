package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class HungerCommand {

    public HungerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hunger")
                .executes((command) -> hunger(command.getSource())));
    }

    private int hunger(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        // Just take away 1 hunger chunk
        int currentFood = player.getFoodData().getFoodLevel();
        if (currentFood > 0) {
            player.getFoodData().setFoodLevel(currentFood - 1);
        }
        return 1;
    }
}
