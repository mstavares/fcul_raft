package utilities;

import java.io.Serializable;

public class LogEntry implements Serializable {

    private String command;
    private int votes = 1;
    private int term;

    public LogEntry(String command, int term) {
        this.command = command;
        this.term = term;
    }

    public int getTerm() {
        return term;
    }

    public int getVotes() {
        return votes;
    }

    public void incrementVotes() {
        votes++;
    }

    @Override
    public String toString() {
        return "Comando: " + command + " term: " + term;
    }
}
