package dev.mja00.swarmsmps2.objects;

public class Commands {
    private CommandInfo[] commands;

    public Commands(CommandInfo[] commands) {
        this.commands = commands;
    }

    public CommandInfo[] getCommands() {
        return commands;
    }
}
