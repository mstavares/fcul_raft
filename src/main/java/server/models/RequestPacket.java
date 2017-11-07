package server.models;

import java.sql.Timestamp;

/** Esta classe é um modelo para os pedidos enviados pelos clientes */
public class RequestPacket {

    private static int requestIdGenerator = 0;
    private Timestamp timestamp;
    private String command;
    private String ip;
    private int id;

    public RequestPacket(String ip, String command) {
        timestamp = new Timestamp(System.currentTimeMillis());
        id = requestIdGenerator++;
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

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "comando: " + command + " de: " + ip + " com o id: " + id;
    }
}
