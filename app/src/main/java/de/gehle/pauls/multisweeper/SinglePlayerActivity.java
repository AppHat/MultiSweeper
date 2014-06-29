package de.gehle.pauls.multisweeper;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

public class SinglePlayerActivity extends BaseGameActivity implements MinesweeperObserver {

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
        imageButton = (ImageButton) findViewById(R.id.Smiley);

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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            tileButtons[row][col].setText("F");
        } else if (state == Tile.TileState.Unknown) {
            tileButtons[row][col].setText("?");
        } else if (state == Tile.TileState.Mine) {
            tileButtons[row][col].setText("X");
        } else if (state == Tile.TileState.BadFlag) {
            tileButtons[row][col].setText("? :(");
        } else if (state == Tile.TileState.ExplodedMine) {
            tileButtons[row][col].setEnabled(false);
            tileButtons[row][col].setText("*");
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
        if (newState == Game.GameState.GAMESTATE_WON) {
            new AlertDialog.Builder(this)
                    // .setMessage(R.string.gamestate_won)
                    .setMessage("You have won! You scored " + game.getScore() + " Points. Your Score will be submitted to the leaderboard.")
                    .setNeutralButton(getString(R.string.gamestate_button), null)
                    .show();
            Games.Leaderboards.submitScore(getApiClient(), getString(R.string.leaderboard_singleplayer), game.getScore());
            Log.d("Score", "Score saved");
        } else if (newState == Game.GameState.GAMESTATE_LOST) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.gamestate_lost)
                    .setNeutralButton(getString(R.string.gamestate_button), null)
                    .show();
        }
    }

    @Override
    public void onSignInFailed() {

    }

    @Override
    public void onSignInSucceeded() {

    }
}
