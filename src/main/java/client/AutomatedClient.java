package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

import common.OperationType;

/**
 * An automated client that sends put requests
 * each 2 seconds to the server.
 */
public class AutomatedClient{
	
	public static void main(String[] args) {
		
		System.out.println("***************************");
		System.out.println("* Simple Automated Client *");
		System.out.println("***************************");
		
		try {
			boolean sendRequests = false;
			Connection con = new Connection("192.168.1.14", 1090); //TODO get this from file
			int count = 0;
			
			while(sendRequests){
				//TODO is there a need for different threads for each request?
				sendRequest(con, OperationType.PUT, "Key" + count, null, "value" + count);
				count++;
				
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					System.out.println("Sleep interrupted");
					e.printStackTrace();
				}
			}
		} catch (RemoteException | NotBoundException e) {
			System.out.println("Error Connecting to the server.");
			e.printStackTrace();
		}
	}
	
	
	private static void sendRequest(Connection con, OperationType op, String key, String oldValue, String newValue) {
		System.out.println("Vou enviar o pedido: " + op + " " + key);
		try {
			String response = con.sendRequest(op, key, oldValue, newValue);
			System.out.println("Recebi a resposta: " + response);
		} catch (RemoteException e) {
			System.out.println("RemoteException");
			e.printStackTrace();
		} catch (ServerNotActiveException e) {
			System.out.println("ServerNotActiveException");
			e.printStackTrace();
		}
	}
	
}
