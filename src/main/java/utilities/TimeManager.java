package utilities;


import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class TimeManager {

    private OnTimeListener timeListener;
    private Random random = new Random();
    private Timer timer = new Timer();
    private Task task;
    private int minTimeout, maxTimeout;

    public TimeManager(OnTimeListener timeListener) {
        this.timeListener = timeListener;
        minTimeout = 3 * 1000;
        maxTimeout = 5 * 1000;
        resetTimer();
    }

    public TimeManager(OnTimeListener timeListener, int minTimeout, int maxTimeout) {
        this(timeListener);
        this.minTimeout = minTimeout;
        this.maxTimeout = maxTimeout;
    }

    private int generateNewTimeout() {
        return random.nextInt(random.nextInt(maxTimeout - minTimeout + 1) + minTimeout);
    }

    private void resetTimer() {
        if(task != null)
            task.cancel();
        task = new Task();
        timer.schedule(task, generateNewTimeout());
    }

    private class Task extends TimerTask {

        public void run() {
            timeListener.timeout();
            resetTimer();
        }
    }

}
