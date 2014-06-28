package de.gehle.pauls.multisweeper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;

import de.gehle.pauls.multisweeper.engine.Game;

import static com.google.android.gms.common.GooglePlayServicesUtil.isGooglePlayServicesAvailable;


public class HomeActivity extends BaseGameActivity {

    public final static String EXTRA_MESSAGE = "gehle.multisweeper.MESSAGE";
    private static final int REQUEST_LEADERBOARD = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

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

    public void continueGame(View view) {
    }

    public void leaderboard(View view) {
        startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getApiClient(), getString(R.string.leaderboard_singleplayer)), REQUEST_LEADERBOARD);
    }

    public void rules(View view) {
        Intent intent = new Intent(this, RulesActivity.class);
        startActivity(intent);
    }

    public void exit(View view) {
        finish();
    }

    @Override
    public void onSignInFailed() {

    }

    @Override
    public void onSignInSucceeded() {

    }
}
