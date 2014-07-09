package de.gehle.pauls.multisweeper.engine;

import android.os.Parcel;
import android.util.Log;

import com.google.android.gms.drive.Contents;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by Andi on 07.07.2014.
 */
public class SaveGame implements Snapshot {

    private static final String TAG = "SaveGame";
    private int difficulty;
    private int timeInSeconds = 0;
    private Tile[][] tiles;

    public SaveGame(Game game) {
        difficulty = game.getDifficulty();
        timeInSeconds = game.getTimeInSeconds();
        tiles = game.getTiles();
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getTimeInSeconds() {
        return timeInSeconds;
    }

    /**
     * Constructs a SaveGame object from serialized data.
     */
    public SaveGame(byte[] data, MinesweeperObserver observer) {
        if (data == null) return; // default progress
        loadFromJson(new String(data), observer);
    }

    /**
     * Constructs a SaveGame object from a JSON string.
     */
    public SaveGame(String json, MinesweeperObserver observer) {
        if (json == null) return; // default progress
        loadFromJson(json, observer);
    }

    /**
     * Replaces this SaveGame's content with the content loaded from the given JSON string.
     */
    public void loadFromJson(String json, MinesweeperObserver observer) {
        if (json == null || json.trim().equals("")) return;

        try {
            JSONObject obj = new JSONObject(json);
            timeInSeconds = obj.getInt("timeInSeconds");
            this.difficulty = obj.getInt("difficulty");

            Game.Difficulty difficulty = Game.difficulties[this.difficulty];
            tiles = new Tile[difficulty.rows][difficulty.cols];

            JSONObject tiles = obj.getJSONObject("tiles");
            Iterator<?> iterator = tiles.keys();
            while (iterator.hasNext()) {
                Tile tmpTile = Tile.loadFromJson(tiles.getString((String) iterator.next()), observer);
                this.tiles[tmpTile.getRow()][tmpTile.getCol()] = tmpTile;
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
            Log.e(TAG, "Save data has a syntax error: " + json, ex);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Save data has an invalid number in it: " + json, ex);
        }
    }


    @Override
    public SnapshotMetadata getMetadata() {
        return null;
    }

    @Override
    public Contents getContents() {
        return null;
    }

    @Override
    public void iH() {

    }

    @Override
    public byte[] readFully() {
        return new byte[0];
    }

    @Override
    public boolean writeBytes(byte[] bytes) {
        return false;
    }

    @Override
    public boolean modifyBytes(int i, byte[] bytes, int i2, int i3) {
        return false;
    }

    @Override
    public Snapshot freeze() {
        return null;
    }

    @Override
    public boolean isDataValid() {
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    /**
     * Serializes this SaveGame to an array of bytes.
     */
    public byte[] toBytes() {
        return toString().getBytes();
    }

    /**
     * Serializes this SaveGame to a JSON string.
     */
    @Override
    public String toString() {
        try {
            JSONObject tiles = new JSONObject();
            for (Tile[] rowTiles : this.tiles) {
                for (Tile tile : rowTiles) {
                    tiles.put(tile.getRow() + "x" + tile.getCol(), tile.toString());
                }
            }

            JSONObject obj = new JSONObject();
            obj.put("tiles", tiles);
            obj.put("difficulty", difficulty);
            obj.put("timeInSeconds", timeInSeconds);
            return obj.toString();
        } catch (JSONException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error converting save data to JSON.", ex);
        }
    }
}
