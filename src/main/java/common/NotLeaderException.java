package common;

public class NotLeaderException extends Exception {

    public NotLeaderException(String leaderInfo) {
        super(leaderInfo);
    }
}
