package server.models;

import server.Log;

import java.io.Serializable;
import java.util.ArrayList;

public class RaftStatus implements Serializable {

	private static final long serialVersionUID = -8014037637389049745L;
	
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

    public RaftStatus() {
    	// TODO please double check if this constructor makes sense
    	requests = new ArrayList<RequestPacket>();
    	votedFor = null; //TODO not sure como inicializar isto?
    	currentTerm= 0;
    	logs = new Log();
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
