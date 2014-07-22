package de.gehle.pauls.multisweeper.components;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;

import java.util.Arrays;

import de.gehle.pauls.multisweeper.R;
import de.gehle.pauls.multisweeper.engine.Game;
import de.gehle.pauls.multisweeper.engine.MinesweeperObserver;
import de.gehle.pauls.multisweeper.engine.Tile;

/**
 * @author Andi
 */
public abstract class AbstractGameActivity extends BaseGameActivity implements MinesweeperObserver {

    private static final String TAG = "GameActivity";

    private final static int tileWH = 32;
    private final static int tilePadding = 2;

    private static final int minDifficulty = 0;

    protected Game game;
    private TableLayout mineField;
    private TextView timerText;
    private TextView mineCountText;
    protected TileButton[][] tileButtons;

    /**
     * Player ID in multiplayer mode
     */
    protected int myId = 0;


    public AbstractGameActivity() {
        super();
    }

    protected AbstractGameActivity(int requestedClients) {
        super(requestedClients);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * @param difficultyIndex 0 for easy, 1 for medium, 2 for hard
     */
    protected int[] getDifficultySettings(int difficultyIndex) {
        int defaultIndex = 0;

        TypedArray difficulties = getResources().obtainTypedArray(R.array.difficulties);
        int length = difficulties.length() - 1;
        int maxDifficulty = length > 0 ? length - 1 : 0;

        if (difficultyIndex < minDifficulty || difficultyIndex > maxDifficulty) {
            Log.e(TAG, "Invalid difficulty");
            difficultyIndex = defaultIndex;
        }

        int resId = difficulties.getResourceId(difficultyIndex, defaultIndex);
        int[] diffSettings = getResources().getIntArray(resId);

        difficulties.recycle(); // Important!

        return diffSettings;
    }

    /**
     * Sets the layout and binds its elements
     */
    protected void bindGameLayout() {
        setContentView(R.layout.game);
        mineField = (TableLayout) findViewById(R.id.MineField);
        timerText = (TextView) findViewById(R.id.Timer);
        mineCountText = (TextView) findViewById(R.id.MineCount);
    }

    /**
     * For multiplayer
     */
    protected void startGame(int nrOfPlayers) {
        Log.d("Multisweeper", "Game started");

        bindGameLayout();

        Intent intent = getIntent();
        int difficultyIndex = intent.getIntExtra(Game.KEY_DIFFICULTY, 0);

        int[] difficultySettings = getDifficultySettings(difficultyIndex);
        Log.d("this is my array", "arr: " + Arrays.toString(difficultySettings));
        game = new Game(this, difficultySettings[0], difficultySettings[1], difficultySettings[2], nrOfPlayers);

        initButtons();
        showGameState();
    }

    /**
     * Fills tableLayout with buttons and sets its click- and long-click-Listeners
     */
    protected void initButtons() {
        tileButtons = new TileButton[game.getRows()][game.getCols()];
        for (int i = 0; i < game.getRows(); ++i) {
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(
                    new TableRow.LayoutParams(
                            tileWH * tilePadding * game.getCols(),
                            tileWH * tilePadding)
            );
            for (int j = 0; j < game.getCols(); ++j) {
                tileButtons[i][j] = new TileButton(this);
                tableRow.addView(tileButtons[i][j]);

                final int curRow = i;
                final int curCol = j;

                tileButtons[i][j].setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                game.playerMove(myId, curRow, curCol);
                            }
                        }
                );
                tileButtons[i][j].setOnLongClickListener(
                        new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                game.playerMoveAlt(curRow, curCol);
                                return true;
                            }
                        }
                );

                tileButtons[i][j].setLayoutParams(
                        new TableRow.LayoutParams(tileWH * tilePadding, tileWH * tilePadding)
                );
                tileButtons[i][j].setPadding(tilePadding, tilePadding, tilePadding, tilePadding);
            }
            mineField.addView(tableRow,
                    new TableLayout.LayoutParams(
                            tileWH * tilePadding * game.getCols(),
                            tileWH * tilePadding
                    )
            );
        }
    }

    /**
     * Update all tileButtons according to their (new) states.
     * Used for example for game over
     */
    protected void showGameState() {
        for (int i = 0; i < game.getRows(); ++i) {
            for (int j = 0; j < game.getCols(); ++j) {
                updateTile(i, j);
            }
        }
    }

    /**
     * Reset game for singleplayer
     */
    public void resetGame() {
        resetGame(1);
    }

    /**
     * Reset game for multiplayer (Some players might be disconnected)
     */
    public void resetGame(int nrOfPlayers) {
        game.reset(nrOfPlayers);
        showGameState();
    }


    /**
     * ============================================================
     * For GGS
     * ============================================================
     */

    @Override
    public void onSignInFailed() {
        Log.d(TAG, "Sign-in failed.");
    }

    @Override
    public void onSignInSucceeded() {
        Log.d(TAG, "Sign-in succeeded.");
    }


    /**
     * ============================================================
     * For MinesweeperObserver
     * ============================================================
     */

    @Override
    public void updateTile(int row, int col) {
        Tile tile = game.getTile(row, col);
        Tile.TileState state = tile.getState();
        tileButtons[row][col].setState(state);
        if (state == Tile.TileState.NUMBER) {
            tileButtons[row][col].setSurroundingMines(tile.getNrSurroundingMines());
        }
    }

    private String prependZeros(int nrOfZeros, String word) {
        for (int i = 0; i < nrOfZeros; ++i) {
            word = "0" + word;
        }
        return word;
    }

    @Override
    public void updateTimer(int secondsPassed) {
        if (timerText != null) {
            String time = String.valueOf(secondsPassed);
            time = prependZeros(3 - time.length(), time);

            timerText.setText(time);
        }
    }

    @Override
    public void updateCounter(int mineCounter) {
        if (mineCountText != null) {
            String mines = String.valueOf(mineCounter);
            mines = prependZeros(3 - mines.length(), mines);
            mineCountText.setText(mines);
        }
    }

    @Override
    public void onGameStateChanged(Game.GameState gameState) {
        if (gameState == Game.GameState.GAME_WON || gameState == Game.GameState.GAME_LOST) {
            Log.d("GameActivity", "Game finished");
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            int score = game.getScore(myId);

            if (game.getPlace(myId) == 1 && score > 0) {
                dialogBuilder.setTitle(R.string.gamestate_won);
                Games.Leaderboards.submitScore(getApiClient(), getString(R.string.leaderboard_singleplayer), score);
                Log.d("Score", "Score saved");
            } else {
                dialogBuilder.setTitle(R.string.gamestate_lost);
            }

            AlertDialog dialog = dialogBuilder.setMessage("Score: " + score + " Points")
                    .setPositiveButton(R.string.new_game, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            resetGame(game.getNrOfPlayers());
                        }
                    })
                    .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            //Stop the activity
                            AbstractGameActivity.this.finish();
                        }
                    })
                    .create();
            dialog.show();
            TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
            messageText.setGravity(Gravity.CENTER);
        }
    }
}
