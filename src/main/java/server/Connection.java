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

    private static final String NODES = "Nodes.xml";
    private List<NodeConnectionInfo> nodes = new ArrayList<NodeConnectionInfo>();
    private TimeManager electionTimer, heartbeatTimer;
    private ServerInterface serverInterface;
    private OnTimeListener timeListener;
    private ThreadPool threadPool;

    Connection(Node node, int port) throws RemoteException {
        super();
        readNodesFile();
        registerService(port);
        this.serverInterface = node;
        this.timeListener = node;
        //heartbeatTimer = new TimeManager(this);
        electionTimer = new TimeManager(this);
        threadPool = new ThreadPool(nodes.size());
    }

    private void readNodesFile() {
        HashMap<String, String> map = XmlSerializer.fileToMap(NODES);
        for (Map.Entry<String, String> e : map.entrySet()) {
            nodes.add(new NodeConnectionInfo(e.getKey(), Integer.parseInt(e.getValue())));
        }
    }

    private void registerService(int port) {
        try {
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("server", this);
            System.out.println("Server is ready for action!");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    public int getMajorityNumber() {
        return nodes.size() / 2;
    }

    public String request(String command) throws RemoteException {
        try {
            RequestPacket rp = new RequestPacket(command, getClientHost());
            System.out.println("SERVIDOR: " + rp.toString());
            threadPool.execute(nodes);
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }
        return "SERVIDOR: " + command;
    }

    public void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, ArrayList<Log> entries, int leaderCommit) throws RemoteException {
        if (entries == null) {
            electionTimer.resetTimer();
        } else {
            serverInterface.appendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit);
        }
    }

    public void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm) throws RemoteException {
        serverInterface.requestVote(term, candidateId, lastLogIndex, lastLogIndex);
    }

    public void timeout() {
        timeListener.timeout();
        if(heartbeatTimer != null) {
            heartbeatTimer.stopTimer();
            heartbeatTimer = null;
        }
    }

}
