package server;


import java.io.Serializable;

/** Este classe é um modelo para as configurações dos servidores */
public class NodeConnectionInfo implements Serializable {

    private String ipAddress;
    private int port;

    public NodeConnectionInfo(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "ip: " + ipAddress + " port: " + port;
    }

}
