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

/**
 * Esta classe é a camada de ligação dos servidores.
 * Serve para enviar e receber pedidos.
 */
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

    /** Este método lê os servidores existentes do ficheiro de configuração */
    private void readNodesFile() {
        HashMap<String, String> map = XmlSerializer.readConfig(NODES);
        for (Map.Entry<String, String> e : map.entrySet()) {
            nodesIds.add(new NodeConnectionInfo(e.getKey(), Integer.parseInt(e.getValue())));
        }
    }

    /** Este método regista o servidor para receber pedidos via RMI */
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

    /** Este método é invocado quando o timer da eleições faz timeout */
    private void electionsTimeout() {
        if(heartbeatTimer != null) {
            heartbeatTimer.stopTimer();
            heartbeatTimer = null;
        }
    }

    /** Este método devolve o número de servidores correspondentes à maioria */
    public int getMajorityNumber() {
        return nodesIds.size() / 2;
    }

    /** Este método recebe os pedidos RMI do cliente */
    public String request(String command) throws RemoteException, ServerNotActiveException {
        return clientInterface.request(command);
    }

    /** Este método envia os logs para os outros servidores */
    public void sendEntry(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, Log entries, int leaderCommit) {
        threadPool.sendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit, nodesIds);
    }

    /** Este método recebe os logs dos outros servidores.
     *  Se o pedido vier com o entries a null quer dizer que é um heartbeat
     */
    public void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, Log entries, int leaderCommit) throws RemoteException, ServerNotActiveException {
        if (entries == null) {
            Debugger.log("Recebi um heartbeat de: " + getClientHost());
            electionTimer.resetTimer();
        } else {
            serverInterface.appendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit);
        }
    }

    /** Este método envia um pedido de voto aos outros servidores */
    public void askForVotes(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm) {
        electionsTimeout();
        threadPool.askForVotes(term, candidateId, lastLogIndex, lastLogTerm, nodesIds);
    }

    /** Este método recebe os pedidos de votos dos outros servidores */
    public void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm) throws RemoteException {
        serverInterface.requestVote(term, candidateId, lastLogIndex, lastLogIndex);
    }

    /** Este método envia a resposta ao pedido de voto de um servidor */
    public void sendVote(NodeConnectionInfo candidateId, boolean vote) {
        threadPool.sendVoteReply(candidateId, vote);
    }

    /** Este método recebe os resultados dos votos dos servidores */
    public void onVoteReceive(boolean vote) throws RemoteException {
        serverInterface.onVoteReceive(vote);
    }

    /** Este método ativa o timer correspondente aos heartbeats */
    public void enableHeartbeatTimer() {
        electionTimer.resetTimer();
        heartbeatTimer = new TimeManager(this, true);
    }

    /** Este método é invocado quando um timeout ocorre, quer seja de eleição quer seja de heartbeat */
    public void timeout(TimeManager timeManager) {
        timeListener.timeout(timeManager);
    }

}
