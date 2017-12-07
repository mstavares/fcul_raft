package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

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
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Insert the interval between request in seconds:");
		int requestInterval = sc.nextInt() * 1000;
		
		try {
			boolean sendRequests = true;
			Connection con = new Connection();
			int count = 0;
			
			while(sendRequests){
				//TODO is there a need for different threads for each request?
				sendRequest(con, OperationType.PUT, "Key" + count, null, "value" + count);
				count++;
				
				try {
					Thread.sleep(requestInterval);
				} catch (InterruptedException e) {
					System.out.println("Sleep interrupted");
					e.printStackTrace();
				}
			}
		} catch (RemoteException | NotBoundException e) {
			System.out.println("Error Connecting to the server.");
			e.printStackTrace();
		}
		
		sc.close();
	}
	
	
	private static void sendRequest(Connection con, OperationType op, String key, String oldValue, String newValue) {
		System.out.println("Vou enviar o pedido: " + op + " " + key);
		String response = con.sendRequest(op, key, oldValue, newValue);
		System.out.println("Recebi a resposta: " + response);
	}
	
}
