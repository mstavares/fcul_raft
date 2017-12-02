package server.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import common.OperationType;

public class LogEntry implements Serializable {

    private List<String> replicatedNodesIds = new ArrayList<String>();
    private OperationType op;
    private int term;
    private String key, oldValue, newValue;

    public LogEntry(OperationType op, int term) {
        this.op = op;
        this.term = term;
    }
    
    public LogEntry(OperationType op, int term, String key) {
    	this(op, term);
    	this.key = key;
    }
    
    public LogEntry(OperationType op, int term, String key, String oldValue, String newValue) {
    	this(op, term, key);
    	this.oldValue = oldValue;
    	this.newValue = newValue;
    }

    public int getNumberOfReplicatedNodes() {
        return replicatedNodesIds.size();
    }

    public OperationType getOp() {
        return op;
    }

    public int getTerm() {
        return term;
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

    public void addReplicatedNode(NodeConnectionInfo replicatedNode) {
        if(!replicatedNodesIds.contains(replicatedNode.getId()))
            replicatedNodesIds.add(replicatedNode.getId());
    }

    @Override
    public String toString() {
        return "Comando: " + op + " term: " + term;
    }
}
