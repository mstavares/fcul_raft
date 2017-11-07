package server;

import client.ClientInterface;
import common.NotLeaderException;
import common.OnTimeListener;
import common.TimeManager;
import server.interfaces.ConnectionInterface;
import server.interfaces.ServerInterface;
import server.models.LogEntry;
import server.models.NodeConnectionInfo;
import server.models.RequestPacket;
import utilities.*;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Esta class é responsável por enviar, receber e interpretar todos os tipos de pedidos,
 * quer sejam dos servidores quer sejam dos clientes.
 */
public class Node implements ServerInterface, ClientInterface, ConnectionInterface, OnTimeListener {

    private static final String NODE_CONFIG = "NodeConfig.xml";
    private static final String NODES = "Nodes.xml";
    private List<NodeConnectionInfo> nodesIds = new ArrayList<NodeConnectionInfo>();
    private enum Role {LEADER, CANDIDATE, FOLLOWER}
    private NodeConnectionInfo nodeId, leaderId;
    private RequestPacket processingRequest;
    private Connection connection;
    private Role role;
    private int votes;

    /** Persistent state on all servers */
    private ArrayList<RequestPacket> requests = new ArrayList<RequestPacket>();
    private NodeConnectionInfo votedFor;
    private Log logs = new Log();
    private int currentTerm;

    /** Volatile state on all servers */
    private int commitIndex = -1;
    private int lastApplied;


    Node() throws RemoteException {
        setFollower();
        readNodesFile();
        HashMap<String, String> map = XmlSerializer.readConfig(NODE_CONFIG);
        Debugger.log("A minha config ip: " + map.get("ipAddress") + " porta: " + map.get("port"));
        nodeId = new NodeConnectionInfo(map.get("ipAddress"), Integer.parseInt(map.get("port")));
        connection = new Connection(nodesIds,this, nodeId.getPort());
    }

    /** Este método lê os servidores existentes do ficheiro de configuração */
    private void readNodesFile() {
        HashMap<String, String> map = XmlSerializer.readConfig(NODES);
        for (Map.Entry<String, String> e : map.entrySet()) {
            nodesIds.add(new NodeConnectionInfo(e.getKey(), Integer.parseInt(e.getValue())));
        }
    }

    /** Este método define o estado do servidor como lider */
    private void setLeader() {
        Debugger.log("Alterei o meu estado para LEADER!");
        leaderId = nodeId;
        role = Role.LEADER;
    }

    /** Este método define o estado do servidor como candidato */
    private void setCandidate() {
        Debugger.log("Alterei o meu estado para CANDIDATE!");
        leaderId = null;
        role = Role.CANDIDATE;
    }

    /** Este método define o estado do servidor como follower */
    private void setFollower() {
        Debugger.log("Alterei o meu estado para FOLLOWER!");
        role = Role.FOLLOWER;
    }

    private void processNextRequest() {
        if(processingRequest == null && !requests.isEmpty()) {
            processingRequest = requests.get(0);
            execute();
        }
    }

    private void requestProcessed() {
        if(processingRequest != null) {
            requests.remove(processingRequest);
            processingRequest = null;
        }
    }

    private void execute() {
        sendAppendEntries();
    }

    private void sendAppendEntries() {
        for(NodeConnectionInfo nodeToConnect : nodesIds) {
            connection.sendAppendEntries(currentTerm, nodeId, logs.fetchPrevLogIndexToSend(nodeToConnect),
                    logs.fetchPrevLogTermToSend(nodeToConnect), logs.fetchNextEntryToSend(nodeToConnect), commitIndex, nodeToConnect);
        }
    }

    /** Este método recebe os pedidos dos clientes provenientes da camada de ligação. */
    public String request(String command) throws ServerNotActiveException, NotLeaderException {
        if(role == Role.LEADER) {
            RequestPacket rp = new RequestPacket(command, RemoteServer.getClientHost());
            Debugger.log("Recebi o request: " + rp.toString());
            logs.add(new LogEntry(command, currentTerm));
            Debugger.log("Logs: " + logs.toString());
            requests.add(rp);
            processNextRequest();
            return "O pedido " + rp.toString() + " foi adicionado à fila de execucao.";
        } else {
            if(leaderId != null) {
                throw new NotLeaderException(leaderId.getIpAddress()+ ":" +leaderId.getPort());
            } else {
                return "O raft esta neste momento a eleger o lider.";
            }
        }
    }

    public void updateLeaderId(NodeConnectionInfo leaderId) {
        this.leaderId = leaderId;
    }

    /** Regra 2 de All Servers */
    private void checkTerm(int term) {
        if (term > currentTerm) {
            Debugger.log("term: " + term + " currentTerm: " + currentTerm);
            Debugger.log("Vou alterar o meu estado de " + role.toString() + " para " + Role.FOLLOWER.toString());
            stepDown(term);
        }
    }

    /** Regra 1 de All Servers */
    private void applyToStateMachine() {
        if(commitIndex > lastApplied) {
            lastApplied++;
            // TODO apply logs.get(lastApplied) to state machine
        }
    }

    /** Este método recebe os pedidos de appendEntries da camada de ligação */
    public void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, LogEntry entry, int leaderCommit) throws RemoteException {
        /** Regra 1 de All Servers */
        applyToStateMachine();
        /** Regra 2 de All Servers */
        checkTerm(term);
        /** Regra 1 e 2 de AppendEntries RPC */
        Debugger.log("Conteudo do log antes do append: " + logs.toString());
        if(prevLogIndex == -1 || (term >= currentTerm && prevLogIndex <= logs.getLastLogIndex() && logs.getTermOfIndex(prevLogIndex) == prevLogTerm)){
            Debugger.log("Vou fazer append de um log!");
            logs.appendLog(entry);
            connection.sendAppendEntriesReply(leaderId, nodeId, true, logs.getLastLogIndex(), -10);
        } else {
            Debugger.log("logs.getTermOfIndex(prevLogIndex): " + logs.getTermOfIndex(prevLogIndex));
            Debugger.log("prevLogTerm: " + prevLogTerm);
            connection.sendAppendEntriesReply(leaderId, nodeId, false, -20, -30);
        }
        Debugger.log("Conteudo do log depois do append: " + logs.toString());
    }

    private NodeConnectionInfo findNode(NodeConnectionInfo replier) {
        for(NodeConnectionInfo node : nodesIds) {
            if (node.getId().equals(replier.getId())) {
                return node;
            }
        }
        return null;
    }

    public void appendEntriesReply(NodeConnectionInfo replier, boolean success, int logIndex, int prevLogTerm) {
        Debugger.log("Fez append? " + success);
        NodeConnectionInfo node = findNode(replier);
        updateNodeIndexes(node, success);
        if(success) {
            logs.addReplicatedNode(node, logIndex);
            if(logs.getNumberOfReplicatedNodes(logIndex) + 1 > getMajority() && logIndex >= commitIndex) {
                Debugger.log("Incrementar o commitIndex de: " + commitIndex + " para: " + (commitIndex + 1));
                commitIndex++;
                requestProcessed();
                processNextRequest();
            }


            /*
            Debugger.log("Vou incrementar os votos desta entry");
            for(int i = 0; i < logs.getLastLogIndex(); i++) {
                boolean commitIndexUpdated = false;


                int numberOfNodesConfirmed = 1;

                for(NodeConnectionInfo nodeId : nodesIds) {
                    if(nodeId.getMatchIndex() >= commitIndex) {
                        if (nodeId.getMatchIndex() >= i) {
                            numberOfNodesConfirmed++;
                            if (numberOfNodesConfirmed > getMajority()) {
                                commitIndexUpdated = true;
                                Debugger.log("Incrementar o commitIndex de: " + commitIndex + " para: " + (commitIndex + 1));
                                commitIndex++;
                                requestProcessed();
                                processNextRequest();
                            }
                        }
                    }
                    if(!commitIndexUpdated)
                        break;
                }
            }
            */
        } else {
            Debugger.log("Append entries rejeitado.");
        }
    }

    private void updateNodeIndexes(NodeConnectionInfo node, boolean success) {
        if(success) {
            node.incrementIndexes();
        } else {
            node.decrementNextIndex();
        }
        Debugger.log("O matchIndex foi atualizado para: " + node.getMatchIndex() + " e o nextIndex para: " + node.getNextIndex());
    }

    /** Este método recebe os pedidos de votos da camada de ligação */
    public void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm) throws RemoteException {
        /** Regra 2 de All Servers */
        checkTerm(term);
        /** Regra 2 de RequestVote RPC */
        if(term >= currentTerm && votedFor == null && logs.areMyLogsOutdated(lastLogIndex, lastLogTerm)) {
            Debugger.log("Vou votar no: " + candidateId.toString());
            votedFor = candidateId;
            connection.sendVote(candidateId, true);
        } else {
            /** Regra 1 de RequestVote RPC */
            Debugger.log("Não vou votar no: " + candidateId.toString());
            connection.sendVote(candidateId, false);
        }
    }

    /** Este método recebe a resposta aos pedidos de votos */
    public void onVoteReceive(boolean vote) {
        if(role == Role.CANDIDATE) {
            if (vote)
                votes++;
            Debugger.log("Tenho " + votes + " votos");
            if (votes > getMajority()) {
                /** Regra 2 de Candidates */
                Debugger.log("Fui eleito como lider!");
                setLeader();
                sendHeartbeat();
                connection.disableElectionTimer();
                connection.enableHeartbeatTimer();
                votes = 0;
                initializeIndexes();
            }
        }
    }

    public int getMajority() {
        return (nodesIds.size() + 1) / 2;
    }

    /** Este método é invocado quando um timeout ocorre. É necessário
     * verificar que tipo de timeout é, se de heartbeat ou de eleições */
    public void timeout(TimeManager timeManager) {
        if (timeManager.isHeartbeat()) {
            sendHeartbeat();
        } else {
            /** Regra 4 de Candidates */
            votes = 0;
            electionsTimeout();
        }
    }

    /** Este método envia um heartbeat para os outros servidores */
    private void sendHeartbeat() {
        /** Regra 1 de Leaders */
        sendAppendEntries();
    }

    /** Este método trata da iniciação dos processos de eleições */
    private void electionsTimeout() {
        /** Regra 1 de Candidates */
        Debugger.log("electionsTimeout");
        connection.disableHeartbeatTimer();
        if(role != Role.LEADER) {
            Debugger.log("Vou iniciar uma eleicao, vou alterar o meu termo para: " + (currentTerm + 1));
            setCandidate();
            /** Regra 1.1 de Candidates */
            currentTerm++;
            /** Regra 1.2 de Candidates */
            votes++;
            /** Regra 1.4 de Candidates */
            connection.askForVotes(currentTerm, nodeId, logs.getLastLogIndex(), logs.getLastLogTerm(), nodesIds);
        }
    }

    /** Este método é invocado durante um processo de eleição em que
     * este servidor recebe um requestVote de um candidato cujo term
     * é superior ao dele.
     */
    private void stepDown(int term) {
        /** Regra 1.3 de Candidates */
        connection.enableElectionTimer();
        connection.disableHeartbeatTimer();
        /** Regra 3 de Candidates */
        setFollower();
        currentTerm = term;
        votedFor = null;
        votes = 0;
    }

    /** Após a eleição de um lider é necessário reiniciar os indices matchIndex e nextIndex */
    private void initializeIndexes() {
        Debugger.log("Vou reinicializar os indices de cada follower");
        for(NodeConnectionInfo node : nodesIds) {
            node.resetMatchIndex();
        }
    }

}
