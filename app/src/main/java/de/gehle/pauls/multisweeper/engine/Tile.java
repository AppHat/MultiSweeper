package de.gehle.pauls.multisweeper.engine;

public class Tile {

    private MinesweeperObserver observer;

    public enum TileState {
        Covered,        // not yet opened (only for shownState)
        Number,         // a no-mine field (default for realState)
        Flag,           // a field considered being a mine by user (only for shownState)
        Unknown,        // a field, where user doesn't know, what it is(only for shownState)
        Mine,           // a mined field
        BadFlag,        // a no-mine field covered with a flag by user (only for Game Over)
        ExplodedMine    // the mine, which the user stepped on (only for Game Over)
    }

    // that is, what tile really is
    private TileState realState;
    // this is what the user sees. The value is being determined by the value of realState
    // and whether the user has opened the tile yet
    private TileState shownState;

    private int nrSurroundingMines;

    // coordinates of this cell
    private int row, col;

    public Tile(MinesweeperObserver observer, int row, int col) {
        this.observer = observer;
        this.row = row;
        this.col = col;
        realState = TileState.Number;
        shownState = TileState.Covered;
        nrSurroundingMines = 0;
    }

    public TileState openTile() {
        if (shownState != TileState.Covered) {
            return shownState;
        }
        if (realState == TileState.Mine) {
            shownState = TileState.ExplodedMine;
        } else {
            shownState = realState;
        }
        observer.updateTile(row, col);
        return realState;
    }

    public void gameOver() {
        if (!isChangeable()) {
            // shownState is correct in this case
            return;
        }

        if (shownState == TileState.Flag &&
                realState != TileState.Mine) {
            shownState = TileState.BadFlag;
            observer.updateTile(row, col);
            return;
        }
        shownState = realState;
        observer.updateTile(row, col);
    }

    public void updateSurroundingMineCount() {
        ++nrSurroundingMines;
    }

    public int getNrSurroundingMines() {
        return nrSurroundingMines;
    }

    public boolean isEmpty() {
        return shownState == TileState.Number && nrSurroundingMines == 0;
    }

    public boolean isChangeable() {
        return shownState == TileState.Covered ||
                shownState == TileState.Unknown ||
                shownState == TileState.Flag;
    }

    public void setCovered() {
        if (isChangeable()) {
            shownState = TileState.Covered;
            observer.updateTile(row, col);
        }
    }

    public void setFlag() {
        if (isChangeable()) {
            shownState = TileState.Flag;
            observer.updateTile(row, col);
        }
    }

    public void setUnknown() {
        if (isChangeable()) {
            shownState = TileState.Unknown;
            observer.updateTile(row, col);
        }
    }

    /**
     * @return the state, which GUI will show to the user
     */
    public TileState getState() {
        return shownState;
    }

    public void putMine() {
        realState = TileState.Mine;
    }

    public boolean isMine() {
        return realState == TileState.Mine;
    }

}