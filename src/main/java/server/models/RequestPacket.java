package server.models;

import java.sql.Timestamp;

import common.OperationType;

/** Esta classe é um modelo para os pedidos enviados pelos clientes */
public class RequestPacket {

    private static int requestIdGenerator = 0;
    private Timestamp timestamp;
    private String key, oldValue, newValue;
    private String ip;
    private int port;
    private OperationType op;
    private int id;

    public RequestPacket(String ip, int port, OperationType op) {
        timestamp = new Timestamp(System.currentTimeMillis());
        id = requestIdGenerator++;
        this.op = op;
        this.ip = ip;
        this.port = port;
    }

    public RequestPacket(String ip, int port, OperationType op, String key) {
        this(ip, port, op);
        this.key = key;
    }

    public RequestPacket(String ip, int port, OperationType op, String key, String oldValue, String newValue) {
        this(ip, port, op, key);
        this.oldValue = oldValue;
    	this.newValue = newValue;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public OperationType getOperation() {
        return op;
    }

    public String getKey() {
        return key;
    }

    public String getNewValue() {
    	return newValue;
    }
    
    public String getOldValue() {
    	return oldValue;
    }

    public String getIp() {
        return ip;
    }
    
    public int getPort() {
    	return port;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "comando: " + op + " de: " + ip + " com o id: " + id;
    }
}
