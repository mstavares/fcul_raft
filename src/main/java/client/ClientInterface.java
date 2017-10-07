package client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

public interface ClientInterface extends Remote {
    String request(String command) throws RemoteException, ServerNotActiveException;
}
