package client;

import utilities.Debugger;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Esta classe liga-se aleatoriamente a um dos servidores carregados da lista.
 * Se esse servidor nao for o lider a resposta ao pedido deve conter o endereço
 * do lider.
 * Se o servidor for o lider, a respota deverá ser um código (ver secção 8 do paper)
 */

public class ClientMain {

    public static void main(String[] args) {
		Debugger.log("Client is starting");
       try {
			new Connection("192.168.1.16", 1099);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

}