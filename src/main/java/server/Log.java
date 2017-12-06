package server;

import server.models.LogEntry;
import server.models.NodeConnectionInfo;
import utilities.Debugger;

import java.io.Serializable;
import java.sql.Timestamp;
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

    void appendLog(LogEntry newEntry, int i) {
        Debugger.log("appendLog: " + newEntry.toString());
        try {
            entries.get(i + 1);
            Debugger.log("vou reescrever");
            entries.set(i + 1, newEntry);
        } catch (IndexOutOfBoundsException e) {
            Debugger.log("vou adicionar");
            entries.add(newEntry);
        }

        //entries.add(newEntry);
        //entries.set(i + 1, newEntry);

        /*
        Debugger.log("appendLog: " + newEntry.toString());
        if(!duplicated(newEntry))
            entries.add(newEntry);
        else
            Debugger.log("--> DUPLICADO <--");
        */

    }

    public boolean duplicated(LogEntry newEntry) {
        for(LogEntry logEntry : entries) {
            if(logEntry.getTimestamp().equals(newEntry.getTimestamp())) {
                Debugger.log("1-> Antigo: " + logEntry.getTimestamp() + " novo: " + newEntry.getTimestamp());
                Debugger.log(newEntry.toString());
                return true;
            }
        }
        Debugger.log("2-> " + newEntry.toString());
        return false;
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


