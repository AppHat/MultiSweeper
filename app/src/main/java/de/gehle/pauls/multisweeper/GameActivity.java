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

    protected Game game;
    protected int myId;
    private TableLayout mineField;
    private TextView timerText;
    private TextView mineCountText;
    //private ImageButton imageButton;
    protected Button[][] tileButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int difficulty = intent.getIntExtra(
                Game.KEY_DIFFICULTY, 0);

        if (difficulty < 0 ||
                difficulty > R.string.hard_label) {
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
        if (state == Tile.TileState.COVERED) {
            tileButtons[row][col].setText("");
        } else if (state == Tile.TileState.NUMBER) {
            if (tile.getNrSurroundingMines() == 0) {
                tileButtons[row][col].setEnabled(false);
                tileButtons[row][col].setText("");
            } else {
                tileButtons[row][col].setText(Integer.toString(tile.getNrSurroundingMines()));
                tileButtons[row][col].setTextColor(colors[tile.getNrSurroundingMines() - 1]);
            }
        } else if (state == Tile.TileState.FLAG) {
            Drawable image = getResources().getDrawable(R.drawable.flag_player1);
            image.setBounds(0, 0, 45, 45);
            tileButtons[row][col].setPadding(8, 0, 0, 0);
            tileButtons[row][col].setCompoundDrawables(image, null, null, null);

        } else if (state == Tile.TileState.UNKNOWN) {
            tileButtons[row][col].setText("?");
            tileButtons[row][col].setTextColor(Color.parseColor("#00d9ff"));
            tileButtons[row][col].setTypeface(null, Typeface.BOLD);

        } else if (state == Tile.TileState.MINE) {
            tileButtons[row][col].setEnabled(false);
            tileButtons[row][col].setText("");
            Drawable image = getResources().getDrawable(R.drawable.mine);
            image.setBounds(0, 0, 45, 45);
            tileButtons[row][col].setPadding(8, 0, 0, 0);
            tileButtons[row][col].setCompoundDrawables(image, null, null, null);

        } else if (state == Tile.TileState.BAD_FLAG) {
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

        } else if (state == Tile.TileState.EXPLODED_MINE) {
            tileButtons[row][col].setEnabled(false);
            tileButtons[row][col].setText("");
            Drawable image = getResources().getDrawable(R.drawable.mine_exploded);
            image.setBounds(0, 0, 45, 45);
            tileButtons[row][col].setPadding(8, 0, 0, 0);
            tileButtons[row][col].setCompoundDrawables(image, null, null, null);
        }
    }

    private String prependZeros(int nrOfZeros, String word){
        for(int i = 0; i < nrOfZeros; ++i){
            word = "0" + word;
        }
        return word;
    }

    @Override
    public void updateTimer(int secondsPassed) {
        if(timerText == null){
            return;
        }
        String time = String.valueOf(secondsPassed);
        time = prependZeros(3 - time.length(), time);

        timerText.setText(time);
    }

    @Override
    public void updateMineCounter(int mineCounter) {
        if(mineCountText == null){
            return;
        }
        String mines = String.valueOf(mineCounter);
        mines = prependZeros(3 - mines.length(), mines);
        mineCountText.setText(mines);
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
