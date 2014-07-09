package de.gehle.pauls.multisweeper.engine;

import android.os.Handler;
import android.util.Log;

import java.util.Random;

/**
 * Game
 */
public class Game {

    public static final String KEY_DIFFICULTY = "de.gehle.pauls.multisweeper.difficulty";
    public static final String KEY_SAVEGAME = "de.gehle.pauls.multisweeper.save_game";

    public static final class Difficulty {
        public int rows;
        public int cols;
        public int mines;

        public Difficulty(int rows, int cols, int mines) {
            this.rows = rows;
            this.cols = cols;
            this.mines = mines;
        }
    }

    final static Difficulty[] difficulties = {
            new Difficulty(10, 10, 10),
            new Difficulty(16, 16, 40),
            new Difficulty(30, 16, 100)
    };

    private Difficulty difficulty;
    private int difficultyChosen;

    private MinesweeperObserver observer;

    private int nrOfPlayers;

    private Timer timer;
    private MineCounter mineCounter;
    private GameBoard gameBoard;
    private Score score;

    /**
     * Constructor
     *
     * @param observer   an object, which will be notified in case of changes
     * @param difficulty 0 for easy, 1 for medium, 2 for hard
     */
    public Game(MinesweeperObserver observer, int difficulty) {
        this.observer = observer;
        difficultyChosen = difficulty;
        this.difficulty = difficulties[difficulty];
        timer = new Timer(observer);
        mineCounter = new MineCounter(observer, this.difficulty.mines);
        gameBoard = new GameBoard(observer);
    }

    /**
     * Initialises a playable field with timer and mine-counter
     */
    public void start(int nrOfPlayers) {
        this.nrOfPlayers = nrOfPlayers;

        timer.stop();
        timer.reset();
        mineCounter.reset();

        // need to create a new score object, since the number of players might be different from
        // previous time
        this.score = new Score(nrOfPlayers);
        score.reset();
        gameBoard.setupMineField(difficulty.rows, difficulty.cols, difficulty.mines);
        setGameOver(false);
    }

    public int getTimeInSeconds() {
        return timer.getSecondsPassed();
    }

    public void endGame() {
        timer.stop();
        gameBoard.uncoverAll();
        setGameOver(true);
    }

    private void setGameOver(boolean gameOver) {
        observer.onGameStateChanged(gameOver);
    }

    public void playerMove(int playerId, int row, int col) {
        Log.d("GameEngine", "Player-" + String.valueOf(playerId) + " clicked on " +
                String.valueOf(row) + ":" + String.valueOf(col));

        init(row, col);

        int uncovered = gameBoard.uncover(row, col);
        score.inc(playerId, uncovered);

        boolean end = false;
        if (gameBoard.hitMine()) {
            score.reset(playerId);
            end = true;
        }
        end |= gameBoard.getCoveredFields() == 0;
        if (end) {
            endGame();
        }
    }

    public void playerMoveAlt(int row, int col) {
        init(row, col);
        Tile.TileState state = gameBoard.swapMarker(row, col);
        if (state == Tile.TileState.FLAG) {
            mineCounter.dec();
        } else if (state == Tile.TileState.UNKNOWN) {
            mineCounter.inc();
        }
    }

    public boolean isRunning() {
        //Timer is also false if game has ended
        return timer.hasStarted();
    }

    private void init(int row, int col) {
        if (!timer.hasStarted()) {
            timer.start();
            gameBoard.setupTiles(row, col);
            observer.onInitGameBoard(gameBoard.getTiles());
        }
    }

    public void forward(Tile[][] tiles) {
        gameBoard.setTiles(tiles);
    }

    public void continueGame(Tile[][] tiles, int timeInSeconds) {
        start(1);
        gameBoard.loadTiles(tiles);

        for (int row = 0; row < tiles.length; row++) {
            for (int col = 0; col < tiles[0].length; col++) {
                if (tiles[row][col].getState() == Tile.TileState.FLAG) {
                    mineCounter.dec();
                }

            }
        }

        timer.setSecondsPassed(timeInSeconds);
        timer.start();
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

    public Tile[][] getTiles() {
        return gameBoard.getTiles();
    }

    public int getScore(int playerId) {
        return score.getFinalScore(
                playerId,
                difficulty.cols * difficulty.rows,
                difficulty.mines,
                timer.getSecondsPassed());
    }

    public int getPlace(int playerId) {
        return score.getPlace(playerId);
    }

    public int getNrOfPlayers() {
        return nrOfPlayers;
    }

    public int getDifficulty() {
        return difficultyChosen;
    }
}

/**
 * GameBoard
 */
class GameBoard {
    private static final String TAG = "GameEngine";
    private MinesweeperObserver observer;
    private boolean hitMine;

    private Tile[][] tiles;

    private int totalRows;
    private int totalCols;
    private int totalMines;

    private int coveredFields;

    private enum Action {
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
        coveredFields = (rows * cols) - mines;

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

    public void loadTiles(Tile[][] tiles) {
        this.tiles = tiles;

        for (int row = 0; row < totalRows; row++) {
            for (int col = 0; col < totalCols; col++) {

                if (tiles[row][col].isMine()) {
                    handleSurroundingTiles(row, col, Action.UPDATE_SURROUNDING_MINE_COUNT);
                } else if (tiles[row][col].getState() == Tile.TileState.NUMBER) {
                    --coveredFields;
                }

                if (tiles[row][col].getState() == Tile.TileState.FLAG) {
                    handleSurroundingTiles(row, col, Action.INC_SURROUNDING_FLAGS_COUNT);
                }

            }
        }
    }

    public int uncover(int row, int col) {
        int coveredOld = coveredFields;
        Tile.TileState state = tiles[row][col].getState();

        if (!tiles[row][col].isUncoverable()) {
            return coveredOld - coveredFields;
        }

        if (state == Tile.TileState.COVERED || state == Tile.TileState.UNKNOWN) {
            --coveredFields;
        }

        // if the field is already uncovered and is a number
        // check for surrounding flags and mines.
        // if for every mine there is a flag, uncover all surrounding fields
        if (state == Tile.TileState.NUMBER) {
            if (tiles[row][col].getNrSurroundingMines() == tiles[row][col].getNrSurroundingFlags()) {
                tiles[row][col].setNumberUncovered();
                handleSurroundingTiles(row, col, Action.UNCOVER);
                return coveredOld - coveredFields;
            }
        }

        state = tiles[row][col].openTile();

        if (state == Tile.TileState.EXPLODED_MINE) {
            hitMine = true;
        }

        // if the tile has no surrounding mines, open surrounding tiles
        if (tiles[row][col].isEmpty()) {
            handleSurroundingTiles(row, col, Action.UNCOVER);
        }
        return coveredOld - coveredFields;
    }

    public Tile.TileState swapMarker(int row, int col) {
        Tile tile = getTile(row, col);
        Tile.TileState state = tile.getState();
        if (!tile.isChangeable() || tile.getState() == Tile.TileState.NUMBER) {
            // numbers can still be changeable, but you cannot put a mark on them
            return state;
        }

        if (tile.getState() == Tile.TileState.COVERED) {
            tile.setFlag();
            state = Tile.TileState.FLAG;
            handleSurroundingTiles(row, col, Action.INC_SURROUNDING_FLAGS_COUNT);
        } else if (tile.getState() == Tile.TileState.FLAG) {
            tile.setUnknown();
            state = Tile.TileState.UNKNOWN;
            handleSurroundingTiles(row, col, Action.DEC_SURROUNDING_FLAGS_COUNT);
        } else {
            tile.setCovered();
            state = Tile.TileState.COVERED;
        }
        return state;
    }

    private void handleSurroundingTiles(int row, int col, Action action) {
        for (int i = row - 1; i < row + 2; ++i) {
            if (i < 0 || i > totalRows - 1) {
                continue;
            }

            for (int j = col - 1; j < col + 2; ++j) {
                if ((i == row && j == col) || j < 0 || j > totalCols - 1) {
                    continue;
                }

                switch (action) {
                    case UNCOVER:
                        if (tiles[i][j].getState() == Tile.TileState.COVERED ||
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

    public boolean hitMine() {
        return hitMine;
    }

    public int getCoveredFields() {
        return coveredFields;
    }
}


class Score {

    private int nrOfPlayers;
    private Integer[] score;

    /**
     * Constructor for more than 1 players
     *
     * @param nrOfPlayers number of players to save score for
     */
    Score(int nrOfPlayers) {
        this.nrOfPlayers = nrOfPlayers;
        score = new Integer[nrOfPlayers];
        reset();
    }

//    public void inc(int playerId){
//        inc(playerId, 1);
//    }

    public void inc(int playerId, int score) {
        if (!inBounds(playerId)) {
            return;
        }
        this.score[playerId] += score;
    }

    public int getFinalScore(int playerId, int fieldSize, int mines, int time) {
        final int uncoveredFieldsFactor = 1;
        final int minesFactor = 2;
        final int timeFactor = 10;

        if (!inBounds(playerId)) {
            // TODO throw an exception
            return 0;
        }

        int minedensity = (mines / fieldSize) * 100;
        if (minedensity < 10 || minedensity > 90) {
            minedensity = 1;
        }
        int seconds = time > 0 ? time : 1;

        Log.d("Score", "Uncovered: " + score[playerId]);
        Log.d("Score", "Minedensity: " + minedensity);
        Log.d("Score", "Timer: " + seconds);

        //return (score[playerId] * minedensity * 100) / seconds;
        if (score[playerId] > 0) {
            return uncoveredFieldsFactor * score[playerId] +
                    minesFactor * minedensity +
                    timeFactor * seconds;
        } else {
            return 0;
        }
    }

    public int getPlace(int playerId) {
        if (!inBounds(playerId)) {
            return 0;
        }

        int place = 1;
        for (int i = 0; i < nrOfPlayers; ++i) {
            if (score[i] > score[playerId]) {
                ++place;
            }
        }
        return place;
    }

    public void reset() {
        for (int i = 0; i < nrOfPlayers; ++i) {
            reset(i);
        }
    }

    public void reset(int playerId) {
        if (!inBounds(playerId)) {
            return;
        }
        score[playerId] = 0;
    }

    private boolean inBounds(int i) {
        return i >= 0 && i < nrOfPlayers;
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

    private Runnable updateTimer = new Runnable() {
        public void run() {
            long currentMilliseconds = System.currentTimeMillis();
            ++secondsPassed;
            observer.updateTimer(secondsPassed);
            timer.postAtTime(this, currentMilliseconds);
            //run again in 1 second
            timer.postDelayed(updateTimer, 1000);
        }
    };

    public Timer(MinesweeperObserver observer) {
        this.observer = observer;
    }

    public void start() {
        timer.removeCallbacks(updateTimer);
        timer.postDelayed(updateTimer, 1000);
        timerStarted = true;
    }

    public boolean hasStarted() {
        return timerStarted;
    }

    public void stop() {
        timer.removeCallbacks(updateTimer);
        timerStarted = false;
    }

    public void reset() {
        secondsPassed = 0;
        observer.updateTimer(secondsPassed);
    }

    public int getSecondsPassed() {
        return secondsPassed;
    }

    public void setSecondsPassed(int seconds) {
        secondsPassed = seconds;
        observer.updateTimer(secondsPassed);
    }
}

/**
 * MineCounter
 */
class MineCounter {

    private MinesweeperObserver observer;
    private int totalMines;
    private int mineCounter;

    MineCounter(MinesweeperObserver observer, int totalMines) {
        this.observer = observer;
        this.totalMines = totalMines;
        mineCounter = totalMines;
    }

    public void reset() {
        mineCounter = totalMines;
        observer.updateMineCounter(mineCounter);
    }

    public void inc() {
        ++mineCounter;
        observer.updateMineCounter(mineCounter);
    }

    public void dec() {
        --mineCounter;
        observer.updateMineCounter(mineCounter);
        // Any actions for mineCounter < 0?
    }
}
