package de.gehle.pauls.multisweeper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.Room;

import java.util.ArrayList;

import de.gehle.pauls.multisweeper.components.AbstractMultiPlayerActivity;
import de.gehle.pauls.multisweeper.engine.Game;
import de.gehle.pauls.multisweeper.engine.GameBoard;

public class MultiPlayerActivity extends AbstractMultiPlayerActivity {

    private static final String TAG = "Multiplayer";

    private boolean gameStarted = false;
    private ArrayList<int[]> clickBuffer = new ArrayList<int[]>();
    private ArrayList<int[]> longClickBuffer = new ArrayList<int[]>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_player);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.multi_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    /**
     * ============================================================
     * Menu button binds
     * ============================================================
     */

    public void startQuickGame(View view) {
        startQuickGameActivity();
    }

    public void invitePlayer(View view) {
        startSelectPlayersActivity();
    }

    public void invitationBox(View view) {
        startInvitationInboxActivity();
    }

    /**
     * ============================================================
     * Changes in game-logic to produce multiplayer mode
     * ============================================================
     */

    /**
     * Overrides ClickListeners to send the click also to all other players
     */
    @Override
    protected void initButtons() {
        super.initButtons();
        for (int i = 0; i < game.getRows(); ++i) {
            for (int j = 0; j < game.getCols(); ++j) {

                final int curRow = i;
                final int curCol = j;

                tileButtons[i][j].setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                int id = mParticipant2Id.get(mMyGoogleId);
                                Log.d(TAG, "My id: " + id);
                                if (hostParticipantId.equals(mMyGoogleId)) {
                                    game.playerMove(id, curRow, curCol);
                                }
                                sendOnClick(curRow, curCol);
                            }
                        }
                );
                tileButtons[i][j].setOnLongClickListener(
                        new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                int id = mParticipant2Id.get(mMyGoogleId);
                                if (hostParticipantId.equals(mMyGoogleId)) {
                                    game.playerMoveAlt(id, curRow, curCol);
                                }
                                sendLongClick(curRow, curCol);
                                return true;
                            }
                        }
                );
            }
        }
    }

    @Override
    public void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);

        //React on state changes ...
    }

    @Override
    protected boolean shouldCancelGame(Room room) {
        //TODO: Impl.
        return false;
    }

    /**
     * ============================================================
     * Real time messages
     * ============================================================
     */

    private void sendOnClick(int row, int col) {
        byte[] message = new byte[3];

        message[0] = 'C';
        message[1] = (byte) row;
        message[2] = (byte) col;

        if (hostParticipantId.equals(mMyGoogleId)) {
            broadcast(message);
        } else {
            sendMessage(hostParticipantId, message);
        }
        Log.d(TAG, "Sending onClick with " + row + "," + col);
    }

    private void sendLongClick(int row, int col) {
        byte[] message = new byte[3];

        message[0] = 'L';
        message[1] = (byte) row;
        message[2] = (byte) col;

        if (hostParticipantId.equals(mMyGoogleId)) {
            broadcast(message);
        } else {
            sendMessage(hostParticipantId, message);
        }
        Log.d(TAG, "Sending onLongClick with " + row + "," + col);
    }

    /**
     * Received messages
     * <p/>
     * Bytes received ([Byte1][Byte2]...[ByteN]) = Meaning
     * [C][3][1] = Participant clicked on field (3,1) with 3,1 as array indices, so min would be 0 and max length -1
     * [L][3][1] = Participant long clicked field (3,1) with 3,1 as array indices, so min would be 0 and max length -1 (Game engine handels if question-mark, flag or removed marks)
     * [B] [Tile1Data1][Tile1Data2][Tile1Data3][Tile1Data4] [Tile2Data1][Tile2Data2].... = Board-sync
     *
     * @param realTimeMessage Real time message received
     */
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
        super.onRealTimeMessageReceived(realTimeMessage);

        byte[] buf = realTimeMessage.getMessageData();
        //String sender = realTimeMessage.getSenderParticipantId();

        char action = (char) buf[0];

        if (action == 'B') {
            byte[] gameBoardData = new byte[buf.length - 1];
            System.arraycopy(buf, 1, gameBoardData, 0, buf.length - 1);
            GameBoard syncGameBoard = GameBoard.fromJson(game, new String(gameBoardData));

            gameStarted = true;
            game.setGameBoard(syncGameBoard);

            if (!clickBuffer.isEmpty()) {
                for (int[] click : clickBuffer) {
                    game.playerMove(click[0], click[1], click[2]);
                }
                clickBuffer.clear();
            }
            if (!longClickBuffer.isEmpty()) {
                for (int[] longClick : longClickBuffer) {
                    game.playerMove(longClick[0], longClick[1], longClick[2]);
                }
                longClickBuffer.clear();
            }

            initButtons();
            showGameState();

        } else {
            int row = (int) buf[1];
            int col = (int) buf[2];
            int id = mParticipant2Id.get(realTimeMessage.getSenderParticipantId());

            if (action == 'C') {
                Log.d(TAG, "Received onClick");
                if (gameStarted || hostParticipantId.equals(mMyGoogleId)) {
                    game.playerMove(id, row, col);
                } else {
                    clickBuffer.add(new int[]{id, row, col});
                }
            } else if (action == 'L') {
                Log.d(TAG, "Received onLongClick");
                if (gameStarted) {
                    game.playerMoveAlt(id, row, col);
                } else {
                    longClickBuffer.add(new int[]{id, row, col});
                }
            }

            //Forwarding messages as host
            if (hostParticipantId.equals(mMyGoogleId)) {
                broadcast(buf);
            }
        }
    }

    /**
     * Broadcast init. of gameboard
     *
     * @param gameState The new state of the game
     */
    @Override
    public void onGameStateChanged(Game.GameState gameState) {
        super.onGameStateChanged(gameState);

        /*
         * If gamestate switches to RUNNING the gameboard is initialised new
         */
        if (gameState == Game.GameState.RUNNING && !gameStarted) {
            Log.d(TAG, "Sending gameboard sync");
            byte[] gameBoardString = game.exportGameBoard();

            byte[] message = new byte[gameBoardString.length + 1];
            message[0] = 'B';
            System.arraycopy(gameBoardString, 0, message, 1, gameBoardString.length);

            //Forwarding messages as host
            if (hostParticipantId.equals(mMyGoogleId)) {
                broadcast(message);
            }
            gameStarted = true;
        } else if (gameState == Game.GameState.GAME_WON || gameState == Game.GameState.GAME_LOST) {
            gameStarted = false;
        }
    }
}
