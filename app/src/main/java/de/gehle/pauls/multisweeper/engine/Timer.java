package de.gehle.pauls.multisweeper.engine;

import android.os.Handler;

/**
 * A general Timer
 * <p/>
 * At the moment just used for minesweeper, but could be used also for other apps
 *
 * @author Andi
 */
public class Timer {

    public interface TimerObserver {
        public void updateTimer(int secondsPassed);
    }

    private TimerObserver observer;
    private Handler timer = new Handler();
    private int secondsPassed = 0;
    private boolean timerStarted = false;

    public Timer(TimerObserver observer) {
        this.observer = observer;
    }

    public Timer(TimerObserver observer, int secondsAlreadyPassed) {
        this.observer = observer;
        secondsPassed = secondsAlreadyPassed;
    }

    private Runnable updateTimer = new Runnable() {
        public void run() {
            long currentMilliseconds = System.currentTimeMillis();
            setSecondsPassed(secondsPassed + 1);
            timer.postAtTime(this, currentMilliseconds);
            //run again in 1 second
            timer.postDelayed(updateTimer, 1000);
        }
    };

    public void start() {
        timer.removeCallbacks(updateTimer);
        timer.postDelayed(updateTimer, 1000);
        timerStarted = true;
    }

    public boolean hasStarted() {
        return timerStarted;
    }

    public void stop() {
        timer.removeCallbacks(updateTimer);
        timerStarted = false;
    }

    public void reset() {
        setSecondsPassed(0);
    }

    public int getSecondsPassed() {
        return secondsPassed;
    }

    public void setSecondsPassed(int seconds) {
        secondsPassed = seconds;
        observer.updateTimer(secondsPassed);
    }
}
