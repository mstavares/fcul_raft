package server;

import client.ClientInterface;
import utilities.OnTimeListener;
import utilities.ThreadPool;
import utilities.TimeManager;
import utilities.XmlSerializer;

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
    private ThreadPool threadPool;

    public Connection(TimeManager electionTimer, int port) throws RemoteException {
        super();
        readNodesFile();
        registerService(port);
        this.electionTimer = electionTimer;
        heartbeatTimer = new TimeManager(this);
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

    public void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, ArrayList<String> entries, int leaderCommit) throws RemoteException {
        System.out.println("RECEBI!!!");
        /* TODO Metodo appendEntries */
    }

    public void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm) throws RemoteException {
        /* TODO Metodo requestVote */
    }

    public void timeout() {
        /* TODO Metodo que envia heartbeats */
    }


}
