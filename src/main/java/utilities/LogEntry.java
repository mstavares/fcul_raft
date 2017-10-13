package utilities;

import java.io.Serializable;

public class LogEntry implements Serializable {

    private String command;
    private int term;

    public LogEntry(String command, int term) {
        this.command = command;
        this.term = term;
    }

    public int getTerm() {
        return term;
    }

    @Override
    public String toString() {
        return "Comando: " + command + " term: " + term;
    }
}
