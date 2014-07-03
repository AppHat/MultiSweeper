package de.gehle.pauls.multisweeper.engine;

import android.os.Handler;
import android.util.Log;

import java.util.Random;

/**
 * Game
 */
public class Game {

    public static final String KEY_DIFFICULTY = "de.gehle.pauls.multisweeper.difficulty";

    public enum GameState {
        GAMESTATE_PLAYING,
        GAMESTATE_WON,
        GAMESTATE_LOST
    }

    private GameState gameState;

    public enum DifficultyId {
        DIFFICULTY_EASY,
        DIFFICULTY_MEDIUM,
        DIFFICULTY_HARD
    }

    public final class Difficulty {
        public int rows;
        public int cols;
        public int mines;

        public Difficulty(int rows, int cols, int mines) {
            this.rows = rows;
            this.cols = cols;
            this.mines = mines;
        }
    }

    private final Difficulty[] difficulties = {
            new Difficulty(10, 10, 22),
            new Difficulty(16, 16, 56),
            new Difficulty(30, 16, 105)
    };
    private Difficulty difficulty;

    private Timer timer;
    private MineCounter mineCounter;
    private GameBoard gameBoard;
    private MinesweeperObserver observer;

    private int score = 0;
    private boolean started = false;

    public Game(MinesweeperObserver observer, DifficultyId difficulty) {
        this.observer = observer;
        this.difficulty = difficulties[difficulty.ordinal()];
        timer = new Timer(observer);
        mineCounter = new MineCounter(observer, this.difficulty.mines);
        gameBoard = new GameBoard(observer);
        setGameState(GameState.GAMESTATE_PLAYING);
    }

    public void start() {
        score = 0;
        mineCounter.setMineCounter(difficulty.mines);
        gameBoard.setupMineField(difficulty.rows, difficulty.cols, difficulty.mines);
    }

    public void playerMove(int row, int col) {
        init(row, col);

        gameBoard.uncover(row, col);
        if (checkLose()) {
            endGame();
            setGameState(GameState.GAMESTATE_LOST);
        } else if (checkWin()) {
            endGame();
            setGameState(GameState.GAMESTATE_WON);
        }
    }

    private boolean checkLose() { return gameBoard.hitMine(); }

    private boolean checkWin() {
        return gameBoard.getUncoveredFields() == 0;
    }

    public void playerMoveAlt(int row, int col) {
        init(row, col);

        gameBoard.swapMarker(row, col);
    }

    private void init(int row, int col) {
        if (!started) {
            started = true;
            gameBoard.setupTiles(row, col);
            observer.onInitGameBoard(gameBoard.getTiles());
        }
        if (!timer.hasStarted()) {
            timer.start();
        }
    }

    public void forward(Tile[][] tiles) {
        gameBoard.setTiles(tiles);
        started = true;
    }

    public int getRows() {
        return difficulty.rows;
    }

    public int getCols() {
        return difficulty.cols;
    }

    public Tile getTile(int row, int col) {
        return gameBoard.getTile(row, col);
    }

    /**
     * For output on GUI
     *
     * @return
     */
    public String getRemainingMines() {
        return mineCounter.getMineCountText();
    }

    public String getTime() {
        return timer.getCurrentTime();
    }

    public GameState getGameState() {
        return gameState;
    }

    private void setGameState(GameState state) {
        gameState = state;
        //Calculate score
        if (getGameState() == GameState.GAMESTATE_WON) {
            int fieldsize = difficulty.rows * difficulty.cols;
            int minedensity = (difficulty.mines / fieldsize) * 100;
            if (minedensity < 10 || minedensity > 90) {
                minedensity = 1;
            }
            int seconds = timer.getSecondsPassed() > 0 ? timer.getSecondsPassed() : 1;

            Log.d("Score", "Fieldsize: " + fieldsize);
            Log.d("Score", "Minedensity: " + minedensity);
            Log.d("Score", "Timer: " + seconds);

            score = (fieldsize * 100 / seconds) * minedensity;
        }
        observer.onGameStateChanged(gameState);
    }

    public void endGame() {
        timer.stop();
        gameBoard.uncoverAll();
    }

    public int getScore() {
        return score;
    }

    public void reset() {
        timer.stop();
        timer.reset();
        mineCounter.reset();
        setGameState(GameState.GAMESTATE_PLAYING);
        started = false;
    }

}

/**
 * GameBoard
 */
class GameBoard {
    private MinesweeperObserver observer;
    private boolean hitMine;

    private Tile[][] tiles;

    private int totalRows;
    private int totalCols;
    private int totalMines;

    private int uncoveredFields;

    private enum Action{
        UNCOVER,
        UPDATE_SURROUNDING_MINE_COUNT,
        INC_SURROUNDING_FLAGS_COUNT,
        DEC_SURROUNDING_FLAGS_COUNT
    }

    public GameBoard(MinesweeperObserver observer) {
        this.observer = observer;
    }

    public void setupMineField(int rows, int cols, int mines) {
        hitMine = false;
        totalRows = rows;
        totalCols = cols;
        totalMines = mines;
        uncoveredFields = (rows * cols) - mines;

        tiles = new Tile[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                tiles[row][col] = new Tile(observer, row, col);
            }
        }
    }

    public void setupTiles(int row, int col) {
        Random random = new Random();
        int mineRow;
        int mineCol;

        for (int i = 0; i < totalMines; i++) {
            mineRow = random.nextInt(totalRows);
            mineCol = random.nextInt(totalCols);

            if (mineRow == row && mineCol == col ||
                    tiles[mineRow][mineCol].isMine()) {
                i--;
                continue;
            }

            //Else: Plant a new mine
            tiles[mineRow][mineCol].putMine();

            /**
             * Refresh surrounding mines counters
             */
            handleSurroundingTiles(mineRow, mineCol, Action.UPDATE_SURROUNDING_MINE_COUNT);
        }
    }

    public Tile.TileState uncover(int row, int col) {
        Tile.TileState state = tiles[row][col].getState();

        if(! tiles[row][col].isUncoverable()){
            return state;
        }

        if (state == Tile.TileState.COVERED || state == Tile.TileState.UNKNOWN) {
            --uncoveredFields;
        }

        // if the field is already uncovered and is a number
        // check for surrounding flags and mines.
        // if for every mine there is a flag, uncover all surrounding fields
        if(state == Tile.TileState.NUMBER){
            tiles[row][col].setNumberUncovered();
            if(tiles[row][col].getNrSurroundingMines() == tiles[row][col].getNrSurroundingFlags()){
                handleSurroundingTiles(row, col, Action.UNCOVER);
                return state;
            }
        }

        state = tiles[row][col].openTile();

        if(state == Tile.TileState.EXPLODED_MINE){
            hitMine = true;
        }

        // if the tile has no surrounding mines, open surrounding tiles
        if (tiles[row][col].isEmpty()) {
            handleSurroundingTiles(row, col, Action.UNCOVER);
        }
        return state;
    }

    public void swapMarker(int row, int col){
        Tile tile = getTile(row, col);
        if (!tile.isChangeable()) {
            return;
        }

        if (tile.getState() == Tile.TileState.COVERED) {
            tile.setFlag();
            handleSurroundingTiles(row, col, Action.INC_SURROUNDING_FLAGS_COUNT);
            //mineCounter.dec();
        } else if (tile.getState() == Tile.TileState.FLAG) {
            tile.setUnknown();
            handleSurroundingTiles(row, col, Action.DEC_SURROUNDING_FLAGS_COUNT);
            //mineCounter.inc();
        } else {
            tile.setCovered();
        }
    }

    private void handleSurroundingTiles(int row, int col, Action action){
        int startRow = row - 1;
        int startCol = col - 1;
        int checkRows = 3;
        int checkCols = 3;

        if (startRow < 0) {
            startRow = 0;
            checkRows = 2;
        } else if (startRow + 3 > totalRows) {
            checkRows = 2;
        }

        if (startCol < 0) {
            startCol = 0;
            checkCols = 2;
        } else if (startCol + 3 > totalCols) {
            checkCols = 2;
        }

        for (int i = startRow; i < startRow + checkRows; i++) {
            for (int j = startCol; j < startCol + checkCols; j++) {
                if(i == row && j == col){
                    continue;
                }
                switch(action){
                    case UNCOVER:
                        if(tiles[i][j].getState() == Tile.TileState.COVERED ||
                           tiles[i][j].getState() == Tile.TileState.UNKNOWN) {
                            uncover(i, j);
                        }
                        break;
                    case UPDATE_SURROUNDING_MINE_COUNT:
                        tiles[i][j].updateSurroundingMineCount();
                        break;
                    case INC_SURROUNDING_FLAGS_COUNT:
                        tiles[i][j].incNrSurroundingFlags();
                        break;
                    case DEC_SURROUNDING_FLAGS_COUNT:
                        tiles[i][j].decNrSurroundingFlags();
                        break;
                }
            }
        }
    }

    public void uncoverAll() {
        for (int i = 0; i < totalRows; i++) {
            for (int j = 0; j < totalCols; j++) {
                tiles[i][j].gameOver();
            }
        }
    }

    public Tile getTile(int row, int col) {
        return tiles[row][col];
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public void setTiles(Tile[][] tiles) {
        this.tiles = tiles;
    }

    public boolean hitMine() { return hitMine; }

    public int getUncoveredFields() {
        return uncoveredFields;
    }
}

/**
 * Timer
 */
class Timer {

    private MinesweeperObserver observer;
    private Handler timer = new Handler();
    private int secondsPassed = 0;
    private boolean timerStarted = false;
    private String currentTime;
    private Runnable updateTimer = new Runnable() {
        public void run() {
            long currentMilliseconds = System.currentTimeMillis();
            ++secondsPassed;
            String curTime = Integer.toString(secondsPassed);
            //update the text view
            if (secondsPassed < 10) {
                currentTime = "00" + curTime;
            } else if (secondsPassed < 100) {
                currentTime = "0" + curTime;
            } else {
                currentTime = curTime;
            }
            observer.updateTimer();
            timer.postAtTime(this, currentMilliseconds);
            //run again in 1 second
            timer.postDelayed(updateTimer, 1000);
        }
    };

    public Timer(MinesweeperObserver observer) {
        this.observer = observer;
    }

    public void start() {
        currentTime = "000";
        if (secondsPassed == 0) {
            timer.removeCallbacks(updateTimer);
            timer.postDelayed(updateTimer, 1000);
        }
        timerStarted = true;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public boolean hasStarted() {
        return timerStarted;
    }

    public void stop() {
        timer.removeCallbacks(updateTimer);
        timerStarted = false;
    }

    public void reset() {
        currentTime = "000";
        secondsPassed = 0;
        observer.updateTimer();
    }

    public int getSecondsPassed() {
        return secondsPassed;
    }
}

/**
 * MineCounter
 */
class MineCounter {

    private MinesweeperObserver observer;
    private int totalMines;
    private int mineCounter;
    private String mineCountText;

    MineCounter(MinesweeperObserver observer, int totalMines) {
        this.observer = observer;
        this.totalMines = totalMines;
    }

    public void set(int value) {
        totalMines = value;
        setMineCounter(totalMines);
    }

    public void reset() {
        setMineCounter(totalMines);
    }

    public void setMineCounter(int counter) {
        mineCounter = counter;
        String strCounter = Integer.toString(mineCounter);
        if (mineCounter < 10) {
            mineCountText = "00" + strCounter;
        } else if (mineCounter < 100) {
            mineCountText = "0" + strCounter;
        } else {
            mineCountText = strCounter;
        }
        observer.updateMineCounter();
    }

    public void inc() {
        setMineCounter(mineCounter + 1);
    }

    public void dec() {
        setMineCounter(mineCounter - 1);
        // Any actions for mineCounter < 0?
    }

    public String getMineCountText() {
        return mineCountText;
    }

    public int getValue() {
        return mineCounter;
    }
}
