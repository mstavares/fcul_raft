package server;

import utilities.Log;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;

public interface ServerInterface extends Remote {

    void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex,
                       int prevLogTerm, Log entries, int leaderCommit) throws RemoteException, ServerNotActiveException;

    void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex,
                     int lastLogTerm) throws RemoteException;

    void onVoteReceive(boolean vote) throws RemoteException;

}