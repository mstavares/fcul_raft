package server;

import client.ClientInterface;
import utilities.*;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Connection extends UnicastRemoteObject implements ClientInterface, ServerInterface, OnTimeListener {

    private static final String SERVICE_NAME = "raft";
    private static final String NODES = "Nodes.xml";
    private List<NodeConnectionInfo> nodesIds = new ArrayList<NodeConnectionInfo>();
    private TimeManager electionTimer, heartbeatTimer;
    private ServerInterface serverInterface;
    private ClientInterface clientInterface;
    private OnTimeListener timeListener;
    private ThreadPool threadPool;

    Connection(Node node, int port) throws RemoteException {
        super();
        readNodesFile();
        registerService(port);
        this.serverInterface = node;
        this.clientInterface = node;
        this.timeListener = node;
        electionTimer = new TimeManager(this);
        threadPool = new ThreadPool(SERVICE_NAME, nodesIds.size());
    }

    private void readNodesFile() {
        HashMap<String, String> map = XmlSerializer.readConfig(NODES);
        for (Map.Entry<String, String> e : map.entrySet()) {
            nodesIds.add(new NodeConnectionInfo(e.getKey(), Integer.parseInt(e.getValue())));
        }
    }

    private void registerService(int port) {
        try {
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind(SERVICE_NAME, this);
            System.out.println("Server is ready for action!");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    private void electionsTimeout() {
        if(heartbeatTimer != null) {
            heartbeatTimer.stopTimer();
            heartbeatTimer = null;
        }
    }

    public int getMajorityNumber() {
        return nodesIds.size() / 2;
    }

    public String request(String command) throws RemoteException, ServerNotActiveException {
        return clientInterface.request(command);
    }

    public void sendEntry(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, Log entries, int leaderCommit) {
        threadPool.sendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit, nodesIds);
    }

    public void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, Log entries, int leaderCommit) throws RemoteException, ServerNotActiveException {
        if (entries == null) {
            Debugger.log("Recebi um heartbeat de: " + getClientHost());
            electionTimer.resetTimer();
        } else {
            serverInterface.appendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit);
        }
    }

    public void askForVotes(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm) {
        electionsTimeout();
        threadPool.askForVotes(term, candidateId, lastLogIndex, lastLogTerm, nodesIds);
    }

    public void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm) throws RemoteException {
        serverInterface.requestVote(term, candidateId, lastLogIndex, lastLogIndex);
    }

    public void sendVote(NodeConnectionInfo candidateId, boolean vote) {
        threadPool.sendVoteReply(candidateId, vote);
    }

    public void onVoteReceive(boolean vote) throws RemoteException {
        serverInterface.onVoteReceive(vote);
    }

    public void sendHeartbeat() {
        electionTimer.resetTimer();
        heartbeatTimer = new TimeManager(this, true);
    }

    public void timeout(TimeManager timeManager) {
        timeListener.timeout(timeManager);
    }

}
