package server.interfaces;

import common.ElectingException;
import common.NotLeaderException;
import common.OperationType;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

/** Interface RMI do cliente -> servidor, servidor -> cliente */
public interface ClientInterface extends Remote {

    String request(OperationType op, String key, String oldValue, String newValue) throws RemoteException, ServerNotActiveException, NotLeaderException;
}
