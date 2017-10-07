package server;

import java.sql.Timestamp;

public class RequestPacket {

    private Timestamp timestamp;
    private String command;
    private String ip;

    public RequestPacket(String ip, String command) {
        timestamp = new Timestamp(System.currentTimeMillis());
        this.command = command;
        this.ip = ip;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getCommand() {
        return command;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return "comando: " + command + " de: " + ip;
    }
}
