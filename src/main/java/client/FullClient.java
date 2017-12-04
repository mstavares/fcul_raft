package client;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.Scanner;

import common.OperationType;

/**
 * A full Raft Client
 */

public class FullClient {

    public static void main(String[] args) {
    	
    	System.out.println("********************");
		System.out.println("* Full Raft Client *");
		System.out.println("********************");
		
		Connection con;
		
		try {
			con = new Connection();
		} catch (RemoteException | NotBoundException e) {
			System.out.println("Error connecting to the server.");
			e.printStackTrace();
			return;
		}
		
		boolean finished = false;
		int op = 0;
		Scanner sc = new Scanner(System.in);
		String key, newValue, oldValue;
		OperationType operation = OperationType.PUT;
		boolean sendRequest;
		
		while (!finished){ //TODO is there a need for multiple threads?
			newValue = null;
			oldValue = null;
			key = null;
			sendRequest = true;
			
			showMenu();
			op = sc.nextInt();

			switch (op){
			case 1:
				operation = OperationType.PUT;
				System.out.println("Insert the key to put:");
				key = sc.next();
				System.out.println("Insert the value:");
				newValue = sc.next();
				break;
			case 2:
				operation = OperationType.GET;
				System.out.println("Insert the key to get:");
				key = sc.next();
				break;
			case 3:
				operation = OperationType.DEL;
				System.out.println("Insert the key to delete:");
				key = sc.next();
				break;
			case 4:
				operation = OperationType.LIST;
				break;
			case 5:
				operation = OperationType.CAS;
				System.out.println("Insert the key:");
				key = sc.next();
				System.out.println("Insert the old value:");
				oldValue = sc.next();
				System.out.println("Insert the new value:");
				newValue = sc.next();
				break;
			case 0:
				finished = true;
				sendRequest = false;
				break;
			default:
				sendRequest = false;
				System.out.println("Invalid option.");
			}
			
			if (sendRequest){
				try {
					System.out.println("Server Answer: ");
					System.out.println(con.sendRequest(operation, key, oldValue, newValue) );
				} catch (RemoteException | ServerNotActiveException e) {
					System.out.println("Error getting answer from the server.");
					System.out.println("Trying to connect to a new server.");
					try {
						con = new Connection();
					} catch (RemoteException | NotBoundException e1) {
						System.out.println("Failed to connect to a new server.");
					}
				}
			}
		}
		
		sc.close();
    }
    
    
    private static void showMenu(){
		System.out.println("\nChoose an action:");
		System.out.println("1- PUT");
		System.out.println("2- GET");
		System.out.println("3- DEL");
		System.out.println("4- LIST");
		System.out.println("5- CAS");
		System.out.println("0- Exit.");
    }

}