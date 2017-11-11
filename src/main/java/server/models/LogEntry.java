package server.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LogEntry implements Serializable {

    private List<String> replicatedNodesIds = new ArrayList<String>();
    private String command;
    private int term;

    public LogEntry(String command, int term) {
        this.command = command;
        this.term = term;
    }

    public int getNumberOfReplicatedNodes() {
        return replicatedNodesIds.size();
    }

    public String getCommand() {
        return command;
    }

    public int getTerm() {
        return term;
    }

    public void addReplicatedNode(NodeConnectionInfo replicatedNode) {
        if(!replicatedNodesIds.contains(replicatedNode.getId()))
            replicatedNodesIds.add(replicatedNode.getId());
    }

    @Override
    public String toString() {
        return "Comando: " + command + " term: " + term;
    }
}
