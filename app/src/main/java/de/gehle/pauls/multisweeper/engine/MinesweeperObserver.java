package de.gehle.pauls.multisweeper.engine;

/**
 * @author Dima
 */
public interface MinesweeperObserver extends Timer.TimerObserver, Counter.CounterObserver {

    public void updateTile(int row, int col);

    public void onGameStateChanged(Game.GameState newState);
}
