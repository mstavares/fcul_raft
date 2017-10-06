package utilities;

public class LogEntry {

    private String command;
    private int term;

    public LogEntry(String command, int term) {
        this.command = command;
        this.term = term;
    }

    public int getTerm() {
        return term;
    }
}
