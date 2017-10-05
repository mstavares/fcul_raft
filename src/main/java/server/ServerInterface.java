package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public interface ServerInterface extends Remote {
    void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex,
                       int prevLogTerm, ArrayList<String> entries, int leaderCommit) throws RemoteException;

    void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex,
                     int lastLogTerm) throws RemoteException;
}