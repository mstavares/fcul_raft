package client;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import utilities.Debugger;
import common.NotLeaderException;
import common.OnTimeListener;
import common.OperationType;
import common.TimeManager;
import server.interfaces.ClientInterface;

public class Connection implements OnTimeListener {

	private ClientInterface stub;
	private Registry registry;
	private TimeManager requestTimer;
	//private static final List<Request> values = Collections.unmodifiableList(Arrays.asList(Request.values()));
	private static final Random rnd = new Random();
	
	public Connection(String host, int port) throws RemoteException, NotBoundException {
		registry = LocateRegistry.getRegistry(host, port);
        stub = (ClientInterface) registry.lookup("raft");
        // requestTimer = new TimeManager(this);
	}
	
	public void timeout(TimeManager timeManager) {
		Debugger.log("Timeout para enviar um pedido");
		// Request request = getRequest();
		// sendRequest(request);
		timeManager.resetTimer();
	}

	private void sendRequest(OperationType op, String key, String value) {
		Debugger.log("Vou enviar o pedido: " + op);
		try {
			String response = stub.request(op, key, value);
			Debugger.log("Recebi a resposta: " + response);
		} catch (RemoteException e) {
			Debugger.log("RemoteException");
			e.printStackTrace();
		} catch (ServerNotActiveException e) {
			Debugger.log("ServerNotActiveException");
			e.printStackTrace();
		} catch (NotLeaderException e) {
			Debugger.log("O node que respondeu nao e lider");
			Debugger.log("Vou reenviar o pedido para: " + e.getMessage());
			String[] arr = e.getMessage().split(":");
			redirectToLeader(arr[0], Integer.parseInt(arr[1]));
			sendRequest(op, key, value);
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

	/*
	private Request getRequest() {
		return values.get(rnd.nextInt(values.size()));
	}
	*/

}
