package server;

import client.ClientInterface;
import utilities.*;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.HashMap;

/**
 * Esta class é responsável por enviar, receber e interpretar todos os tipos de pedidos,
 * quer sejam dos servidores quer sejam dos clientes.
 */
public class Node implements ServerInterface, ClientInterface, OnTimeListener {

    private static final String NODE_CONFIG = "NodeConfig.xml";
    private enum Role {LEADER, CANDIDATE, FOLLOWER}
    private NodeConnectionInfo nodeId;
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

    Node () throws RemoteException {
        setFollower();
        HashMap<String, String> map = XmlSerializer.readConfig(NODE_CONFIG);
        Debugger.log("A minha config ip: " + map.get("ipAddress") + " porta: " + map.get("port"));
        nodeId = new NodeConnectionInfo(map.get("ipAddress"), Integer.parseInt(map.get("port")));
        connection = new Connection(this, nodeId.getPort());
    }

    /** Este método define o estado do servidor como lider */
    private void setLeader() {
        role = Role.LEADER;
    }

    /** Este método define o estado do servidor como candidato */
    private void setCandidate() {
        role = Role.CANDIDATE;
    }

    /** Este método define o estado do servidor como follower */
    private void setFollower() {
        role = Role.FOLLOWER;
    }

    /** Este método recebe os pedidos dos clientes provenientes da camada de ligação. */
    public String request(String command) throws ServerNotActiveException {
        RequestPacket rp = new RequestPacket(command, RemoteServer.getClientHost());
        Debugger.log("Recebi o request: " + rp.toString());
        return "SERVIDOR: " + rp.toString();
    }

    /** Este método recebe os pedidos de appendEntries da camada de ligação */
    public void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, Log entries, int leaderCommit) throws RemoteException {
        if(role != Role.FOLLOWER) {
            if (term > currentTerm) {
                stepDown(term);
            }
        } else {
            /* TODO replicar logs */
        }
    }

    /** Este método recebe os pedidos de votos da camada de ligação */
    public void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm) throws RemoteException {
        if (term > currentTerm) {
            Debugger.log("Vou alterar o meu estado de " + role.toString() + " para " + Role.FOLLOWER.toString());
            stepDown(term);
        }
        if(term >= currentTerm && votedFor == null && logs.areMyLogsOutdated(lastLogIndex, lastLogTerm)) {
            Debugger.log("Vou votar no: " + candidateId.toString());
            votedFor = candidateId;
            connection.sendVote(candidateId, true);
        } else {
            Debugger.log("Não vou votar no: " + candidateId.toString());
            connection.sendVote(candidateId, false);
        }
    }

    /** Este método recebe a resposta aos pedidos de votos */
    public void onVoteReceive(boolean vote) {
        if(vote)
            votes++;
        Debugger.log("Tenho " + votes + " votos");
        if(votes >= connection.getMajorityNumber()) {
            Debugger.log("Fui eleito como lider!");
            votes = 0;
            setLeader();
            sendHeartbeat();
            connection.enableHeartbeatTimer();
        }
    }

    /** Este método é invocado quando um timeout ocorre. É necessário
     * verificar que tipo de timeout é, se de heartbeat ou de eleições */
    public void timeout(TimeManager timeManager) {
        if (timeManager.isHeartbeat())
            sendHeartbeat();
        else
            electionsTimeout();
    }

    /** Este método envia um heartbeat para os outros servidores */
    private void sendHeartbeat() {
        connection.sendEntry(currentTerm, nodeId, logs.getLastLogIndex(), logs.getLastLogTerm(), null, commitIndex);
    }

    /** Este método trata da iniciação dos processos de eleições */
    public void electionsTimeout() {
        Debugger.log("Vou iniciar uma eleicao");
        setCandidate();
        currentTerm++;
        votes++;
        connection.askForVotes(currentTerm, nodeId, logs.getLastLogIndex(), logs.getLastLogTerm());
    }

    /** Este método é invocado durante um processo de eleição em que
     * este servidor recebe um requestVote de um candidato cujo term
     * é superior ao dele.
     */
    private void stepDown(int term) {
        setFollower();
        currentTerm = term;
        votedFor = null;
        votes = 0;
    }


}
