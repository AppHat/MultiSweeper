package de.gehle.pauls.multisweeper.engine;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Random;

/**
 * GameBoard
 */
public class GameBoard {
    private static final String TAG = "GameBoard";

    private Game game;

    private int rows;
    private int cols;
    private int mines;

    private Tile[][] tiles;

    private int nrOfCoveredFields;
    private boolean hitMine;

    /**
     * Action for travers surrounding tiles
     * (Some simple version of a visitor-pattern)
     */
    private enum Action {
        UNCOVER,
        UPDATE_SURROUNDING_MINE_COUNT,
        INC_SURROUNDING_FLAGS_COUNT,
        DEC_SURROUNDING_FLAGS_COUNT
    }

    /**
     * GameBoard constructor
     */
    public GameBoard(Game game, int rows, int cols, int mines) {
        this.game = game;
        this.rows = rows;
        this.cols = cols;
        this.mines = (mines > rows * cols) ? rows * cols : mines;

        tiles = new Tile[rows][cols];
        reset();
    }

    public void reset() {
        hitMine = false;
        nrOfCoveredFields = rows * cols - mines;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                tiles[row][col] = new Tile();
            }
        }
    }

    /**
     * Setup mines on minefield
     * <p/>
     * Cause the first click should not lead to a gameover/ explosion of a mine,
     * we plant the mines after the first click!
     *
     * @param clickedRow Row of first clicked tile
     * @param clickedCol Col of first clicked tile
     */
    public void setupTiles(int clickedRow, int clickedCol) {
        Random random = new Random();
        int mineRow;
        int mineCol;

        for (int i = 0; i < mines; i++) {
            mineRow = random.nextInt(rows);
            mineCol = random.nextInt(cols);

            if (mineRow == clickedRow && mineCol == clickedCol || tiles[mineRow][mineCol].isMine()) {
                i--;
                continue;
            }

            //Else: Plant a new mine
            tiles[mineRow][mineCol].putMine();

            //And refresh surrounding-mines counters
            traversSurroundingTilesOf(mineRow, mineCol, Action.UPDATE_SURROUNDING_MINE_COUNT);
        }
    }

    /**
     * Uncovers a tile and if possible all surrounding tiles with number equals zero
     * or if already uncovered but not all surrounding ones check flags = mines and open if so
     *
     * @param row Row of tile to uncover
     * @param col Col of tile to uncover
     * @return Amount of field which were uncovered (If not uncoverable = 0)
     */
    public int uncover(int row, int col) {
        if (!tiles[row][col].isUncoverable() && !tiles[row][col].canUncoverSurroundings()) {
            return 0;
        }

        int oldNrOfCoveredFields = nrOfCoveredFields;

        Tile.TileState state = tiles[row][col].getState();
        if ((state == Tile.TileState.COVERED || state == Tile.TileState.UNKNOWN) && !tiles[row][col].isMine()) {
            --nrOfCoveredFields;
        }

        /**
         * Uncover surrounding tiles if flags equals mines
         *
         * If the field is already uncovered and a number: Check for surrounding flags and mines.
         * If for every mine there is a flag, uncover all surrounding fields.
         */
        else if (state == Tile.TileState.NUMBER && tiles[row][col].getNrSurroundingMines() == tiles[row][col].getNrSurroundingFlags()) {
            tiles[row][col].setNumberUncovered();
            traversSurroundingTilesOf(row, col, Action.UNCOVER);
        }


        state = tiles[row][col].openTile();
        game.onTileStateChanged(row, col);


        if (state == Tile.TileState.EXPLODED_MINE) {
            hitMine = true;
        }
        //If the tile has no surrounding mines, open surrounding tiles
        else if (tiles[row][col].isEmpty()) {
            traversSurroundingTilesOf(row, col, Action.UNCOVER);
        }

        return oldNrOfCoveredFields - nrOfCoveredFields;
    }

    /**
     * Swaps marker
     * <p/>
     * Swaps marker from COVERED -> FLAG -> QUESTION_MARK -> COVERED
     *
     * @param playerId PlayerId for multiplayer. In Singleplayer we have the id = 0
     * @param row      Row of the tile to swap
     * @param col      Col of the tile to swap
     * @return New state of the tile
     */
    public Tile.TileState swapMarker(int playerId, int row, int col) {
        Tile tile = getTile(row, col);
        Tile.TileState state = tile.getState();

        if (!tile.isSwappable()) {
            return state;
        }

        if (state == Tile.TileState.COVERED) {
            tile.setFlag(playerId);
            traversSurroundingTilesOf(row, col, Action.INC_SURROUNDING_FLAGS_COUNT);
        } else if (state == Tile.TileState.FLAG) {
            tile.setUnknown(playerId);
            traversSurroundingTilesOf(row, col, Action.DEC_SURROUNDING_FLAGS_COUNT);
        } else {
            tile.setCovered();
        }

        game.onTileStateChanged(row, col);
        return tile.getState();
    }

    public boolean hitMine() {
        return hitMine;
    }

    public boolean allUncovered() {
        return nrOfCoveredFields == 0;
    }

    /**
     * Travers the surrounding tiles of a given tile at (row, col)
     *
     * @param row    Row of the middle tile
     * @param col    Col of the middle tile
     * @param action Visitor-action
     */
    private void traversSurroundingTilesOf(int row, int col, Action action) {
        for (int i = row - 1; i < row + 2; ++i) {
            if (i < 0 || i > rows - 1) {
                continue;
            }

            for (int j = col - 1; j < col + 2; ++j) {
                if ((i == row && j == col) || j < 0 || j > cols - 1) {
                    continue;
                }

                switch (action) {
                    case UNCOVER:
                        if (tiles[i][j].getState() == Tile.TileState.COVERED || tiles[i][j].getState() == Tile.TileState.UNKNOWN) {
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

    /**
     * Uncovers all tiles (E.g. used for game over)
     */
    public void uncoverAll() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                tiles[i][j].gameOver();
                game.onTileStateChanged(i, j);
            }
        }
    }

    public Tile getTile(int row, int col) {
        return tiles[row][col];
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getMines() {
        return mines;
    }

    /**
     * ============================================================
     * For save games
     * ============================================================
     */

    /**
     * Serializes this gameboard to an array of bytes.
     */
    public byte[] toBytes() {
        return toString().getBytes();
    }

    /**
     * Serializes this gameboard to a JSON string.
     */
    @Override
    public String toString() {
        return toJson().toString();
    }

    /**
     * Serializes this SaveGame to a JSON string.
     */
    public JSONObject toJson() {
        try {
            JSONObject jsonGameBoard = new JSONObject();

            jsonGameBoard.put("rows", rows);
            jsonGameBoard.put("cols", cols);
            jsonGameBoard.put("mines", mines);

            JSONObject jsonTiles = new JSONObject();
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    jsonTiles.put(row + "x" + col, tiles[row][col].toString());
                }
            }
            jsonGameBoard.put("tiles", jsonTiles);

            return jsonGameBoard;
        } catch (JSONException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error converting gameboard to JSON.", ex);
        }
    }

    public static GameBoard fromJson(Game game, String json) {
        if (json == null || json.trim().equals("")) return null;

        GameBoard gameBoard = null;

        try {
            JSONObject jsonGameBoard = new JSONObject(json);

            int rows = jsonGameBoard.getInt("rows");
            int cols = jsonGameBoard.getInt("cols");
            int mines = jsonGameBoard.getInt("mines");

            gameBoard = new GameBoard(game, rows, cols, mines);

            Tile[][] tiles = new Tile[rows][cols];
            JSONObject jsonTiles = jsonGameBoard.getJSONObject("tiles");
            Iterator<?> iterator = jsonTiles.keys();
            while (iterator.hasNext()) {
                String index = (String) iterator.next();
                String[] coordinates = index.split("x");
                int row = Integer.parseInt(coordinates[0]);
                int col = Integer.parseInt(coordinates[1]);

                tiles[row][col] = Tile.loadFromJson(jsonTiles.getString(index));
            }
            gameBoard.loadTiles(tiles);

        } catch (JSONException ex) {
            ex.printStackTrace();
            Log.e(TAG, "Save data has a syntax error: " + json, ex);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Save data has an invalid number in it: " + json, ex);
        }

        return gameBoard;
    }

    /**
     * Loads a given tile-set instead of creating an own (s. setupTiles)
     *
     * @param tiles Tile-set to load
     */
    private void loadTiles(Tile[][] tiles) {
        this.tiles = tiles;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                if (tiles[row][col].isMine()) {
                    traversSurroundingTilesOf(row, col, Action.UPDATE_SURROUNDING_MINE_COUNT);
                } else if (tiles[row][col].getState() == Tile.TileState.NUMBER) {
                    --nrOfCoveredFields;
                }

                if (tiles[row][col].getState() == Tile.TileState.FLAG) {
                    traversSurroundingTilesOf(row, col, Action.INC_SURROUNDING_FLAGS_COUNT);
                }

            }
        }
    }

}