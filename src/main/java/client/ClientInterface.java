package client;

import common.NotLeaderException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

/** Interface RMI do cliente -> servidor, servidor -> cliente */
public interface ClientInterface extends Remote {

    String request(String command) throws RemoteException, ServerNotActiveException, NotLeaderException;
}
