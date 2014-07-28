package de.gehle.pauls.multisweeper.engine;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Game
 */
public class Game {

    public static final String KEY_DIFFICULTY = "de.gehle.pauls.multisweeper.difficulty";
    public static final String KEY_SAVEGAME = "de.gehle.pauls.multisweeper.save_game";

    private static final String TAG = "GameClass";

    private MinesweeperObserver observer;

    private Timer timer;
    private Counter mineCounter;
    private GameBoard gameBoard;
    private Score score;

    private int nrOfPlayers;
    private int currentPlayer;

    public enum GameState {
        PREPARED,
        RUNNING,
        GAME_WON,
        GAME_LOST
    }

    private GameState gameState;

    /**
     * Game constructor for Minesweeper
     */
    public Game(MinesweeperObserver observer, int rows, int cols, int mines) {
        this(observer, rows, cols, mines, 1);
    }

    /**
     * Game constructor for Minesweeper
     * <p/>
     * Initialises a playable field with timer and mine-counter
     *
     * @param observer   An object, which will be notified in case of changes
     * @param rows       Rows the gamefield should have
     * @param cols       Cols the gamefield should have
     * @param mines      Amount of mines placed on the gamefield
     */
    public Game(MinesweeperObserver observer, int rows, int cols, int mines, int nrOfPlayers) {
        this.observer = observer;
        this.nrOfPlayers = nrOfPlayers;
        timer = new Timer(observer);
        mineCounter = new CounterDown(observer, mines);
        gameBoard = new GameBoard(this, rows, cols, mines);
        score = new Score(nrOfPlayers);
        gameState = GameState.PREPARED;
    }

    /**
     * Constructor for loading save-games
     */
    public Game(MinesweeperObserver observer, byte[] data) {
        this.observer = observer;
        //Save games only for singleplayer
        this.nrOfPlayers = 1;
        loadFromJson(new String(data));
        score = new Score(nrOfPlayers);
        setGameState(GameState.RUNNING);
    }

    /**
     * Called everytime a player clicks on a tileButton
     *
     * @param playerId PlayerId for multiplayer. In Singleplayer we have the id = 0
     * @param row      Row of the clicked tileButton
     * @param col      Col of the clicked tileButton
     */
    public void playerMove(int playerId, int row, int col) {
        Log.d("GameEngine", "Player-" + String.valueOf(playerId) + " clicked on " +
                String.valueOf(row) + ":" + String.valueOf(col));

        currentPlayer = playerId;

        //Start game on first click
        if (gameState == GameState.PREPARED) {
            startGame(row, col);
            setGameState(GameState.RUNNING);
        }

        /**
         * If mine uncovered gameboard will set gamestate to game_over
         */
        int uncovered = gameBoard.uncover(row, col);
        score.inc(playerId, uncovered);
    }

    /**
     * Called everytime a player does a long-click on a tileButton
     *
     * @param row Row of the clicked tileButton
     * @param col Col of the clicked tileButton
     */
    public void playerMoveAlt(int row, int col) {
        Tile.TileState state = gameBoard.swapMarker(row, col);
        if (state == Tile.TileState.FLAG) {
            mineCounter.dec();
        } else if (state == Tile.TileState.UNKNOWN) {
            mineCounter.inc();
        }
    }

    /**
     * Start a game with init click
     *
     * @param row Row of init click
     * @param col Col of init click
     */
    private void startGame(int row, int col) {
        timer.start();
        gameBoard.setupTiles(row, col);
    }

    public void endGame(GameState state) {
        if (state == GameState.GAME_LOST) {
            score.reset(currentPlayer);
        }
        timer.stop();
        gameBoard.uncoverAll();
        setGameState(state);
    }

    /**
     * @param nrOfPlayers New number of player, cause for example a player in multiplayer could be disconnected
     */
    public void reset(int nrOfPlayers) {
        this.nrOfPlayers = nrOfPlayers;
        timer.stop();
        timer.reset();
        mineCounter.reset();
        gameBoard.reset();
        setGameState(GameState.PREPARED);

        /**
         * Need to create a new score object,
         * since the number of players might be different from previous time.
         * E.g. disconnected players in multiplayer game
         */
        score = new Score(nrOfPlayers);
    }

    public int getRows() {
        return gameBoard.getRows();
    }

    public int getCols() {
        return gameBoard.getCols();
    }

    public int getMines() {
        return gameBoard.getMines();
    }

    public byte[] exportGameBoard() {
        return gameBoard.toBytes();
    }

    public int getNrOfPlayers() {
        return nrOfPlayers;
    }

    public Tile getTile(int row, int col) {
        return gameBoard.getTile(row, col);
    }

    public boolean isRunning() {
        //Timer is also false if game has ended
        return timer.hasStarted();
    }

    void setGameState(GameState state) {
        gameState = state;
        observer.onGameStateChanged(gameState);
    }

    public int getScore(int playerId) {
        return score.getFinalScore(
                playerId,
                getCols() * getRows(),
                getMines(),
                timer.getSecondsPassed());
    }

    public int getPlace(int playerId) {
        return score.getPlace(playerId);
    }

    /**
     * Called every time a tile state is changed
     *
     * @param row Row of changed tile
     * @param col Col of changed tile
     */
    public void onTileStateChanged(int row, int col) {
        observer.updateTile(row, col);
    }

    /**
     * ============================================================
     * For save games
     * ============================================================
     */

    /**
     * Serializes this game to an array of bytes.
     */
    public byte[] toBytes() {
        return toString().getBytes();
    }

    /**
     * Serializes this game to a JSON string.
     */
    @Override
    public String toString() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("gameBoard", gameBoard.toJson());
            obj.put("timeInSeconds", timer.getSecondsPassed());
            return obj.toString();
        } catch (JSONException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error converting game to JSON.", ex);
        }
    }

    /**
     * Replaces this game's content with the content loaded from the given JSON string.
     */
    public void loadFromJson(String json) {
        if (json == null || json.trim().equals("")) return;

        try {
            JSONObject obj = new JSONObject(json);

            gameBoard = GameBoard.fromJson(this, obj.getJSONObject("gameBoard").toString());

            //Recalculate counter value
            mineCounter = new CounterDown(observer, gameBoard.getMines());
            Tile[][] tiles = gameBoard.getTiles();
            for (Tile[] rowTiles : tiles) {
                for (Tile tile : rowTiles) {
                    if (tile.getState() == Tile.TileState.FLAG) {
                        mineCounter.dec();
                    }
                }
            }

            timer = new Timer(observer);
            timer.setSecondsPassed(obj.getInt("timeInSeconds"));
            timer.start();

        } catch (JSONException ex) {
            ex.printStackTrace();
            Log.e(TAG, "Save data has a syntax error: " + json, ex);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Save data has an invalid number in it: " + json, ex);
        }
    }

    /**
     * ============================================================
     * For multiplayer
     * ============================================================
     */

    public void setGameBoard(GameBoard gameBoard) {
        this.gameBoard = gameBoard;
        if(gameState == GameState.PREPARED) {
            setGameState(GameState.RUNNING);
        }
    }
}
