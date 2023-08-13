package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class FallbackCommand {

    public FallbackCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fbserver")
                .executes((command) -> fallback(command.getSource())));
    }

    private int fallback(CommandSourceStack source) throws CommandSyntaxException {
        // We literally just run a command as op :)
        String playerName = source.getPlayerOrException().getName().getString();
        source.getServer().getCommands().performCommand(source.getServer().createCommandSourceStack(), "redirect " + playerName + " fallback.swarmsmp.com");
        return Command.SINGLE_SUCCESS;
    }
}
