package de.gehle.pauls.multisweeper.engine;

import android.os.Handler;
import android.util.Log;

import java.util.Random;

/**
 * Game
 */
public class Game {

    public static final String KEY_DIFFICULTY = "de.gehle.pauls.multisweeper.difficulty";

    private final class Difficulty {
        public int rows;
        public int cols;
        public int mines;

        public Difficulty(int rows, int cols, int mines) {
            this.rows = rows;
            this.cols = cols;
            this.mines = mines;
        }
    }

    private Difficulty difficulty;

    private MinesweeperObserver observer;

    private int nrOfPlayers;

    private Timer timer;
    private MineCounter mineCounter;
    private GameBoard gameBoard;
    private Score score;

    /**
     * Constructor
     *
     * @param observer an object, which will be notified in case of changes
     * @param difficulty 0 for easy, 1 for medium, 2 for hard
     */
    public Game(MinesweeperObserver observer, int difficulty) {
        this.observer = observer;
        final Difficulty[] difficulties = {
                new Difficulty(10, 10, 22),
                new Difficulty(16, 16, 56),
                new Difficulty(30, 16, 105)
        };
        this.difficulty = difficulties[difficulty];
        timer = new Timer(observer);
        mineCounter = new MineCounter(observer, this.difficulty.mines);
        gameBoard = new GameBoard(observer);
    }

    /**
     * Initialises a playable field with timer and mine-counter
     */
    public void start(int nrOfPlayers) {
        timer.stop();
        timer.reset();
        mineCounter.reset();

        this.nrOfPlayers = nrOfPlayers;

        // need to create a new score object, since the number of players might be different from
        // previous time
        this.score = new Score(nrOfPlayers);
        score.reset();
        mineCounter.setMineCounter(difficulty.mines);
        gameBoard.setupMineField(difficulty.rows, difficulty.cols, difficulty.mines);
        setGameOver(false);
    }

    public void endGame() {
        timer.stop();
        gameBoard.uncoverAll();
        setGameOver(true);
    }

    private void setGameOver(boolean gameOver){
        observer.onGameStateChanged(gameOver);
    }

    public void playerMove(int playerId, int row, int col) {
        init(row, col);

        int uncovered = gameBoard.uncover(row, col);
        score.inc(playerId, uncovered);

        boolean end = false;
        if(gameBoard.hitMine()){
            score.reset(playerId);
            end = true;
        }
        end |= gameBoard.getUncoveredFields() == 0;
        if(end){
            endGame();
        }
    }

    public void playerMoveAlt(int row, int col) {
        init(row, col);
        Tile.TileState state = gameBoard.swapMarker(row, col);
        if(state == Tile.TileState.FLAG){
            mineCounter.dec();
        }else if(state == Tile.TileState.UNKNOWN){
            mineCounter.inc();
        }
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

    public int getRows() {
        return difficulty.rows;
    }

    public int getCols() {
        return difficulty.cols;
    }

    public Tile getTile(int row, int col) {
        return gameBoard.getTile(row, col);
    }

    public int getScore(int playerId) {
        return score.getFinalScore(
                playerId,
                difficulty.cols * difficulty.rows,
                difficulty.mines,
                timer.getSecondsPassed());
    }

    public int getPlace(int playerId){
        return score.getPlace(playerId);
    }

    public int getNrOfPlayers(){
        return nrOfPlayers;
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

    public int uncover(int row, int col) {
        int uncoveredOld = uncoveredFields;
        Tile.TileState state = tiles[row][col].getState();

        if(! tiles[row][col].isUncoverable()){
            return uncoveredOld - uncoveredFields;
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
                return uncoveredOld - uncoveredFields;
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
        return uncoveredOld - uncoveredFields;
    }

    public Tile.TileState swapMarker(int row, int col){
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

    private void handleSurroundingTiles(int row, int col, Action action){
        for(int i = row - 1; i < row + 2; ++i){
            if(i < 0 || i > totalRows - 1){
                continue;
            }

            for(int j = col - 1; j < col + 2; ++j){
                if((i == row && j == col) || j < 0 || j > totalCols - 1){
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


class Score {

    private int nrOfPlayers;
    private Integer [] score;

    /**
     * Constructor for more than 1 players
     * @param nrOfPlayers number of players to save score for
     */
    Score(int nrOfPlayers){
        this.nrOfPlayers = nrOfPlayers;
        score = new Integer[nrOfPlayers];
        reset();
    }

//    public void inc(int playerId){
//        inc(playerId, 1);
//    }

    public void inc(int playerId, int score){
        if(!inBounds(playerId)){
            return;
        }
        this.score[playerId] += score;
    }

    public int getFinalScore(int playerId, int fieldSize, int mines, int time){
        if(!inBounds(playerId)){
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

        return (score[playerId] * minedensity * 100) / seconds;
    }

    public int getPlace(int playerId) {
        if(!inBounds(playerId)){
            return 0;
        }

        int place = 1;
        for(int i = 0; i < nrOfPlayers; ++i){
            if(score[i] > score[playerId]){
                ++place;
            }
        }
        return place;
    }

//    public boolean isMaxScore(int playerId) {
//        if(!inBounds(playerId)){
//            return false;
//        }
//
//        int max = 0;
//        for(int  i = 0; i < nrOfPlayers; ++i){
//            max = score[i] > max ? score[i] : max;
//        }
//        return score[playerId] == max;
//    }

    public void reset(){
        for(int i = 0; i < nrOfPlayers; ++i){
            reset(i);
        }
    }

    public void reset(int playerId){
        if(!inBounds(playerId)){
            return;
        }
        score[playerId] = 0;
    }

    private boolean inBounds(int i){
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
            observer.updateTimer(currentTime);
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

//    public String getCurrentTime() {
//        return currentTime;
//    }

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
        observer.updateTimer(currentTime);
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

    MineCounter(MinesweeperObserver observer, int totalMines) {
        this.observer = observer;
        this.totalMines = totalMines;
        setMineCounter(totalMines);
    }

    public void reset() {
        setMineCounter(totalMines);
    }

    public void setMineCounter(int counter) {
        mineCounter = counter;
        String mineCountText;
        String strCounter = Integer.toString(mineCounter);
        if (mineCounter < 10) {
            mineCountText = "00" + strCounter;
        } else if (mineCounter < 100) {
            mineCountText = "0" + strCounter;
        } else {
            mineCountText = strCounter;
        }
        observer.updateMineCounter(mineCountText);
    }

    public void inc() {
        setMineCounter(mineCounter + 1);
    }

    public void dec() {
        setMineCounter(mineCounter - 1);
        // Any actions for mineCounter < 0?
    }
}
