package server;

import common.NotLeaderException;
import common.OnTimeListener;
import common.OperationType;
import common.SnapshotOnTimeListener;
import common.TimeManager;
import server.interfaces.ClientInterface;
import server.interfaces.ConnectionInterface;
import server.interfaces.ServerInterface;
import server.models.LogEntry;
import server.models.NodeConnectionInfo;
import server.models.RequestPacket;
import server.models.RaftStatus;
import utilities.*;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Esta class √© respons√°vel por enviar, receber e interpretar todos os tipos de pedidos,
 * quer sejam dos servidores quer sejam dos clientes.
 */
public class Node implements ServerInterface, ClientInterface, ConnectionInterface, OnTimeListener, SnapshotOnTimeListener {

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
    
    /** State Machine **/
    private StateMachine stateMachine;

    /** Volatile state on all servers */
    private int commitIndex = -1;
    private int lastApplied = -1;
    
    private boolean majority = false;
    

    Node() throws RemoteException {
        setFollower();
        readNodesFile();
        /** cÛdigo relacionado com snapshots comentado atÈ conseguirmos arranjar uma soluÁ„o **/
        /*
        try {
			recoverStatus();
		} catch (IOException e) {
			System.out.println("Error recovering status");
			e.printStackTrace();
			System.exit(-1);
		}
		*/
        HashMap<String, String> map = XmlSerializer.readConfig(NODE_CONFIG);
        Debugger.log("A minha config ip: " + map.get("ipAddress") + " porta: " + map.get("port"));
        nodeId = new NodeConnectionInfo(map.get("ipAddress"), Integer.parseInt(map.get("port")));
        connection = new Connection(nodesIds,this, nodeId.getPort());
        stateMachine = new StateMachine();
        // new TimeManager(this, true, 15);
    }

    /** Este m√©todo l√™ os servidores existentes do ficheiro de configura√ß√£o */
    private void readNodesFile() {
        HashMap<String, String> map = XmlSerializer.readConfig(NODES);
        for (Map.Entry<String, String> e : map.entrySet()) {
            nodesIds.add(new NodeConnectionInfo(e.getKey(), Integer.parseInt(e.getValue())));
        }
    }

    private void storeCurrentStatus(OperationType op, int term, String key, String oldValue, String newValue) {
        // RaftStatus raftStatus = new RaftStatus(requests, votedFor, currentTerm, logs);
    	Debugger.log("Applying operation to log");
        try {
			// new FileManager().writeDatabaseToFile(raftStatus);
        	new FileManager().appendOperationToLog(op, term, key, oldValue, newValue);
        	Debugger.log("Applied operation to log");
		} catch (IOException e) {
			System.out.println("Error appending operation to log");
			e.printStackTrace();
		}
    }

    private void recoverStatus() throws IOException {
        RaftStatus raftStatus = new FileManager().restoreDatabase();
        currentTerm = raftStatus.getCurrentTerm();
        requests = raftStatus.getRequests();
        votedFor = raftStatus.getVotedFor();
        logs = raftStatus.getLogs();
    }

    /** Este m√©todo define o estado do servidor como lider */
    private void setLeader() {
        Debugger.log("Alterei o meu estado para LEADER!");
        updateLeaderId(nodeId);
        role = Role.LEADER;
    }

    /** Este m√©todo define o estado do servidor como candidato */
    private void setCandidate() {
        Debugger.log("Alterei o meu estado para CANDIDATE!");
        leaderId = null;
        role = Role.CANDIDATE;
    }

    /** Este m√©todo define o estado do servidor como follower */
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

    /** Este m√©todo recebe os pedidos dos clientes provenientes da camada de liga√ß√£o. */
    public String request(OperationType op, String key, String oldValue, String newValue) throws ServerNotActiveException, NotLeaderException {
        if(role == Role.LEADER) {
            RequestPacket rp = new RequestPacket(RemoteServer.getClientHost(), 1095, op);
            Debugger.log("Recebi o request: " + rp.toString());

            logs.add(new LogEntry(op, currentTerm, key, oldValue, newValue));
            storeCurrentStatus(op,this.currentTerm, key, oldValue, newValue); /** <------ */
            Debugger.log("Logs: " + logs.toString());
            requests.add(rp);
            processNextRequest();
            boolean brk = false;
            String result = null;
            while(!brk) {
            	// && processingRequest == rp
            	Debugger.log("Iterating");
            	if(majority) {
            		Debugger.log("Got all majority, applying to statemachine");
            		majority = false;
            		brk = true;
                    Debugger.log("Incrementar o commitIndex de: " + commitIndex + " para: " + (commitIndex + 1));
                    commitIndex++;
                    result = applyToStateMachine();
                    Debugger.log("Repplying with :" + result);
                    requestProcessed();
                    processNextRequest();
            	}
                
            }
            return result;

        } else {
            if(leaderId != null) {
            	Debugger.log("Redirected client to leader");
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
    private void checkTerm(int term, NodeConnectionInfo leaderId) {
        if (term > currentTerm) {
            Debugger.log("term: " + term + " currentTerm: " + currentTerm);
            Debugger.log("Vou alterar o meu estado de " + role.toString() + " para " + Role.FOLLOWER.toString());
            stepDown(term);
            updateLeaderId(leaderId);
        }
    }

    /** Regra 1 de All Servers */
    private String applyToStateMachine() {
    	Debugger.log("Entering StateMachine");
        if(commitIndex > lastApplied) {
            lastApplied++;
            // TODO apply logs.get(lastApplied) to state machine
            LogEntry last = logs.getLogEntryOfIndex(lastApplied);
            String res;
            switch(last.getOp()) {
			case CAS:
				res = stateMachine.cas(last.getKey(), last.getOldValue(), last.getNewValue());
				Debugger.log("Adicionei a operaÁ„o CAS ‡ state Machine");
				break;
			case DEL:
				stateMachine.del(last.getKey());
				res = "Valor Removido";
				Debugger.log("Adicionei a operaÁ„o DEL ‡ state Machine");
				break;
			case GET:
				res = stateMachine.get(last.getKey());
				Debugger.log("Adicionei a operaÁ„o GET ‡ state Machine");
				break;
			case LIST:
				res = stateMachine.list();
				Debugger.log("Adicionei a operaÁ„o LIST ‡ state Machine");
				break;
			case PUT:
				stateMachine.put(last.getKey(), last.getNewValue());
				res = "Valor inserido";
				Debugger.log("Adicionei a operaÁ„o PUT ‡ state Machine");
				break;
			default:
				res = null;
				Debugger.log("OperaÁ„o Inv·lida");
				break;
            }
            return res;
        }
        return null;
    }

    /** Este m√©todo recebe os pedidos de appendEntries da camada de liga√ß√£o */
    public void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, LogEntry entry, int leaderCommit) throws RemoteException {
    	// TODO Isto È mesmo aqui ou dentro do if?
    	/** Regra 1 de All Servers */
        applyToStateMachine();
        /** Regra 2 de All Servers */
        checkTerm(term, leaderId);
        /** Regra 1 e 2 de AppendEntries RPC */
        Debugger.log("Conteudo do log antes do append: " + logs.toString());
        if(prevLogIndex == -1 || (term >= currentTerm && prevLogIndex <= logs.getLastLogIndex() && logs.getTermOfIndex(prevLogIndex) == prevLogTerm)){
            Debugger.log("Vou fazer append de um log!");
            /** Adicionar ao Log **/
            logs.appendLog(entry);
            storeCurrentStatus(entry.getOp(),entry.getTerm(), entry.getKey(), entry.getOldValue(), entry.getNewValue());
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

    public void appendEntriesReply(NodeConnectionInfo replier, boolean success, int logIndex, int prevLogTerm) throws RemoteException, NotBoundException {
        Debugger.log("Fez append? " + success);
        NodeConnectionInfo node = findNode(replier);
        updateNodeIndexes(node, success);
        if(success) {
            logs.addReplicatedNode(node, logIndex);
            if(logs.getNumberOfReplicatedNodes(logIndex) + 1 > getMajority() && logIndex >= commitIndex) {
            	/*
            	Debugger.log("Incrementar o commitIndex de: " + commitIndex + " para: " + (commitIndex + 1));
                commitIndex++;
                repyToClient(replyToStateMachine());
                requestProcessed();
                processNextRequest();
            	 */
            	Debugger.log("Got Majority");
            	majority = true;
            }
        } else {
            Debugger.log("Append entries rejeitado.");
        }
    }
    
    /*
    private void replyToClient(String result) throws RemoteException, NotBoundException {
    	Registry r = LocateRegistry.getRegistry(processingRequest.getIp(), processingRequest.getPort());
    	ReplyInterface stub = (ReplyInterface) r.lookup("raft");
    	stub.reply(result);
    }
    */

    private void updateNodeIndexes(NodeConnectionInfo node, boolean success) {
        if(success) {
            node.incrementIndexes();
        } else {
            node.decrementNextIndex();
        }
        Debugger.log("O matchIndex foi atualizado para: " + node.getMatchIndex() + " e o nextIndex para: " + node.getNextIndex());
    }

    /** Este m√©todo recebe os pedidos de votos da camada de liga√ß√£o */
    public void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm) throws RemoteException {
        /** Regra 2 de All Servers */
        checkTerm(term, candidateId);
        /** Regra 2 de RequestVote RPC */
        if(term >= currentTerm && votedFor == null && logs.areMyLogsOutdated(lastLogIndex, lastLogTerm)) {
            Debugger.log("Vou votar no: " + candidateId.toString());
            votedFor = candidateId;
            connection.sendVote(candidateId, true);
            // storeCurrentStatus();
        } else {
            /** Regra 1 de RequestVote RPC */
            Debugger.log("N√£o vou votar no: " + candidateId.toString());
            connection.sendVote(candidateId, false);
        }
    }

    /** Este m√©todo recebe a resposta aos pedidos de votos */
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

    /** Este m√©todo √© invocado quando um timeout ocorre. √â necess√°rio
     * verificar que tipo de timeout √©, se de heartbeat ou de elei√ß√µes */
    public void timeout(TimeManager timeManager) {
        if (timeManager.isHeartbeat()) {
            sendHeartbeat();
        } else {
            /** Regra 4 de Candidates */
            votes = 0;
            electionsTimeout();
        }
    }

    /** Este m√©todo envia um heartbeat para os outros servidores */
    private void sendHeartbeat() {
        /** Regra 1 de Leaders */
        sendAppendEntries();
    }

    /** Este m√©todo trata da inicia√ß√£o dos processos de elei√ß√µes */
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

    /** Este m√©todo √© invocado durante um processo de elei√ß√£o em que
     * este servidor recebe um requestVote de um candidato cujo term
     * √© superior ao dele.
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
        // storeCurrentStatus();
    }

    /** Ap√≥s a elei√ß√£o de um lider √© necess√°rio reiniciar os indices matchIndex e nextIndex */
    private void initializeIndexes() {
        Debugger.log("Vou reinicializar os indices de cada follower");
        for(NodeConnectionInfo node : nodesIds) {
            node.resetMatchIndex();
        }
    }

	@Override
	public void snapshot(TimeManager timeManager) {
		Debugger.log("Initiating Snapshot");
		RaftStatus raftStatus = new RaftStatus(requests, votedFor, currentTerm, logs);
		try {
			new FileManager().writeDatabaseToFile(raftStatus);
			Debugger.log("Snapshot Complete");
		} catch (IOException e) {
			System.out.println("Error storing current status");
			e.printStackTrace();
		}
	}

}