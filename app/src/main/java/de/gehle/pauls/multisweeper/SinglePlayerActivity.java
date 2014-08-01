package de.gehle.pauls.multisweeper;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.Toast;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.example.games.basegameutils.BaseGameActivity;

import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import de.gehle.pauls.multisweeper.components.AbstractGameActivity;
import de.gehle.pauls.multisweeper.engine.Game;
import de.gehle.pauls.multisweeper.engine.MinesweeperObserver;

public class SinglePlayerActivity extends AbstractGameActivity {

    private static final String TAG = "SINGLE";

    private static final int MAX_SNAPSHOT_RESOLVE_RETRIES = 3;
    public static final String DEFAULT_SAVE_GAME_NAME = "continueSnapshot";

    private String loadSaveGameName = null;

    public SinglePlayerActivity() {
        super(BaseGameActivity.CLIENT_GAMES | BaseGameActivity.CLIENT_SNAPSHOT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String saveGameName = intent.getStringExtra(Game.KEY_SAVEGAME);
        if (saveGameName != null) {
            if (isSignedIn()) {
                loadSaveGame(saveGameName);
            } else {
                //To see the game, but not yet started or init
                bindGameLayout();
                loadSaveGameName = saveGameName;
            }
        } else {
            startGame(1);
        }
    }

    @Override
    protected void onStop() {
        //TODO:Uncomment cause: Better way to save games also on crash but: Donno y, but ggs is disconnected here before saving. But super onStop is called later... :/
        /*
        if (game.isRunning() && loggedIn) {
            Log.d(TAG, "Saving game!");
            saveSnapshot(new SaveGame(game));
        }
        loggedIn = false;
        */
        super.onStop();
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
                ProgressDialog progress = new ProgressDialog(this);
                progress.setTitle("Loading");
                progress.setMessage("Wait while loading...");
                progress.show();

                saveSnapshot(game);

                progress.dismiss();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSignInFailed() {
        startGame(1);
    }

    @Override
    public void onSignInSucceeded() {
        if (loadSaveGameName != null) {
            loadSaveGame(loadSaveGameName);
            loadSaveGameName = null;
        }
    }

    @Override
    public void onGameStateChanged(Game.GameState gameState) {
        super.onGameStateChanged(gameState);

        if (gameState == Game.GameState.GAME_WON) {
            Games.Achievements.increment(getApiClient(), getString(R.string.achievement_singleplayer_5_games_won), 1);
        }
    }


    /**
     * ============================================================
     * For save games
     * ============================================================
     */

    /**
     * Prepares saving Snapshot to the user's synchronized storage, conditionally resolves errors,
     * and stores the Snapshot.
     */
    void saveSnapshot(Game saveGame) {
        final byte[] saveGameData = saveGame.toBytes();
        Log.d(TAG, saveGame.toString());

        AsyncTask<Void, Void, Snapshots.OpenSnapshotResult> task = new AsyncTask<Void, Void, Snapshots.OpenSnapshotResult>() {
            @Override
            protected Snapshots.OpenSnapshotResult doInBackground(Void... params) {
                return Games.Snapshots.open(getApiClient(), DEFAULT_SAVE_GAME_NAME, true).await();
            }

            @Override
            protected void onPostExecute(Snapshots.OpenSnapshotResult result) {
                Snapshot toWrite = processSnapshotOpenResult(result, 0);

                writeSnapshot(toWrite, saveGameData);
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
    private void writeSnapshot(Snapshot snapshot, byte[] saveGame) {
        // Set the data payload for the snapshot.
        snapshot.writeBytes(saveGame);

        // Save the snapshot.
        SnapshotMetadataChange.Builder metadataChangeBuilder = new SnapshotMetadataChange.Builder()
                .setDescription("Modified data at: " + Calendar.getInstance().getTime());

        Bitmap screenshot = getScreenShot();
        if (screenshot != null) {
            metadataChangeBuilder.setCoverImage(screenshot);
        }

        SnapshotMetadataChange metadataChange = metadataChangeBuilder.build();
        Games.Snapshots.commitAndClose(getApiClient(), snapshot, metadataChange);
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
            coverImage = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(coverImage);
            root.layout(0, 0, root.getLayoutParams().width, root.getLayoutParams().height);
            root.draw(c);
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
        Snapshot mResolvedSnapshot;
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

            Snapshots.OpenSnapshotResult resolveResult = Games.Snapshots.resolveConflict(getApiClient(), result.getConflictId(), mResolvedSnapshot).await();

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

    private void loadSaveGame(String saveGameName) {
        Log.i(TAG, "Opening snapshot " + saveGameName);

        final String finalSaveGameName = saveGameName;
        final MinesweeperObserver observer = this;

        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                // Open the saved game using its name.
                Snapshots.OpenSnapshotResult result = Games.Snapshots.open(getApiClient(), finalSaveGameName, true).await();
                int status = result.getStatus().getStatusCode();

                if (status == GamesStatusCodes.STATUS_OK) {
                    final Snapshot snapshot = result.getSnapshot();
                    Games.Snapshots.delete(getApiClient(), snapshot.getMetadata());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Read the byte content of the saved game.
                            game = new Game(observer, snapshot.readFully());
                        }
                    });
                } else {
                    Log.e(TAG, "Error while loading: " + status);
                }

                return status;
            }

            @Override
            protected void onPostExecute(Integer status) {
                Log.d("Multisweeper", "Game loaded!");
                initButtons();
                showGameState();
            }
        };

        task.execute();
    }
}
