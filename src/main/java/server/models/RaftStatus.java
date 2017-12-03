package server.models;

import server.Log;

import java.io.Serializable;
import java.util.ArrayList;

public class RaftStatus implements Serializable {

    private ArrayList<RequestPacket> requests;
    private NodeConnectionInfo votedFor;
    private int currentTerm;
    private Log logs;

    public RaftStatus(ArrayList<RequestPacket> requests, NodeConnectionInfo votedFor, int currentTerm, Log logs) {
        this.requests = requests;
        this.votedFor = votedFor;
        this.currentTerm = currentTerm;
        this.logs = logs;
    }

    public ArrayList<RequestPacket> getRequests() {
        return requests != null ? requests : new ArrayList<RequestPacket>();
    }

    public NodeConnectionInfo getVotedFor() {
        return votedFor;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public Log getLogs() {
        return logs != null ? logs : new Log();
    }

}
