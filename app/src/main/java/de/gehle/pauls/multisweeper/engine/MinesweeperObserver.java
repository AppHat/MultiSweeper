package de.gehle.pauls.multisweeper.engine;

/**
 * Created by dima on 25.06.2014.
 */
public interface MinesweeperObserver {

    public void updateTile(int row, int col);

    public void updateTimer();

    public void updateMineCounter();
}
