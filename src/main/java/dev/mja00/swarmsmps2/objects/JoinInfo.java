package dev.mja00.swarmsmps2.objects;

public class JoinInfo {

    private Boolean allow;
    private String msg;

    public JoinInfo(Boolean allow, String msg) {
        this.allow = allow;
        this.msg = msg;
    }

    public Boolean getAllow() {
        return allow;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "JoinInfo{" + "allow=" + allow + ", msg=" + msg + '}';
    }
}
