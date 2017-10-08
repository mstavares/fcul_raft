package client;

import utilities.XmlSerializer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Esta classe liga-se aleatoriamente a um dos servidores carregados da lista.
 * Se esse servidor nao for o lider a resposta ao pedido deve conter o endereço
 * do lider.
 * Se o servidor for o lider, a respota deverá ser um código (ver secção 8 do paper)
 */

public class Client {

    private static final String SERVICE_NAME = "raft";

    public static void main(String[] args) {
        try {

            HashMap<String, String> map = XmlSerializer.readConfig("Nodes.xml");
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            ClientInterface stub = (ClientInterface) registry.lookup(SERVICE_NAME);

            Scanner scan = new Scanner(System.in);
            String inputKey;

            for (;;) {

                /** Este bocado de codigo envia os pedidos para o servidor */
                System.out.print("\nCommand$ ");
                inputKey = scan.next();
                String teste = stub.request(inputKey);
                System.out.println("Cliente " + teste);
            }


        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }

    }

}