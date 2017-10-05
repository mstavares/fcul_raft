package server;

import utilities.OnTimeListener;
import utilities.TimeManager;
import utilities.XmlSerializer;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Node implements OnTimeListener, OnLogListener {

    private static final String NODE_CONFIG = "NodeConfig.xml";
    private enum State {LEADER, CANDIDATE, FOLLOWER}
    private List<String> logs = new ArrayList<String>();
    private NodeConnectionInfo connectionInfo;
    private Connection connection;
    private State state;

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
        HashMap<String, String> map = XmlSerializer.fileToMap(NODE_CONFIG);
        connectionInfo = new NodeConnectionInfo(map.get("ipAddress"), Integer.parseInt(map.get("port")));
        connection = new Connection(new TimeManager(this), connectionInfo.getPort());
    }

    public void setLeader() {
        state = State.LEADER;
    }

    public void setCandidate() {
        state = State.CANDIDATE;
    }

    public void setFollower() {
        state = State.FOLLOWER;
    }

    public void onAppendLog(String command) {
        logs.add(command);
    }

    public void timeout() {
        currentTerm++;
        setCandidate();
    }

}
