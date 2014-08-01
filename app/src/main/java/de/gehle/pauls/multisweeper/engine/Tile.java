package de.gehle.pauls.multisweeper.engine;


import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Tile {

    private static final String TAG = "Tile";

    public enum TileState {
        COVERED,         // not yet opened (only for shownState)
        NUMBER,          // a no-mine field (default for realState)
        FLAG,            // a field considered being a mine by user (only for shownState)
        UNKNOWN,         // a field, where user doesn't know, what it is(only for shownState)
        MINE,            // a mined field
        BAD_FLAG,        // a no-mine field covered with a flag by user (only for Game Over)
        GOOD_FLAG,        // a mine field covered with a flag by user (only for Game Over)
        EXPLODED_MINE    // the mine, which the user stepped on (only for Game Over)
    }

    /**
     * This is the real state of the tile (s. showState)
     */
    private TileState realState;

    /**
     * This state sees the user.
     * The value is being determined by the value of realState and whether the user has opened the tile yet.
     */
    private TileState shownState;

    private int nrSurroundingMines;

    private int nrSurroundingFlags;
    private boolean numberUncovered;

    private int playerLastClicked = 0;

    public Tile() {
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
        return shownState;
    }

    public void gameOver() {
        if (shownState == TileState.FLAG) {
            if (realState != TileState.MINE) {
                shownState = TileState.BAD_FLAG;
            } else {
                shownState = TileState.GOOD_FLAG;
            }
            return;
        }
        if (shownState == TileState.EXPLODED_MINE) {
            return;
        }

        shownState = realState;
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

    public void incNrSurroundingFlags() {
        ++nrSurroundingFlags;
    }

    public void decNrSurroundingFlags() {
        --nrSurroundingFlags;
    }

    public int getNrSurroundingFlags() {
        return nrSurroundingFlags;
    }

    public void setNumberUncovered() {
        numberUncovered = true;
    }

    public boolean isUncoverable() {
        return shownState == TileState.COVERED || shownState == TileState.UNKNOWN;
    }

    public boolean canUncoverSurroundings() {
        return shownState == TileState.NUMBER && !numberUncovered;
    }

    public boolean isSwappable() {
        return isUncoverable() || shownState == TileState.FLAG;
    }

    public void setCovered() {
        if (isSwappable()) {
            shownState = TileState.COVERED;
            playerLastClicked = 0;
        }
    }

    /**
     * Sets a flag for the given player (Visualization of flag depends on playerId)
     *
     * @param playerId PlayerId for multiplayer. In Singleplayer we have the id = 0
     */
    public void setFlag(int playerId) {
        if (isSwappable()) {
            shownState = TileState.FLAG;
            playerLastClicked = playerId;
        }
    }


    /**
     * Sets a question-mark for the given player (Visualization of question-mark depends on playerId)
     *
     * @param playerId PlayerId for multiplayer. In Singleplayer we have the id = 0
     */
    public void setUnknown(int playerId) {
        if (isSwappable()) {
            shownState = TileState.UNKNOWN;
            playerLastClicked = playerId;
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

    public int getPlayerId() {
        return playerLastClicked;
    }

    /**
     * ============================================================
     * For save games & multiplayer exchange gameboard
     * ============================================================
     */

    @Override
    public String toString() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("isMine", realState == TileState.MINE);
            obj.put("isFlag", shownState == TileState.FLAG);
            obj.put("isQuestionMark", shownState == TileState.UNKNOWN);
            obj.put("isCovered", shownState != TileState.NUMBER);
            return obj.toString();
        } catch (JSONException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error converting tile to JSON.", ex);
        }
    }

    public static Tile loadFromJson(String json) {
        if (json == null || json.trim().equals("")) return null;

        Tile tile = new Tile();

        try {
            JSONObject obj = new JSONObject(json);

            if (obj.getBoolean("isMine")) {
                tile.putMine();
            }

            if (obj.getBoolean("isFlag")) {
                tile.shownState = TileState.FLAG;
            } else if (obj.getBoolean("isQuestionMark")) {
                tile.shownState = TileState.UNKNOWN;
            } else if (!obj.getBoolean("isCovered")) {
                tile.shownState = TileState.NUMBER;
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
            Log.e(TAG, "Save data has a syntax error: " + json, ex);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Save data has an invalid number in it: " + json, ex);
        }

        return tile;
    }

}