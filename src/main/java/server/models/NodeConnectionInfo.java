package server.models;


import java.io.Serializable;

/** Este classe é um modelo para as configurações dos servidores */
public class NodeConnectionInfo implements Serializable {

    private int matchIndex, nextIndex;
    private String ipAddress;
    private int port;

    public NodeConnectionInfo(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        matchIndex = -1;
        nextIndex = 0;
    }

    public void incrementIndexes() {
        matchIndex++;
        nextIndex++;
    }

    public void decrementNextIndex() {
        matchIndex--;
        nextIndex--;
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public int getMatchIndex() {
        return matchIndex;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return ipAddress + port;
    }

    public void resetMatchIndex() {
        matchIndex = -1;
    }

    @Override
    public String toString() {
        return "ip: " + ipAddress + " port: " + port;
    }

}
