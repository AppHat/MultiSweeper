package de.gehle.pauls.multisweeper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.example.games.basegameutils.BaseGameActivity;

import de.gehle.pauls.multisweeper.engine.Game;

import static com.google.android.gms.common.GooglePlayServicesUtil.isGooglePlayServicesAvailable;


public class HomeActivity extends BaseGameActivity {

    private static final int REQUEST_LEADERBOARD = 0;
    private static final int REQUEST_ACHIEVEMENTS = 1;
    private static final String TAG = "HOME";

    private Button continueButton;
    private MenuItem logoutButton;
    private MenuItem loginButton;

    public HomeActivity() {
        super(BaseGameActivity.CLIENT_GAMES | BaseGameActivity.CLIENT_SNAPSHOT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        continueButton = (Button) findViewById(R.id.continue_game_button);
        if (isSignedIn()) {
            checkSaveGame(SinglePlayerActivity.DEFAULT_SAVE_GAME_NAME);
        }

        if (isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            Log.d("GGS", "Google Game Service is available!");
        } else {
            Log.d("GGS", "Google Game Service not available! Status: " + isGooglePlayServicesAvailable(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);

        logoutButton = menu.findItem(R.id.action_logout);
        loginButton = menu.findItem(R.id.action_login);
        if (isSignedIn() || true) {
            showLogoutButton();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            signOut();
            showLoginButton();
        } else if (id == R.id.action_login) {
            beginUserInitiatedSignIn();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * ============================================================
     * Menu button binds
     * ============================================================
     */

    public void startGame(View view) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.difficulty_title)
                .setItems(R.array.difficulty,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                startNewGame(i);
                            }
                        }
                ).show();
    }

    private void startNewGame(int i) {
        Intent intent = new Intent(this, SinglePlayerActivity.class);
        intent.putExtra(Game.KEY_DIFFICULTY, i);
        startActivity(intent);
    }

    private void startNewGame(String currentSaveName) {
        Intent intent = new Intent(this, SinglePlayerActivity.class);
        intent.putExtra(Game.KEY_SAVEGAME, currentSaveName);
        startActivity(intent);
    }

    public void continueGame(View view) {
        Intent savedGamesIntent = Games.Snapshots.getSelectSnapshotIntent(this.getApiClient(), "Game to continue", false, false, 1);
        startActivityForResult(savedGamesIntent, 0);
    }

    /**
     * After you start the intent to select a snapshot, this callback
     * will be triggered.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (intent != null && intent.hasExtra(Snapshots.EXTRA_SNAPSHOT_METADATA)) {
            SnapshotMetadata snapshotMetadata = intent.getParcelableExtra(Snapshots.EXTRA_SNAPSHOT_METADATA);
            String currentSaveName = snapshotMetadata.getUniqueName();
            startNewGame(currentSaveName);
        }
    }

    public void multiplayer(View view) {
        Intent intent = new Intent(this, MultiPlayerActivity.class);
        startActivity(intent);
    }

    public void leaderboard(View view) {
        startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getApiClient(), getString(R.string.leaderboard_singleplayer)), REQUEST_LEADERBOARD);
    }

    public void achievements(View view) {
        startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), REQUEST_ACHIEVEMENTS);
    }

    public void rules(View view) {
        Intent intent = new Intent(this, RulesActivity.class);
        startActivity(intent);
    }

    public void exit(View view) {
        finish();
    }


    /**
     * ============================================================
     * Login/ Logout button
     * ============================================================
     */

    public void showLogoutButton() {
        Log.d(TAG, "Showing logout button");
        loginButton.setVisible(false);
        logoutButton.setVisible(true);
    }

    public void showLoginButton() {
        Log.d(TAG, "Showing login button");
        logoutButton.setVisible(false);
        loginButton.setVisible(true);
    }


    /**
     * ============================================================
     * For GGS
     * ============================================================
     */

    @Override
    public void onSignInFailed() {
        if (logoutButton != null) {
            showLoginButton();
        }
    }

    @Override
    public void onSignInSucceeded() {
        if (logoutButton != null) {
            showLogoutButton();
        }
        checkSaveGame(SinglePlayerActivity.DEFAULT_SAVE_GAME_NAME);
    }

    private void checkSaveGame(String saveGameName) {
        Log.i(TAG, "Checking snapshot for " + saveGameName);

        final String finalSaveGameName = saveGameName;

        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                // Open the saved game using its name.
                Snapshots.OpenSnapshotResult result = Games.Snapshots.open(getApiClient(), finalSaveGameName, true).await();
                int status = result.getStatus().getStatusCode();

                if (status == GamesStatusCodes.STATUS_OK) {
                    final Snapshot snapshot = result.getSnapshot();
                    if (snapshot.readFully().length == 0) {
                        return GamesStatusCodes.STATUS_GAME_NOT_FOUND;
                    }
                }

                return status;
            }

            @Override
            protected void onPostExecute(Integer status) {
                if (status == GamesStatusCodes.STATUS_OK) {
                    continueButton.setEnabled(true);
                }
            }
        };

        task.execute();
    }
}
