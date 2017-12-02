package client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

public interface ReplyInterface extends Remote {

	void reply(String result);
}
