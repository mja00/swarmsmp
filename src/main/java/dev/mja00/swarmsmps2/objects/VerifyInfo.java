package dev.mja00.swarmsmps2.objects;

public class VerifyInfo {

    private String integration;
    private String username;
    private String identifier;
    private String code;

    public VerifyInfo(String username, String identifier, String code) {
        this.integration = "Minecraft";
        this.username = username;
        this.identifier = identifier;
        this.code = code;
    }

    public String getIntegration() {
        return integration;
    }

    public String getUsername() {
        return username;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getCode() {
        return code;
    }

}
