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

    public LogEntry getLastEntry() {
        return entries.get(getLastLogIndex());
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
        if(isEmpty())
            return 0;
        else
            return entries.size() - 1;
    }

    public int getPrevLogTerm() {
        if(entries.size() == 1)
            return 0;
        else
            return entries.get(getLastLogIndex() - 1).getTerm();
    }

    /** Este método devolve o term do último log adicionado */
    public int getLastLogTerm() {
        if(isEmpty())
            return 0;
        else
            return entries.get(getLastLogIndex()).getTerm();
    }

    /** Este métdo devolve o termo de uma dada posição */
    public int getTermOfIndex(int position) {
        if(isEmpty())
            return 0;
        else
            return entries.get(position).getTerm();
    }

    public void appendLog(LogEntry newEntry) {
        Debugger.log("appendLog: " + newEntry.toString());
        if(isEmpty()) {
            entries.add(newEntry);
        } else {
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (entries.get(i).getTerm() != newEntry.getTerm()) {
                    entries.remove(i);
                } else {
                    entries.add(newEntry);
                    break;
                }
            }
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


