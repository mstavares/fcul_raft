package utilities;

import server.NodeConnectionInfo;
import server.ServerInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Esta classe manipula uma série de threads de forma e enviar
 *  os pedidos para todos os servidores.
 *  Os pedidos podem ser heartbeats, entries e votes.
 */
public class ThreadPool {

    private String serviceName;
    private ExecutorService threadPool;

    public ThreadPool(String serviceName, int numberOfThreads) {
        this.serviceName = serviceName;
        threadPool = Executors.newFixedThreadPool(numberOfThreads);
    }

    public void sendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex, int prevLogTerm, Log entries, int leaderCommit, List<NodeConnectionInfo> nodesId) {
        for (NodeConnectionInfo connectionId : nodesId) {
            threadPool.execute(new EntriesWorker(connectionId, term, leaderId, prevLogIndex, prevLogTerm, leaderCommit, entries));
        }
    }

    public void sendEntriesReply(NodeConnectionInfo leaderId, int logIndex, int term, boolean success) {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        threadPool.execute(new ReplierEntries(leaderId, logIndex, term, success));
    }

    public void askForVotes(int term, NodeConnectionInfo candidateId, int lastLogIndex, int lastLogTerm, List<NodeConnectionInfo> nodesId) {
        for (NodeConnectionInfo connectionId : nodesId) {
            Debugger.log(candidateId.toString());
            threadPool.execute(new VotesWorker(connectionId, term, candidateId, lastLogIndex, lastLogTerm));
        }
    }

    public void sendVoteReply(NodeConnectionInfo candidateId, boolean vote) {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        threadPool.execute(new ReplierVotesWorker(candidateId, vote));
    }


    /** Este worker serve de classe mãe para os restantes workers,
     *  evitando assim duplicação de código. */
    private class Worker {

        int term, logIndex, logTerm;
        NodeConnectionInfo connectionId, myId;
        Log entries;

        Worker(NodeConnectionInfo connectionId) {
            this.connectionId = connectionId;
        }

        Worker(NodeConnectionInfo connectionId, int term, NodeConnectionInfo myId, int logIndex, int logTerm, Log entries) {
            this.connectionId = connectionId;
            this.term = term;
            this.myId = myId;
            this.logIndex = logIndex;
            this.logTerm = logTerm;
            this.entries = entries;
        }

    }

    /** Este worker é responsável por enviar os heartbeats e as entries
     * para os outros servidores.
     */
    private class EntriesWorker extends Worker implements Runnable {

        int leaderCommit;

        EntriesWorker(NodeConnectionInfo connectionId, int term, NodeConnectionInfo myId, int logIndex, int logTerm, int leaderCommit, Log entries) {
            super(connectionId, term, myId, logIndex, logTerm, entries);
            this.leaderCommit = leaderCommit;
        }

        public void run() {
            try {
                Registry registry = LocateRegistry.getRegistry(connectionId.getIpAddress(), connectionId.getPort());
                ServerInterface stub = (ServerInterface) registry.lookup(serviceName);
                stub.appendEntries(term, myId, logIndex, logTerm, entries, leaderCommit);
            } catch (RemoteException e) {
                e.printStackTrace();
                //Debugger.log("Nao consegui ligar ao ip: " + connectionId.getIpAddress() + " com a porta: " + connectionId.getPort());
            } catch (NotBoundException e) {
                e.printStackTrace();
            } catch (ServerNotActiveException e) {
                e.printStackTrace();
            }
        }
    }

    private class ReplierEntries extends Worker implements Runnable {

        boolean success;

        ReplierEntries(NodeConnectionInfo connectionId, int logIndex, int term, boolean success) {
            super(connectionId, term, null, logIndex, 0, null);
            this.success = success;
        }

        public void run() {
            try {
                Registry registry = LocateRegistry.getRegistry(connectionId.getIpAddress(), connectionId.getPort());
                ServerInterface stub = (ServerInterface) registry.lookup(serviceName);
                stub.appendEntriesReply(logIndex, term, success);
            } catch (RemoteException e) {
                e.printStackTrace();
                //Debugger.log("Nao consegui ligar ao ip: " + connectionId.getIpAddress() + " com a porta: " + connectionId.getPort());
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    /** Este worker é responsável por enviar os pedidos de votos
     *  para os outros servidores.
     */
    private class VotesWorker extends Worker implements Runnable {

        VotesWorker(NodeConnectionInfo connectionId, int term, NodeConnectionInfo myId, int logIndex, int logTerm) {
            super(connectionId, term, myId, logIndex, logTerm, null);
        }

        public void run() {
            try {
                Registry registry = LocateRegistry.getRegistry(connectionId.getIpAddress(), connectionId.getPort());
                ServerInterface stub = (ServerInterface) registry.lookup(serviceName);
                stub.requestVote(term, myId, logIndex, logTerm);
            } catch (RemoteException e) {
                e.printStackTrace();
                //Debugger.log("Nao consegui ligar ao ip: " + connectionId.getIpAddress() + " com a porta: " + connectionId.getPort());
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    /** Este worker é responsável por responder ao servidor que
     *  fez o pedido de votos.
     */
    private class ReplierVotesWorker extends Worker implements Runnable {

        private boolean vote;

        ReplierVotesWorker(NodeConnectionInfo connectionId, boolean vote) {
            super(connectionId);
            this.vote = vote;
        }

        public void run() {
            try {
                Registry registry = LocateRegistry.getRegistry(connectionId.getIpAddress(), connectionId.getPort());
                ServerInterface stub = (ServerInterface) registry.lookup(serviceName);
                stub.onVoteReceive(vote);
            } catch (RemoteException e) {
                e.printStackTrace();
                //Debugger.log("Nao consegui ligar ao ip: " + connectionId.getIpAddress() + " com a porta: " + connectionId.getPort());
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }
    }


}
