package utilities;

import java.util.ArrayList;

/** Esta classe encapsula o registo de logs dos servidores.
 * Usa o Delegate Design Pattern
 * */
public class Log {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private ArrayList<LogEntry> entries = new ArrayList<LogEntry>();

    public void add(LogEntry logEntry) {
        entries.add(logEntry);
    }

    public void set(int position, LogEntry logEntry) {
        entries.set(position, logEntry);
    }

    public LogEntry getLogEntry(int position) {
        return entries.get(position);
    }

    /** Este método verifica se os meus logs estão outdated, se estiverem não posso ser lider */
    public boolean areMyLogsOutdated(int lastLogIndex, int lastLogTerm) {
        return lastLogIndex >= getLastLogIndex() && lastLogTerm >= getLastLogTerm();
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
        return entries.get(position).getTerm();
    }

    public ArrayList<LogEntry> getEntries() {
        return entries;
    }

    public void appendLogs(Log newEntries) {
        for(int i = 0; i < newEntries.getEntries().size() - 1; i++) {
            if(entries.get(i).getTerm() != newEntries.getLogEntry(i).getTerm()) {
                cleanLogsSince(i);
                appendLogsSince(newEntries.getEntries(), i);
                break;
            }
        }
    }

    private void cleanLogsSince(int position) {
        for(int i = position; i < entries.size() - 1; i++) {
            entries.remove(i);
        }
    }

    private void appendLogsSince(ArrayList<LogEntry> newEntries, int position) {
        for(int i = position; i < newEntries.size() - 1; i++) {
            entries.add(newEntries.get(i));
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


