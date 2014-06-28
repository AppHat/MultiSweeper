package de.gehle.pauls.multisweeper.engine;

import android.os.Handler;

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

    public class Difficulty {
        public int rows;
        public int cols;
        public int mines;

        public Difficulty(int rows, int cols, int mines) {
            this.rows = rows;
            this.cols = cols;
            this.mines = mines;
        }
    }

    private Difficulty[] difficulties = {
            new Difficulty(10, 10, 10),
            new Difficulty(16, 16, 50),
            new Difficulty(30, 16, 144)
    };
    private Difficulty difficulty;

    private Timer timer;
    private MineCounter mineCounter;
    private GameBoard gameBoard;
    MinesweeperObserver observer;

    private boolean started = false;

    public Game(MinesweeperObserver observer, DifficultyId difficulty) {
        this.observer = observer;
        this.difficulty = difficulties[difficulty.ordinal()];
        gameState = GameState.GAMESTATE_PLAYING;
    }

    public void start() {
        timer = new Timer(observer);
        mineCounter = new MineCounter(observer, difficulty.mines);
        mineCounter.setMineCounter(difficulty.mines);
        gameBoard = new GameBoard(observer);
        gameBoard.setupMineField(difficulty.rows, difficulty.cols, difficulty.mines);
    }

    public void playerMove(int row, int col) {
        init(row, col);
        Tile.TileState state = gameBoard.uncover(row, col);
        if (state == Tile.TileState.Mine) {
            gameState = GameState.GAMESTATE_LOST;
            endGame();
        } else {
            if (checkWin()) {
                gameState = GameState.GAMESTATE_WON;
                endGame();
            }
        }
    }

    private boolean checkWin() {
        return gameBoard.getUncoveredFields() == 0;
    }

    public void playerMoveAlt(int row, int col) {
        init(row, col);

        Tile tile = gameBoard.getTile(row, col);
        if (!tile.isChangeable()) {
            return;
        }

        if (tile.getState() == Tile.TileState.Covered) {
            tile.setFlag();
        } else if (tile.getState() == Tile.TileState.Flag) {
            tile.setUnknown();
        } else {
            tile.setCovered();
        }

    }

    private void init(int row, int col) {
        if (!started) {
            started = true;
            gameBoard.setupTiles(row, col);
        }
        if (!timer.hasStarted()) {
            timer.start();
        }
    }

    public int getRows() {
        return difficulty.rows;
    }

    public int getCols() {
        return difficulty.cols;
    }

    public int getTotalMines() {
        return difficulty.mines;
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

    public void endGame() {
        timer.stop();
        gameBoard.uncoverAll();
    }

    public void reset() {
        timer.stop();
        timer.reset();
        mineCounter.reset();
        gameBoard.setupMineField(
                difficulty.rows,
                difficulty.cols,
                difficulty.mines
        );
        gameState = GameState.GAMESTATE_PLAYING;
        started = false;
    }
}

/**
 * GameBoard
 */
class GameBoard {
    MinesweeperObserver observer;

    private Tile[][] tiles;

    private int totalRows;
    private int totalCols;
    private int totalMines;

    private int uncoveredFields;

    public GameBoard(MinesweeperObserver observer) {
        this.observer = observer;
    }

    public void setupMineField(int rows, int cols, int mines) {
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
            int startRow = mineRow - 1;
            int startCol = mineCol - 1;
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

            for (int j = startRow; j < startRow + checkRows; j++) {
                for (int k = startCol; k < startCol + checkCols; k++) {
                    if (!tiles[j][k].isMine()) {
                        tiles[j][k].updateSurroundingMineCount();
                    }
                }
            }

        }
    }

    public Tile.TileState uncover(int row, int col) {
        if (tiles[row][col].isChangeable()) {
            --uncoveredFields;
        }
        Tile.TileState state = tiles[row][col].openTile();

        if (!tiles[row][col].isEmpty()) {
            return state;
        }

        /**
         * Uncover surrounding tiles if noSurroundingMines == 0
         */
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
                if (tiles[i][j].getState() == Tile.TileState.Covered) {
                    uncover(i, j);
                }
            }
        }
        return state;
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
        secondsPassed = 0;
    }

    public void reset() {
        currentTime = "000";
        observer.updateTimer();
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
