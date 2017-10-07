package utilities;

import java.util.ArrayList;

public class Log {

    private ArrayList<LogEntry> entries = new ArrayList<LogEntry>();

    public void add(LogEntry logEntry) {
        entries.add(logEntry);
    }

    public void set(int position, LogEntry logEntry) {
        entries.set(position, logEntry);
    }

    public boolean areMyLogsOutdated(int lastLogIndex, int lastLogTerm) {
        return lastLogIndex >= getLastLogIndex() && lastLogTerm >= getLastLogTerm();
    }

    public int getLastLogIndex() {
        if(entries.size() == 0)
            return 0;
        else
            return entries.size() - 1;
    }

    public int getLastLogTerm() {
        int lastLogIndex = getLastLogIndex();
        if(lastLogIndex == 0)
            return lastLogIndex;
        else
            return entries.get(getLastLogIndex()).getTerm();
    }

}


