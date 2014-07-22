package de.gehle.pauls.multisweeper.engine;

/**
 * Counter which counts down
 *
 * @author Andi
 */
public class CounterDown extends Counter {

    CounterDown(CounterObserver observer, int max) {
        super(observer, max);
        reset();
    }

    CounterDown(CounterObserver observer, int max, int min) {
        super(observer, max, min);
        reset();
    }

    @Override
    public void reset() {
        setCounter(max);
    }
}
