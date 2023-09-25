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
        float saturationLevel = player.getFoodData().getSaturationLevel();
        float exhaustionToSet = 4 * saturationLevel;
        player.getFoodData().setExhaustion(exhaustionToSet + (4 * 8)); // This should clear any saturation and eat into their chunks
        return 1;
    }
}
