package de.gehle.pauls.multisweeper;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.example.games.basegameutils.BaseGameActivity;

import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import de.gehle.pauls.multisweeper.engine.Game;
import de.gehle.pauls.multisweeper.engine.MinesweeperObserver;
import de.gehle.pauls.multisweeper.engine.SaveGame;
import de.gehle.pauls.multisweeper.engine.Tile;

public class SinglePlayerActivity extends GameActivity {

    private static final String TAG = "SINGLE";
    private static final int MAX_SNAPSHOT_RESOLVE_RETRIES = 3;
    private static String defaultSaveGameName = "continueSnapshot";
    private boolean loggedIn = false;

    public SinglePlayerActivity() {
        super(BaseGameActivity.CLIENT_GAMES | BaseGameActivity.CLIENT_SNAPSHOT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindGameLayout();
    }

    @Override
    protected void onStop() {
        //Also false if game ended
        /*
        if (game.isRunning() && loggedIn) {
            Log.d(TAG, "Saving game!");
            ProgressDialog progress = new ProgressDialog(this);
            progress.setTitle("Loading");
            progress.setMessage("Wait while loading...");
            progress.show();
            saveSnapshot(new SaveGame(game));
            progress.dismiss();
        }
        loggedIn = false;
        */
        super.onStop();
    }

    private void loadSaveGame(String saveGameName) {
        Log.i(TAG, "Opening snapshot " + saveGameName);

        final String finalSaveGameName = saveGameName;
        final MinesweeperObserver observer = this;

        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {

            SaveGame s;

            @Override
            protected Integer doInBackground(Void... params) {
                // Open the saved game using its name.
                Snapshots.OpenSnapshotResult result = Games.Snapshots.open(getApiClient(), finalSaveGameName, true).await();
                int status = result.getStatus().getStatusCode();

                if (status == GamesStatusCodes.STATUS_OK) {
                    Snapshot snapshot = result.getSnapshot();
                    // Read the byte content of the saved game.
                    s = new SaveGame(snapshot.readFully(), observer);
                } else {
                    Log.e(TAG, "Error while loading: " + status);
                }

                return status;
            }

            @Override
            protected void onPostExecute(Integer status) {
                // Reflect the changes in the UI.
                Log.d(TAG, s.toString());
                startGame(s);
            }
        };

        task.execute();
    }

    protected void startGame(SaveGame saveGame) {
        initButtons();
        game = new Game(this, saveGame.getDifficulty());
        game.continueGame(saveGame.getTiles(), saveGame.getTimeInSeconds());
        showGameState();
        Log.d("Multisweeper", "Game loaded!");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.single_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_new) {
            resetGame();
        } else if (id == android.R.id.home) {
            //Also false if game ended
            if (game.isRunning()) {
                Log.d(TAG, "Saving game!");
                saveSnapshot(new SaveGame(game));
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSignInFailed() {
        loggedIn = false;
        startGame();
    }

    @Override
    public void onSignInSucceeded() {
        loggedIn = true;
        Intent intent = getIntent();
        String saveGameName = intent.getStringExtra(Game.KEY_SAVEGAME);
        if (saveGameName != null) {
            loadSaveGame(saveGameName);
        } else {
            startGame();
        }
    }

    @Override
    public void onInitGameBoard(Tile[][] tiles) {

    }

    /**
     * Prepares saving Snapshot to the user's synchronized storage, conditionally resolves errors,
     * and stores the Snapshot.
     */

    void saveSnapshot(SaveGame saveGame) {
        final byte[] saveGameData = saveGame.toBytes();
        Log.d(TAG, saveGame.toString());

        AsyncTask<Void, Void, Snapshots.OpenSnapshotResult> task = new AsyncTask<Void, Void, Snapshots.OpenSnapshotResult>() {
            @Override
            protected Snapshots.OpenSnapshotResult doInBackground(Void... params) {
                Snapshots.OpenSnapshotResult result = Games.Snapshots.open(getApiClient(), defaultSaveGameName, true).await();
                return result;
            }

            @Override
            protected void onPostExecute(Snapshots.OpenSnapshotResult result) {
                Snapshot toWrite = processSnapshotOpenResult(result, 0);

                Log.d(TAG, writeSnapshot(toWrite, saveGameData));
            }
        };

        try {
            task.execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates metadata, takes a screenshot, and performs the write operation for saving a
     * snapshot.
     */
    private String writeSnapshot(Snapshot snapshot, byte[] saveGame) {
        // Set the data payload for the snapshot.
        snapshot.writeBytes(saveGame);

        // Save the snapshot.
        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                //.setCoverImage(getScreenShot())
                .setDescription("Modified data at: " + Calendar.getInstance().getTime())
                .build();
        GoogleApiClient api = this.getApiClient();
        Games.Snapshots.commitAndClose(api, snapshot, metadataChange);
        return snapshot.toString();
    }

    /**
     * Gets a screenshot to use with snapshots. Note that in practice you probably do not want to
     * use this approach because tablet screen sizes can become pretty large and because the image
     * will contain any UI and layout surrounding the area of interest.
     */
    Bitmap getScreenShot() {
        TableLayout root = (TableLayout) findViewById(R.id.MineField);
        Bitmap coverImage;
        try {
            root.setDrawingCacheEnabled(true);
            Bitmap base = root.getDrawingCache();
            coverImage = base.copy(base.getConfig(), false /* isMutable */);
        } catch (Exception ex) {
            Log.d(TAG, "Failed to create a screenshot", ex);
            coverImage = null;
        } finally {
            root.setDrawingCacheEnabled(false);
        }
        return coverImage;
    }

    /**
     * Conflict resolution for when Snapshots are opened.
     *
     * @param result The open snapshot result to resolve on open.
     * @return The opened Snapshot on success; otherwise, returns null.
     */
    Snapshot processSnapshotOpenResult(Snapshots.OpenSnapshotResult result, int retryCount) {
        Snapshot mResolvedSnapshot = null;
        retryCount++;
        int status = result.getStatus().getStatusCode();

        Log.d(TAG, "Save Result status: " + status);

        if (status == GamesStatusCodes.STATUS_OK) {
            return result.getSnapshot();
        } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
            return result.getSnapshot();
        } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
            Snapshot snapshot = result.getSnapshot();
            Snapshot conflictSnapshot = result.getConflictingSnapshot();

            // Resolve between conflicts by selecting the newest of the conflicting snapshots.
            mResolvedSnapshot = snapshot;

            if (snapshot.getMetadata().getLastModifiedTimestamp() <
                    conflictSnapshot.getMetadata().getLastModifiedTimestamp()) {
                mResolvedSnapshot = conflictSnapshot;
            }

            Snapshots.OpenSnapshotResult resolveResult = Games.Snapshots.resolveConflict(
                    getApiClient(), result.getConflictId(), mResolvedSnapshot)
                    .await();

            if (retryCount < MAX_SNAPSHOT_RESOLVE_RETRIES) {
                return processSnapshotOpenResult(resolveResult, retryCount);
            } else {
                String message = "Could not resolve snapshot conflicts";
                Log.e(TAG, message);
                Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
            }

        }
        // Fail, return null.
        return null;
    }


}
