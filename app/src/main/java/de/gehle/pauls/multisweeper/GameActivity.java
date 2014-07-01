package de.gehle.pauls.multisweeper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;

import de.gehle.pauls.multisweeper.engine.Game;
import de.gehle.pauls.multisweeper.engine.MinesweeperObserver;
import de.gehle.pauls.multisweeper.engine.Tile;

/**
 * Created by Andi on 30.06.2014.
 */
public abstract class GameActivity extends BaseGameActivity implements MinesweeperObserver {

    private final int tileWH = 32;
    private final int tilePadding = 2;

    private Game game;
    private TableLayout mineField;
    private TextView timerText;
    private TextView mineCountText;
    private ImageButton imageButton;
    private Button[][] tileButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_player);

        mineField = (TableLayout) findViewById(R.id.MineField);
        timerText = (TextView) findViewById(R.id.Timer);
        mineCountText = (TextView) findViewById(R.id.MineCount);
        //imageButton = (ImageButton) findViewById(R.id.Smiley);

        Intent intent = getIntent();
        int difficulty = intent.getIntExtra(
                Game.KEY_DIFFICULTY,
                Game.DifficultyId.DIFFICULTY_EASY.ordinal());

        if (difficulty < Game.DifficultyId.DIFFICULTY_EASY.ordinal() ||
                difficulty > Game.DifficultyId.DIFFICULTY_HARD.ordinal()) {
            Log.d("MultiSweeper", "onCreate in SinglePlayerActivity. Invalid difficulty");
            // TODO error handling
        }
        game = new Game(this, Game.DifficultyId.values()[difficulty]);
        game.start();
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
        int[] colors = {
                Color.CYAN, Color.GREEN, Color.RED, Color.RED,
                Color.LTGRAY, Color.GREEN, Color.WHITE, Color.YELLOW,
                Color.GRAY
        };
        Tile tile = game.getTile(row, col);
        Tile.TileState state = tile.getState();
        tileButtons[row][col].setEnabled(true);
        tileButtons[row][col].setTextColor(Color.WHITE);
        tileButtons[row][col].setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        tileButtons[row][col].setPadding(0, 0, 0, 0);
        tileButtons[row][col].setTypeface(null, Typeface.NORMAL);
        if (state == Tile.TileState.Covered) {
            tileButtons[row][col].setText("");
        } else if (state == Tile.TileState.Number) {
            tileButtons[row][col].setEnabled(false);
            if (tile.getNrSurroundingMines() == 0) {
                tileButtons[row][col].setText("");
            } else {
                tileButtons[row][col].setText(Integer.toString(tile.getNrSurroundingMines()));
                tileButtons[row][col].setTextColor(colors[tile.getNrSurroundingMines()]);
            }
        } else if (state == Tile.TileState.Flag) {
            Drawable image = getResources().getDrawable(R.drawable.flag_player1);
            image.setBounds(0, 0, 45, 45);
            tileButtons[row][col].setPadding(8, 0, 0, 0);
            tileButtons[row][col].setCompoundDrawables(image, null, null, null);

        } else if (state == Tile.TileState.Unknown) {
            tileButtons[row][col].setText("?");
            tileButtons[row][col].setTextColor(Color.parseColor("#00d9ff"));
            tileButtons[row][col].setTypeface(null, Typeface.BOLD);

        } else if (state == Tile.TileState.Mine) {
            tileButtons[row][col].setEnabled(false);
            tileButtons[row][col].setText("");
            Drawable image = getResources().getDrawable(R.drawable.mine);
            image.setBounds(0, 0, 45, 45);
            tileButtons[row][col].setPadding(8, 0, 0, 0);
            tileButtons[row][col].setCompoundDrawables(image, null, null, null);

        } else if (state == Tile.TileState.BadFlag) {
            tileButtons[row][col].setEnabled(false);
            if (tile.getNrSurroundingMines() == 0) {
                tileButtons[row][col].setText("");
            } else {
                tileButtons[row][col].setText(Integer.toString(tile.getNrSurroundingMines()));
                tileButtons[row][col].setTextColor(colors[tile.getNrSurroundingMines()]);
            }
            Drawable image = getResources().getDrawable(R.drawable.badflag_player1);
            image.setBounds(0, 0, 45, 45);
            tileButtons[row][col].setPadding(8, 0, 0, 0);
            tileButtons[row][col].setCompoundDrawables(image, null, null, null);
            tileButtons[row][col].setCompoundDrawablePadding(-53);
            tileButtons[row][col].setTextColor(Color.parseColor("#e9e9e9"));

        } else if (state == Tile.TileState.ExplodedMine) {
            tileButtons[row][col].setEnabled(false);
            tileButtons[row][col].setText("");
            Drawable image = getResources().getDrawable(R.drawable.mine_exploded);
            image.setBounds(0, 0, 45, 45);
            tileButtons[row][col].setPadding(8, 0, 0, 0);
            tileButtons[row][col].setCompoundDrawables(image, null, null, null);
        }
    }

    @Override
    public void updateTimer() {
        timerText.setText(game.getTime());
    }

    @Override
    public void updateMineCounter() {
        mineCountText.setText(game.getRemainingMines());
    }

    @Override
    public void onGameStateChanged(Game.GameState newState) {
        if (newState == Game.GameState.GAMESTATE_WON || newState == Game.GameState.GAMESTATE_LOST) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            int score = game.getScore();

            if (newState == Game.GameState.GAMESTATE_WON) {
                dialogBuilder.setTitle(R.string.gamestate_won);
                Games.Leaderboards.submitScore(getApiClient(), getString(R.string.leaderboard_singleplayer), score);
                Log.d("Score", "Score saved");
            } else if (newState == Game.GameState.GAMESTATE_LOST) {
                dialogBuilder.setTitle(R.string.gamestate_lost);
            }

            AlertDialog dialog = dialogBuilder.setMessage("Score: " + score + " Points")
                    .setPositiveButton(R.string.new_game, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            resetGame(null);
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

    private void initButtons() {
        tileButtons = new Button[game.getRows()][game.getCols()];
        for (int i = 0; i < game.getRows(); ++i) {
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(
                    new TableRow.LayoutParams(
                            tileWH * tilePadding * game.getCols(),
                            tileWH * tilePadding)
            );
            for (int j = 0; j < game.getCols(); ++j) {
                tileButtons[i][j] = new Button(this);
                tableRow.addView(tileButtons[i][j]);

                final int curRow = i;
                final int curCol = j;

                tileButtons[i][j].setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                game.playerMove(curRow, curCol);
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

    public void resetGame(View view) {
        game.reset();
        showGameState();
    }
}
