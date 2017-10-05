package client;

import utilities.ThreadPool;
import utilities.XmlSerializer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        try {

            HashMap<String, String> map = XmlSerializer.fileToMap("Nodes.xml");
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            ClientInterface stub = (ClientInterface) registry.lookup("server");

            Scanner scan = new Scanner(System.in);
            String inputKey;

            for (;;) {
                System.out.print("\nCommand$ ");
                inputKey = scan.next();
                String teste = stub.request(inputKey);
                System.out.println("Cliente " + teste);
            }


        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

    }

}