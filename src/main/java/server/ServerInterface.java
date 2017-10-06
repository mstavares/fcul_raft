package server;

import utilities.Log;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ServerInterface extends Remote {
    void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex,
                       int prevLogTerm, ArrayList<Log> entries, int leaderCommit) throws RemoteException;

    void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex,
                     int lastLogTerm) throws RemoteException;
}