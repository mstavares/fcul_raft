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
			con = new Connection("192.168.1.14", 1090); //TODO get this from file
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
		
		while (!finished){
			newValue = null;
			oldValue = null;
			key = null;
			sendRequest = false;
			
			showMenu();
			op = sc.nextInt();

			switch (op){
			case 1:
				operation = OperationType.PUT;
				System.out.println("Insert the key to put:");
				key = sc.nextLine();
				System.out.println("Insert the value:");
				newValue = sc.nextLine();
				break;
			case 2:
				operation = OperationType.GET;
				System.out.println("Insert the key to get:");
				key = sc.nextLine();
				break;
			case 3:
				operation = OperationType.DEL;
				System.out.println("Insert the key to delete:");
				key = sc.nextLine();
				break;
			case 4:
				operation = OperationType.LIST;
				break;
			case 5:
				operation = OperationType.CAS;
				System.out.println("Insert the key:");
				key = sc.nextLine();
				System.out.println("Insert the old value:");
				oldValue = sc.nextLine();
				System.out.println("Insert the new value:");
				newValue = sc.nextLine();
				break;
			case 0:
				finished = true;
				break;
			default:
				System.out.println("Invalid option.");
			}
			
			if (sendRequest){
				try {
					System.out.print("Server Answer: ");
					System.out.println(con.sendRequest(operation, key, oldValue, newValue) );
				} catch (RemoteException | ServerNotActiveException e) {
					System.out.println("Error getting answer from the server.");
				}
			}
		}
		
		sc.close();
    }
    
    
    public static void showMenu(){
		System.out.println("Choose an action:");
		System.out.println("1- PUT");
		System.out.println("2- GET");
		System.out.println("3- DEL");
		System.out.println("4- LIST");
		System.out.println("5- CAS");
		System.out.println("0- Exit.");
    }

}