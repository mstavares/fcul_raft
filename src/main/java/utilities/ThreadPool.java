package utilities;

import server.NodeConnectionInfo;
import server.ServerInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {

    private ExecutorService threadPool;

    public ThreadPool(int numberOfThreads) {
        threadPool = Executors.newFixedThreadPool(numberOfThreads);
    }

    public void execute(List<NodeConnectionInfo> nodesConnectionInfo) {
        for (NodeConnectionInfo nodeConnectionInfo : nodesConnectionInfo) {
            threadPool.execute(new Worker(nodeConnectionInfo));
        }
    }

    private class Worker implements Runnable {

        private NodeConnectionInfo nodeConnectionInfo;

        Worker(NodeConnectionInfo nodeConnectionInfo) {
            this.nodeConnectionInfo = nodeConnectionInfo;
        }

        public void run() {
            try {
                Registry registry = LocateRegistry.getRegistry(nodeConnectionInfo.getIpAddress(), nodeConnectionInfo.getPort());
                ServerInterface stub = (ServerInterface) registry.lookup("server");
                stub.appendEntries(1, nodeConnectionInfo, 1, 1, new ArrayList<String>(), 1);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }
    }
}
