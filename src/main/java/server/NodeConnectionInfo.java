package server;


import java.io.Serializable;

public class NodeConnectionInfo implements Serializable {

    private String ipAddress;
    private int port;

    NodeConnectionInfo(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

}
