package de.gehle.pauls.multisweeper.engine;

/**
 * Created by dima on 25.06.2014.
 */
public interface MinesweeperObserver {

    public void updateTile(int row, int col);

    public void updateTimer(int secondsPassed);

    public void updateMineCounter(int mineCounter);

    public void onGameStateChanged(boolean newState);

    public void onInitGameBoard(Tile[][] tiles);
}
