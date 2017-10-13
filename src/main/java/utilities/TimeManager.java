package utilities;


import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class TimeManager {

    private OnTimeListener timeListener;
    private Random random = new Random();
    private Timer timer;
    private boolean heartbeat;
    private Task task;
    private int minTimeout, maxTimeout;

    public TimeManager(OnTimeListener timeListener) {
        this.timeListener = timeListener;
        this.heartbeat = false;
        minTimeout = 6 * 1000;
        maxTimeout = 8 * 1000;
        resetTimer();
    }

    public TimeManager(OnTimeListener timeListener, boolean heartbeat) {
        this(timeListener);
        this.heartbeat = heartbeat;
        minTimeout = 1 * 1000;
        maxTimeout = 2 * 1000;
    }

    private int generateNewTimeout() {
        return random.nextInt(maxTimeout - minTimeout + 1) + minTimeout;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    public void resetTimer() {
        stopTimer();
        task = new Task();
        timer = new Timer();
        timer.schedule(task, generateNewTimeout());
    }

    public void stopTimer() {
        if(timer != null)
            timer.cancel();
    }

    private class Task extends TimerTask {

        public void run() {
            timeListener.timeout(TimeManager.this);
            resetTimer();
        }
    }

}
