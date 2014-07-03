package de.gehle.pauls.multisweeper.engine;

public class Tile {

    private MinesweeperObserver observer;

    public enum TileState {
        COVERED,         // not yet opened (only for shownState)
        NUMBER,          // a no-mine field (default for realState)
        FLAG,            // a field considered being a mine by user (only for shownState)
        UNKNOWN,         // a field, where user doesn't know, what it is(only for shownState)
        MINE,            // a mined field
        BAD_FLAG,        // a no-mine field covered with a flag by user (only for Game Over)
        EXPLODED_MINE    // the mine, which the user stepped on (only for Game Over)
    }

    // that is, what tile really is
    private TileState realState;
    // this is what the user sees. The value is being determined by the value of realState
    // and whether the user has opened the tile yet
    private TileState shownState;

    private int nrSurroundingMines;
    private int nrSurroundingFlags;

    private boolean numberUncovered;

    // coordinates of this cell
    private int row, col;

    public Tile(MinesweeperObserver observer, int row, int col) {
        this.observer = observer;
        this.row = row;
        this.col = col;
        realState = TileState.NUMBER;
        shownState = TileState.COVERED;
        nrSurroundingMines = 0;
        nrSurroundingFlags = 0;
        numberUncovered = false;
    }

    public TileState openTile() {
        if (shownState != TileState.COVERED && shownState != TileState.UNKNOWN) {
            return shownState;
        }
        if (realState == TileState.MINE) {
            shownState = TileState.EXPLODED_MINE;
        } else {
            shownState = realState;
        }
        observer.updateTile(row, col);
        return shownState;
    }

    public void gameOver() {
        if (shownState == TileState.FLAG &&
            realState != TileState.MINE) {
            shownState = TileState.BAD_FLAG;
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
        return shownState == TileState.NUMBER && nrSurroundingMines == 0;
    }

    public void incNrSurroundingFlags() { ++nrSurroundingFlags; }

    public void decNrSurroundingFlags() { --nrSurroundingFlags; }

    public int getNrSurroundingFlags() { return nrSurroundingFlags; }

    public void setNumberUncovered() { numberUncovered = true; }

    public boolean isUncoverable(){
        return shownState == TileState.COVERED ||
               shownState == TileState.UNKNOWN ||
               (shownState == TileState.NUMBER && !numberUncovered);
    }

    public boolean isChangeable() {
        return isUncoverable() ||
               shownState == TileState.FLAG;
    }

    public void setCovered() {
        if (isChangeable()) {
            shownState = TileState.COVERED;
            observer.updateTile(row, col);
        }
    }

    public void setFlag() {
        if (isChangeable()) {
            shownState = TileState.FLAG;
            observer.updateTile(row, col);
        }
    }

    public void setUnknown() {
        if (isChangeable()) {
            shownState = TileState.UNKNOWN;
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
        realState = TileState.MINE;
    }

    public boolean isMine() {
        return realState == TileState.MINE;
    }

}