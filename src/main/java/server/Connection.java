package server;

import common.ElectingException;
import common.NotLeaderException;
import common.OnTimeListener;
import common.OperationType;
import common.TimeManager;
import server.interfaces.ClientInterface;
import server.interfaces.ConnectionInterface;
import server.interfaces.ServerInterface;
import server.models.LogEntry;
import server.models.NodeConnectionInfo;
import utilities.*;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * Esta classe é a camada de ligação dos servidores.
 * Serve para enviar e receber pedidos.
 */
public class Connection extends UnicastRemoteObject implements ClientInterface, ServerInterface, OnTimeListener {
	
	private static final long serialVersionUID = -6470055805559967731L;
	
	private static final String SERVICE_NAME = "raft";
    private TimeManager electionTimer, heartbeatTimer;
    private ConnectionInterface connectionInterface;
    private ServerInterface serverInterface;
    private ClientInterface clientInterface;
    private OnTimeListener timeListener;
    private ThreadPool threadPool;

    Connection(List<NodeConnectionInfo> nodesIds, Node node, int port) throws RemoteException {
        super();
        registerService(port);
        this.connectionInterface = node;
        this.serverInterface = node;
        this.clientInterface = node;
        this.timeListener = node;
        electionTimer = new TimeManager(this);
        threadPool = new ThreadPool(SERVICE_NAME, nodesIds.size());
    }

    /** Este método regista o servidor para receber pedidos via RMI */
    private void registerService(int port) {
        try {
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind(SERVICE_NAME, this);
            System.out.println("ServerMain is ready for action!");
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

    /** Este método recebe os pedidos RMI do cliente 
     * @throws ElectingException */
    public String request(OperationType op, String key, String oldValue, String newValue) throws RemoteException, ServerNotActiveException, NotLeaderException, ElectingException{
        return clientInterface.request(op, key, oldValue, newValue);
    }

    /** Este método envia os logs para os outros servidores */
    public void sendAppendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, LogEntry entry, int leaderCommit, NodeConnectionInfo connectionId) {
        if(entry == null) {
            String teste;
            //Debugger.log("Vou enviar um heartbeat para " + connectionId.toString());
        } else
            Debugger.log("Vou enviar " + entry.toString() + " para " + connectionId.toString());
        threadPool.sendAppendEntries(term, leaderId, prevLogIndex, prevLogTerm, entry, leaderCommit, connectionId);
    }

    /** Este método recebe os logs dos outros servidores.
     *  Se o pedido vier com o entries a null quer dizer que é um heartbeat
     */
    public void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, LogEntry entry, int leaderCommit) throws RemoteException, ServerNotActiveException {
        //Debugger.log("Recebi um appendEntries de: " + getClientHost());
        connectionInterface.updateLeaderId(leaderId);
        electionTimer.resetTimer();
        //if(entry != null)
            serverInterface.appendEntries(term, leaderId, prevLogIndex, prevLogTerm, entry, leaderCommit);
    }

    public void appendEntriesReply(NodeConnectionInfo replier, boolean success, int logIndex, int prevLogTerm) throws RemoteException, NotBoundException {
        serverInterface.appendEntriesReply(replier, success, logIndex, prevLogTerm);
    }

    public void sendAppendEntriesReply(NodeConnectionInfo leaderId, NodeConnectionInfo nodeId, boolean success, int prevLogIndex, int prevLogTerm) {
        threadPool.sendEntriesReply(leaderId, nodeId, success, prevLogIndex, prevLogTerm);
    }

    /** Este método envia um pedido de voto aos outros servidores */
    public void askForVotes(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm, List<NodeConnectionInfo> nodesIds) {
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
        heartbeatTimer = new TimeManager(this, true);
    }

    public void disableHeartbeatTimer() {
        if(heartbeatTimer != null) {
            heartbeatTimer.stopTimer();
            heartbeatTimer = null;
        }
    }

    public void disableElectionTimer() {
        electionTimer.stopTimer();
    }


    public void enableElectionTimer() {
        electionTimer.stopTimer();
        electionTimer = new TimeManager(this);
    }

    /** Este método é invocado quando um timeout ocorre, quer seja de eleição quer seja de heartbeat */
    public void timeout(TimeManager timeManager) {
        timeListener.timeout(timeManager);
    }

}