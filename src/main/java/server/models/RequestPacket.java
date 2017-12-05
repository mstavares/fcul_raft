package server.models;

import java.sql.Timestamp;

import common.OperationType;

/** Esta classe é um modelo para os pedidos enviados pelos clientes */
public class RequestPacket {

    private static int requestIdGenerator = 0;
    private Timestamp timestamp;
    private String key, oldValue, newValue;
    private OperationType op;
    private int id, clientId;

    public RequestPacket(OperationType op, int clientId) {
        timestamp = new Timestamp(System.currentTimeMillis());
        id = requestIdGenerator++;
        this.op = op;
        this.clientId = clientId;
    }

    public RequestPacket(OperationType op, int clientId, String key) {
        this(op, clientId);
        this.key = key;
    }

    public RequestPacket(OperationType op, int clientId, String key, String oldValue, String newValue) {
        this(op, clientId, key);
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

    public int getId() {
        return id;
    }
    public int getClientId() {
    	return clientId;
    }

    @Override
    public String toString() {
        return "comando: " + op + " com o id: " + id;
    }
}
