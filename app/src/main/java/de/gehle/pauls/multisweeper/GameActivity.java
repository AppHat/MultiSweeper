package de.gehle.pauls.multisweeper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;

import de.gehle.pauls.multisweeper.components.TileButton;
import de.gehle.pauls.multisweeper.engine.Game;
import de.gehle.pauls.multisweeper.engine.MinesweeperObserver;
import de.gehle.pauls.multisweeper.engine.Tile;

/**
 * Created by Andi on 30.06.2014.
 */
public abstract class GameActivity extends BaseGameActivity implements MinesweeperObserver {

    private final static int tileWH = 32;
    private final static int tilePadding = 2;

    protected Game game;
    protected int myId;
    private TableLayout mineField;
    private TextView timerText;
    private TextView mineCountText;
    //private ImageButton imageButton;
    protected TileButton[][] tileButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int difficulty = intent.getIntExtra(
                Game.KEY_DIFFICULTY, 0);

        if (difficulty < 0 || difficulty > 2) {
            Log.d("MultiSweeper", "onCreate in SinglePlayerActivity. Invalid difficulty");
            // TODO error handling
        }
        game = new Game(this, difficulty);
    }

    protected void bindGameLayout() {
        setContentView(R.layout.game);

        mineField = (TableLayout) findViewById(R.id.MineField);
        timerText = (TextView) findViewById(R.id.Timer);
        mineCountText = (TextView) findViewById(R.id.MineCount);
        //imageButton = (ImageButton) findViewById(R.id.Smiley);
    }

    protected void startGame(int nrOfPlayers) {
        game.start(nrOfPlayers);
        Log.d("Multisweeper", "Game started");
        initButtons();
        showGameState();
    }

    @Override
    public void onSignInFailed() {

    }

    @Override
    public void onSignInSucceeded() {

    }

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
    public void updateMineCounter(int mineCounter) {
        if (mineCountText != null) {
            String mines = String.valueOf(mineCounter);
            mines = prependZeros(3 - mines.length(), mines);
            mineCountText.setText(mines);
        }
    }

    @Override
    /**
     * newState = true stands for playing
     * newState = false stands for game over
     */
    public void onGameStateChanged(boolean gameOver) {
        if (gameOver) {
            Log.d("GameActivity", "Game finished");
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            int score = game.getScore(myId);

            if (game.getPlace(myId) == 1 && game.getScore(myId) > 0) {
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
                            GameActivity.this.finish();
                        }
                    })
                    .create();
            dialog.show();
            TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
            messageText.setGravity(Gravity.CENTER);
        }
    }

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

    private void showGameState() {
        for (int i = 0; i < game.getRows(); ++i) {
            for (int j = 0; j < game.getCols(); ++j) {
                updateTile(i, j);
            }
        }
    }

    public void resetGame(int nrOfPlayers) {
        game.start(nrOfPlayers);
        showGameState();
    }
}
