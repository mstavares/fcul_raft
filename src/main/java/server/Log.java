package server;

import server.models.LogEntry;
import server.models.NodeConnectionInfo;
import utilities.Debugger;

import java.io.Serializable;
import java.util.ArrayList;

/** Esta classe encapsula o registo de logs dos servidores.
 * Usa o Delegate Design Pattern
 * */
public class Log implements Serializable {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private ArrayList<LogEntry> entries = new ArrayList<LogEntry>();

    void add(LogEntry logEntry) {
        entries.add(logEntry);
    }

    /** Este método verifica se os meus logs estão outdated, se estiverem não posso ser lider */
    boolean areMyLogsOutdated(int lastLogIndex, int lastLogTerm) {
        return lastLogIndex >= getLastLogIndex() && lastLogTerm >= getLastLogTerm();
    }

    LogEntry getLogEntryOfIndex(int index) {
        return entries.get(index);
    }

    boolean isEmpty() {
        return entries.size() == 0;
    }

    /** Este método envia o indice do último log adicionado */
    int getLastLogIndex() {
        return isEmpty() ? 0 : entries.size() - 1;
    }

    /** Este método devolve o term do último log adicionado */
    int getLastLogTerm() {
        return isEmpty() ? 0 : entries.get(getLastLogIndex()).getTerm();
    }

    /** Este métdo devolve o termo de uma dada posição */
    int getTermOfIndex(int position) {
        return (isEmpty() || position == -1) ? 0 : entries.get(position).getTerm();
    }

    int getNumberOfReplicatedNodes(int index) {
        return entries.get(index).getNumberOfReplicatedNodes();
    }

    void addReplicatedNode(NodeConnectionInfo replicatedNode, int index) {
        entries.get(index).addReplicatedNode(replicatedNode);
    }

    LogEntry fetchNextEntryToSend(NodeConnectionInfo node) {
        if(!isEmpty()) {
            if(entries.size() >= node.getNextIndex()) {
                try {
                    if (entries.get(node.getNextIndex()) != null && getLastLogIndex() != node.getMatchIndex()) {
                        return entries.get(node.getNextIndex());
                    }
                } catch (IndexOutOfBoundsException e) {
                    /** Vou devolver null porque não há log entries recentes para enviar */
                }
            }
        }
        return null;
    }

    int fetchPrevLogIndexToSend(NodeConnectionInfo node) {
        return entries.isEmpty() ? getLastLogIndex() : node.getNextIndex() - 1;
    }

    int fetchPrevLogTermToSend(NodeConnectionInfo node) {
        return getTermOfIndex(fetchPrevLogIndexToSend(node));
    }

    void appendLog(LogEntry newEntry) {
        Debugger.log("appendLog: " + newEntry.toString());
        entries.add(newEntry);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(LogEntry entry : entries) {
            sb.append(LINE_SEPARATOR).append(entry.toString());
        }
        return sb.toString();
    }

}


