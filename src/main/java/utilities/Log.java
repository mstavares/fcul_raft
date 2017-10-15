package utilities;

import java.io.Serializable;
import java.util.ArrayList;

/** Esta classe encapsula o registo de logs dos servidores.
 * Usa o Delegate Design Pattern
 * */
public class Log implements Serializable {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private ArrayList<LogEntry> entries = new ArrayList<LogEntry>();

    public void add(LogEntry logEntry) {
        entries.add(logEntry);
    }

    public void set(int position, LogEntry logEntry) {
        entries.set(position, logEntry);
    }

    public LogEntry getEntry(int position) {
        return entries.get(position);
    }

    /** Este método verifica se os meus logs estão outdated, se estiverem não posso ser lider */
    public boolean areMyLogsOutdated(int lastLogIndex, int lastLogTerm) {
        return lastLogIndex >= getLastLogIndex() && lastLogTerm >= getLastLogTerm();
    }

    public boolean isEmpty() {
        return entries.size() == 0;
    }

    /** Este método envia o indice do último log adicionado */
    public int getLastLogIndex() {
        if(entries.size() == 0)
            return 0;
        else
            return entries.size() - 1;
    }

    /** Este método devolve o term do último log adicionado */
    public int getLastLogTerm() {
        int lastLogIndex = getLastLogIndex();
        if(lastLogIndex == 0)
            return lastLogIndex;
        else
            return entries.get(getLastLogIndex()).getTerm();
    }

    /** Este métdo devolve o termo de uma dada posição */
    public int getTermOfIndex(int position) {
        if(entries.size() == 0)
            return 0;
        else
            return entries.get(position).getTerm();
    }

    public ArrayList<LogEntry> getEntries() {
        return entries;
    }

    public ArrayList<Integer> appendLogs(Log newEntries) {
        Debugger.log("appendLogs: " + newEntries.toString());
        ArrayList<Integer> updates = new ArrayList<Integer>();
        for(int i = 0; i < newEntries.getEntries().size() - 1; i++) {
            Debugger.log("ENTREI 1");
            if(!entries.isEmpty() && entries.get(i).getTerm() != newEntries.getEntry(i).getTerm()) {
                Debugger.log("ENTREI 2");
                cleanLogsSince(i);
                appendLogsSince(updates, newEntries.getEntries(), i);
                break;
            } else {
                entries.add(newEntries.getEntry(i));
                updates.add(i);
            }
        }
        return updates;
    }

    private void cleanLogsSince(int position) {
        for(int i = position; i < entries.size() - 1; i++) {
            entries.remove(i);
        }
    }

    private void appendLogsSince(ArrayList<Integer> updates, ArrayList<LogEntry> newEntries, int position) {
        for(int i = position; i < newEntries.size() - 1; i++) {
            entries.add(newEntries.get(i));
            updates.add(i);
        }
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


