package dev.mja00.swarmsmps2.objects;

public class JoinInfo {

    private Boolean whitelisted;
    private String message;

    public JoinInfo(Boolean allow, String message) {
        this.whitelisted = allow;
        this.message = message;
    }

    public Boolean getAllow() {
        return whitelisted;
    }

    public String getMessage() {
        return message;
    }


    @Override
    public String toString() {
        return "JoinInfo{" + "allow=" + whitelisted + '}';
    }
}
