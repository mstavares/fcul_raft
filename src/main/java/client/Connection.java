package client;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;

import utilities.Debugger;
import common.NotLeaderException;
import common.OperationType;
import server.interfaces.ClientInterface;

public class Connection{

	private ClientInterface stub;
	private Registry registry;
	
	public Connection(String host, int port) throws RemoteException, NotBoundException {
		registry = LocateRegistry.getRegistry(host, port);
        stub = (ClientInterface) registry.lookup("raft");
	}
	
	
	public String sendRequest(OperationType op, String key, String oldValue, String newValue) throws RemoteException, ServerNotActiveException {
		try {
			return stub.request(op, key, oldValue, newValue);
		} catch (NotLeaderException e) {
			Debugger.log("O node que respondeu nao e lider");
			Debugger.log("Vou reenviar o pedido para: " + e.getMessage());
			String[] arr = e.getMessage().split(":");
			redirectToLeader(arr[0], Integer.parseInt(arr[1]));
			return sendRequest(op, key, oldValue, newValue);
		}
	}
	
	
	private void redirectToLeader(String host, Integer port) {
		try {
			registry = LocateRegistry.getRegistry(host, port);
			stub = (ClientInterface) registry.lookup("raft");
		} catch (RemoteException e) {
			Debugger.log("Erro a alterar o registry");
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}
	
}
