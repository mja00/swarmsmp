package dev.mja00.swarmsmps2.objects;

public class JoinInfo {

    private Boolean whitelisted;

    public JoinInfo(Boolean allow) {
        this.whitelisted = allow;
    }

    public Boolean getAllow() {
        return whitelisted;
    }


    @Override
    public String toString() {
        return "JoinInfo{" + "allow=" + whitelisted + '}';
    }
}
