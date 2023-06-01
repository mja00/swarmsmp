package dev.mja00.swarmsmps2.objects;

public class JoinInfo {

    private Boolean whitelisted;
    private String message;

    public JoinInfo(Boolean allow, String message) {
        this.whitelisted = allow;
        this.message = message;
    }

    public Boolean getAllow() {
        // Null pointer check
        if (whitelisted == null) {
            return false;
        }
        return whitelisted;
    }

    public String getMessage() {
        if (message == null) {
            return "You are not signed up on the website.";
        }
        return message;
    }


    @Override
    public String toString() {
        return "JoinInfo{" + "allow=" + whitelisted + '}';
    }
}
