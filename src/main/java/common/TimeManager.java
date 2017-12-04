package common;


import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class TimeManager {

    private OnTimeListener timeListener;
    private SnapshotOnTimeListener snapshotTimeListener;
    private Random random = new Random();
    private Timer timer;
    private boolean heartbeat;
    private Task task;
    private int minTimeout, maxTimeout;
    private boolean snapshot;

    public TimeManager(OnTimeListener timeListener) {
        this.timeListener = timeListener;
        this.heartbeat = false;
        minTimeout = 10 * 1000;
        maxTimeout = 15 * 1000;
        resetTimer();
    }

    public TimeManager(OnTimeListener timeListener, boolean heartbeat) {
        this.timeListener = timeListener;
        this.heartbeat = heartbeat;
        minTimeout = 2 * 1000;
        maxTimeout = 3 * 1000;
        resetTimer();
    }

    public TimeManager(OnTimeListener timeListener, int time) {
        this.timeListener = timeListener;
        this.heartbeat = false;
        minTimeout = time;
        maxTimeout = time;
        resetTimer();
    }
    
    public TimeManager(SnapshotOnTimeListener timeListener, boolean snapshot, int time) {
    	this.snapshotTimeListener = timeListener;
    	this.snapshot = snapshot;
    	minTimeout = 1000 * time;
        maxTimeout = 1000 * time;
        resetTimer();
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
        	if(snapshot)
        		snapshotTimeListener.snapshot(TimeManager.this);
        	else
        		timeListener.timeout(TimeManager.this);
            resetTimer();
        }
    }

}
