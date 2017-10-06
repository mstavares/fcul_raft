package server;

import utilities.Log;
import utilities.OnTimeListener;
import utilities.XmlSerializer;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Node implements ServerInterface, OnTimeListener {

    private static final String NODE_CONFIG = "NodeConfig.xml";
    private enum Role {LEADER, CANDIDATE, FOLLOWER}
    private NodeConnectionInfo connectionInfo;
    private Connection connection;
    private Log logs = new Log();
    private Role role;
    private int votes;

    /** Persistent state on all servers */
    private NodeConnectionInfo votedFor;
    private int currentTerm;

    /** Volatile state on all servers */
    private int commitIndex;
    private int lastApplied;

    /** Volatile state on leaders */
    private int[] nextIndex;
    private int[] matchIndex;

    public Node () throws RemoteException {
        setFollower();
        HashMap<String, String> map = XmlSerializer.fileToMap(NODE_CONFIG);
        connectionInfo = new NodeConnectionInfo(map.get("ipAddress"), Integer.parseInt(map.get("port")));
        connection = new Connection(this, connectionInfo.getPort());
    }

    public void setLeader() {
        role = Role.LEADER;
    }

    public void setCandidate() {
        role = Role.CANDIDATE;
    }

    public void setFollower() {
        role = Role.FOLLOWER;
    }


    /* TODO Metodo appendEntries */
    public void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, ArrayList<Log> entries, int leaderCommit) throws RemoteException {
        if(role != Role.FOLLOWER) {
            if (term > currentTerm) {
                stepDown(term);
            }
        } else {
            /* Escrever no log */
        }
    }

    /* TODO Metodo requestVote */
    public void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm) throws RemoteException {
        if (term > currentTerm) {
            stepDown(term);
        }
        if(term >= currentTerm && votedFor == null && logs.areMyLogsOutdated(lastLogIndex, lastLogTerm)) {

        }
    }

    /* TODO pedir votos */
    public void timeout() {
        setCandidate();
        currentTerm++;
        votes++;
    }

    private void stepDown(int term) {
        setFollower();
        currentTerm = term;
        votedFor = null;
        votes = 0;
    }


}
