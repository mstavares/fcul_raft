package client;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import utilities.Debugger;
import utilities.XmlSerializer;
import common.ElectingException;
import common.NotLeaderException;
import common.OperationType;
import server.interfaces.ClientInterface;

public class Connection{

	private ClientInterface stub;
	private Registry registry;
	private String serverIp;
	
	
	/**
	 * Creates a new connection to a random server
	 */
	public Connection() throws RemoteException, NotBoundException {
		initialize();
	}
	
	
	public void initialize() throws RemoteException, NotBoundException {
		Server server = getRandomServer();
		serverIp = server.getIp();
		registry = LocateRegistry.getRegistry( server.getIp(), server.getPort() );
        stub = (ClientInterface) registry.lookup("raft");
	}
	
	
	public String sendRequest(OperationType op, String key, String oldValue, String newValue){
		try {
			return stub.request(op, key, oldValue, newValue);
			
		} catch (NotLeaderException e) {
			Debugger.log("The answering server is not the leader");
			Debugger.log("Trying to contact: " + e.getMessage());
			String[] arr = e.getMessage().split(":");
			redirectToLeader(arr[0], Integer.parseInt(arr[1]));
			return sendRequest(op, key, oldValue, newValue);
			
		} catch (ElectingException e) {
			Debugger.log("An Election is happening. Waiting 15 seconds before retrying.");
			try {
				Thread.sleep(15000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			return sendRequest(op, key, oldValue, newValue);
			
		} catch (RemoteException | ServerNotActiveException e) {
			Debugger.log("Error getting answer from the server.");
			Debugger.log("Trying to connect to a new server.");
			try {
				initialize();
			} catch (RemoteException | NotBoundException e1) {
				Debugger.log("Failed to connect to a new server.");
				Debugger.log("Waiting 15 seconds before retrying.");
				try {
					Thread.sleep(15000);
				} catch (InterruptedException e2) {
					e1.printStackTrace();
				}
			}
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
	
	
    private Server getRandomServer(){
        HashMap<String, String> map = XmlSerializer.readConfig("Nodes.xml");
        ArrayList<String> keys = new ArrayList<String>( map.keySet() );
        
        if (serverIp != null && keys.size() > 1)
        	keys.remove(serverIp);
        
        Random rd = new Random();
        String key = keys.get(rd.nextInt(keys.size()));
        return new Server(key, map.get(key) );
    }
    
    
    private static class Server{
    	String ip;
    	int port;
    	
    	public Server(String ip, String port){
    		this.ip = ip;
    		this.port = Integer.parseInt(port);
    	}
    	
    	public String getIp(){
    		return ip;
    	}
    	
    	public int getPort(){
    		return port;
    	}
    }
	
}
