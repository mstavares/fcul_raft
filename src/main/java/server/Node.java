package server;

import common.ElectingException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Esta class é responsável por enviar, receber e interpretar todos os tipos de pedidos,
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
    private FileManager fileManager;

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

    // private boolean majority = false;
    private ConcurrentHashMap<Integer, Boolean> requestMap = new ConcurrentHashMap<>();
    private AtomicInteger idGen = new AtomicInteger();


    Node() throws RemoteException {
        setFollower();
        readNodesFile();
        /** c�digo relacionado com snapshots comentado at� conseguirmos arranjar uma solu��o **/
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
        System.setProperty("java.rmi.server.hostname", map.get("ipAddress"));
        nodeId = new NodeConnectionInfo(map.get("ipAddress"), Integer.parseInt(map.get("port")));
        connection = new Connection(nodesIds,this, nodeId.getPort());
        stateMachine = new StateMachine();
        fileManager = new FileManager();
        // new TimeManager(this, true, 15);
    }

    /** Este método lê os servidores existentes do ficheiro de configuração */
    private void readNodesFile() {
        HashMap<String, String> map = XmlSerializer.readConfig(NODES);
        for (Map.Entry<String, String> e : map.entrySet()) {
            nodesIds.add(new NodeConnectionInfo(e.getKey(), Integer.parseInt(e.getValue())));
        }
    }

    private void storeCurrentStatus() {
    /*
        RaftStatus raftStatus = new RaftStatus(requests, votedFor, currentTerm, logs);
    	Debugger.log("Applying operation to log");
        try {
			this.fileManager.writeDatabaseToFile(raftStatus);
        	// fileManager.appendOperationToLog(op, term, key, oldValue, newValue);
        	Debugger.log("Applied operation to log");
		} catch (IOException e) {
			System.out.println("Error appending operation to log");
			e.printStackTrace();
		}
    */
    }

    private void recoverStatus() throws IOException {
        RaftStatus raftStatus = new FileManager().restoreDatabase();
        currentTerm = raftStatus.getCurrentTerm();
        requests = raftStatus.getRequests();
        votedFor = raftStatus.getVotedFor();
        logs = raftStatus.getLogs();
    }

    /** Este método define o estado do servidor como lider */
    private void setLeader() {
        Debugger.log("Alterei o meu estado para LEADER!");
        updateLeaderId(nodeId);
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

    /** Este método recebe os pedidos dos clientes provenientes da camada de ligação.
     * @throws ElectingException */
    public String request(OperationType op, String key, String oldValue, String newValue) throws ServerNotActiveException, NotLeaderException{
        if(role == Role.LEADER) {
        	int id = idGen.incrementAndGet();
        	requestMap.put(id, false);
            RequestPacket rp = new RequestPacket(op, id);
            Debugger.log("Recebi o request: " + rp.toString());

            logs.add(new LogEntry(op, currentTerm, key, oldValue, newValue));
            storeCurrentStatus();
            Debugger.log("Logs: " + logs.toString());
            requests.add(rp);
            processNextRequest();
            String result = null;
            while(requestMap.get(id) == false) {
            	Debugger.log("Waiting for majority");
            	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }
            requestMap.remove(id);
    		Debugger.log("Got all majority, applying to statemachine");
            Debugger.log("Incrementar o commitIndex de: " + commitIndex + " para: " + (commitIndex + 1));
            commitIndex++;
            result = applyToStateMachine(commitIndex);
            Debugger.log("Replying with :" + result);
            requestProcessed();
            processNextRequest();
            return result;

        } else {
            if(leaderId != null) {
            	Debugger.log("Redirected client to leader");
                throw new NotLeaderException(leaderId.getIpAddress()+ ":" +leaderId.getPort());
            } else {
            	Debugger.log("Electing leader");
                // throw new ElectingException("Raft is Electing the Leader");
            	return "electing";
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
    private String applyToStateMachine(int commitIndex) {
        if(commitIndex > lastApplied) {
            Debugger.log("Entering StateMachine");
            //lastApplied++;
            // TODO apply logs.get(lastApplied) to state machine

            Debugger.log("++++++++++++++++++++++");
            Debugger.log("CommitIndex = " + commitIndex + " lastApplied = " + lastApplied);
            Debugger.log("LOG \n" + logs.toString());
            Debugger.log("\n STATE MACHINE ANTES \n" + stateMachine.toString());


            LogEntry last = null;
            try {
                last = logs.getLogEntryOfIndex(lastApplied + 1);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }


            String res;
            switch(last.getOp()) {
			case CAS:
				res = stateMachine.cas(last.getKey(), last.getOldValue(), last.getNewValue());
                lastApplied++;
				Debugger.log("Adicionei a operacao CAS a state Machine");
				break;
			case DEL:
				stateMachine.del(last.getKey());
				res = "Valor Removido";
                lastApplied++;
				Debugger.log("Adicionei a operacao DEL a state Machine");
				break;
			case GET:
				res = stateMachine.get(last.getKey());
                lastApplied++;
				Debugger.log("Adicionei a operacao GET a state Machine");
				break;
			case LIST:
				res = stateMachine.list();
                lastApplied++;
				Debugger.log("Adicionei a operacao LIST a state Machine");
				break;
			case PUT:
				stateMachine.put(last.getKey(), last.getNewValue());
				res = "Valor inserido";
                lastApplied++;
				Debugger.log("Adicionei a operacao PUT a state Machine");
				break;
			default:
				res = null;
				Debugger.log("Operacao Invalida");
				break;
            }
            Debugger.log("commitIndex =  " + commitIndex + " lastApplied = " + lastApplied);
            Debugger.log("\n STATE MACHINE DEPOIS \n" + stateMachine.toString());
            Debugger.log("++++++++++++++++++++++");
            return res;
        }
        return null;
    }

    /** Este método recebe os pedidos de appendEntries da camada de ligação */
    public void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, LogEntry entry, int leaderCommit) throws RemoteException {
    	/** Regra 1 de All Servers */
        updateCommitIndex(leaderCommit);
        applyToStateMachine(leaderCommit);
        /** Regra 2 de All Servers */
        checkTerm(term, leaderId);
        if(entry != null) {
            /** Regra 1 e 2 de AppendEntries RPC */
            Debugger.log("Conteudo do log antes do append: " + logs.toString());
            if (prevLogIndex == -1 || (term >= currentTerm && prevLogIndex <= logs.getLastLogIndex() && logs.getTermOfIndex(prevLogIndex) == prevLogTerm)) {
                Debugger.log("Vou fazer append de um log!");
                /** Adicionar ao Log **/
                logs.appendLog(entry, prevLogIndex);
                //storeCurrentStatus();
                connection.sendAppendEntriesReply(leaderId, nodeId, true, logs.getLastLogIndex(), -10);
            } else {
                Debugger.log("Nao vou fazer append do log!");
                connection.sendAppendEntriesReply(leaderId, nodeId, false, -20, -30);
            }
            Debugger.log("Conteudo do log depois do append: " + logs.toString());
        }
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
                if(processingRequest != null) {
                    //commitIndex++;
            		Debugger.log("Got Majority");
            		requestMap.put(processingRequest.getClientId(), true);
            	} else {
            		Debugger.log("Accepted delayed entry");
            	}
            }
        } else {
            Debugger.log("Append entries rejeitado.");
        }
    }

    private void updateCommitIndex(int leaderCommit) {
        if(leaderCommit > commitIndex)
            commitIndex = Math.min(leaderCommit, logs.getLastLogIndex());
        Debugger.log("updateCommitIndex : " + commitIndex);
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
        checkTerm(term, candidateId);
        /** Regra 2 de RequestVote RPC */
        if(term >= currentTerm && votedFor == null && logs.areMyLogsOutdated(lastLogIndex, lastLogTerm)) {
            Debugger.log("Vou votar no: " + candidateId.toString());
            votedFor = candidateId;
            connection.sendVote(candidateId, true);
            storeCurrentStatus();
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
        storeCurrentStatus();
    }

    /** Após a eleição de um lider é necessário reiniciar os indices matchIndex e nextIndex */
    private void initializeIndexes() {
        Debugger.log("Vou reinicializar os indices de cada follower");
        for(NodeConnectionInfo node : nodesIds) {
            node.resetMatchIndex();
        }
    }

	@Override
	public void snapshot(TimeManager timeManager) {
        /*
		Debugger.log("Initiating Snapshot");
		RaftStatus raftStatus = new RaftStatus(requests, votedFor, currentTerm, logs);
		try {
			fileManager.writeDatabaseToFile(raftStatus);
			Debugger.log("Snapshot Complete");
		} catch (IOException e) {
			System.out.println("Error storing current status");
			e.printStackTrace();
		}
		*/
	}

}