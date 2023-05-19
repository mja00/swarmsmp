package dev.mja00.swarmsmps2.objects;

public class CommandInfo {

    private int id;
    private int user_id;
    private String command;
    private int created;

    public CommandInfo(int id, int user_id, String command, int created) {
        this.id = id;
        this.user_id = user_id;
        this.command = command;
        this.created = created;
    }

    public int getId() {
        return id;
    }

    public int getUser_id() {
        return user_id;
    }

    public String getCommand() {
        return command;
    }

    public int getCreated() {
        return created;
    }
}

