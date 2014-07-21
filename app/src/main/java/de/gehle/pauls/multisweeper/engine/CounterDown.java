package de.gehle.pauls.multisweeper.engine;

/**
 * Counter which counts down
 *
 * @author Andi
 */
public class CounterDown extends Counter {

    CounterDown(CounterObserver observer, int max) {
        super(observer, max);
    }

    CounterDown(CounterObserver observer, int max, int min) {
        super(observer, max, min);
        counter = max;
    }

    @Override
    public void reset() {
        setCounter(max);
    }
}
