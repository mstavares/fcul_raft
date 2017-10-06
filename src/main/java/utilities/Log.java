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
        return lastLogIndex >= getLastLogIndex() && lastLogTerm > getLastLogTerm();
    }

    private int getLastLogIndex() {
        return entries.size() - 1;
    }

    private int getLastLogTerm() {
        return entries.get(getLastLogIndex()).getTerm();
    }

}


