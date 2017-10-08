package utilities;

import java.util.ArrayList;

/** Esta classe encapsula o registo de logs dos servidores.
 * Usa o Delegate Design Pattern
 * */
public class Log {

    private ArrayList<LogEntry> entries = new ArrayList<LogEntry>();

    public void add(LogEntry logEntry) {
        entries.add(logEntry);
    }

    public void set(int position, LogEntry logEntry) {
        entries.set(position, logEntry);
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

}


