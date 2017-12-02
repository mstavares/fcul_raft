package server.interfaces;

import server.models.LogEntry;
import server.models.NodeConnectionInfo;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

/** Interface RMI servidor -> servidor */
public interface ServerInterface extends Remote {

    void appendEntries(int term, NodeConnectionInfo leaderId, int prevLogIndex,
                       int prevLogTerm, LogEntry entry, int leaderCommit) throws RemoteException, ServerNotActiveException;

    void appendEntriesReply(NodeConnectionInfo replier, boolean success, int prevLogIndex, int prevLogTerm) throws RemoteException, NotBoundException;

    void requestVote(int term, NodeConnectionInfo candidateId, int lastLogIndex,
                     int lastLogTerm) throws RemoteException;

    void onVoteReceive(boolean vote) throws RemoteException;

}