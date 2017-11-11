package server.interfaces;

import server.models.NodeConnectionInfo;

public interface ConnectionInterface {

    void updateLeaderId(NodeConnectionInfo leaderId);
}
