package de.gehle.pauls.multisweeper.engine;

/**
 * General counter
 *
 * @author Andi
 */
public class Counter {

    public interface CounterObserver {
        public void updateCounter(int newValue);
    }

    protected CounterObserver observer;
    protected int min;
    protected int max;
    protected int counter;

    Counter(CounterObserver observer, int max) {
        this(observer, max, 0);
    }

    Counter(CounterObserver observer, int max, int min) {
        this.observer = observer;
        this.max = max;
        this.min = min;
        counter = min;
    }

    public void reset() {
        setCounter(min);
    }

    public void inc() {
        setCounter(counter + 1);
    }

    public void dec() {
        setCounter(counter - 1);
    }

    public void setCounter(int value) {
        if (value >= min && value <= max) {
            counter = value;
            observer.updateCounter(value);
        }
    }
}

