package de.gehle.pauls.multisweeper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.Room;

import java.util.ArrayList;
import java.util.HashMap;

import de.gehle.pauls.multisweeper.components.AbstractMultiPlayerActivity;
import de.gehle.pauls.multisweeper.engine.Game;
import de.gehle.pauls.multisweeper.engine.GameBoard;

public class MultiPlayerActivity extends AbstractMultiPlayerActivity {

    private static final String TAG = "Multiplayer";

    /**
     * Arbitrary request codes for the default UIs like waiting-room or invitation-box.
     * This can be any integer that's unique in our Activity.
     */
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_WAITING_ROOM = 10002;

    final static int MIN_OTHER_PLAYERS = 1;
    final static int MAX_OTHER_PLAYERS = 2;

    private String mRoomId;
    private boolean mWaitingRoomFinishedFromCode = false;
    private String mIncomingInvitationId;

    private boolean mPlaying = false;
    // The participants in the currently active game
    ArrayList<Participant> mParticipants = null;
    HashMap<String, Integer> mParticipant2Id = null;

    private String mMyGoogleId = null;

    //============================================================

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
                                game.playerMove(id, curRow, curCol);
                                sendOnClick(curRow, curCol);
                            }
                        }
                );
                tileButtons[i][j].setOnLongClickListener(
                        new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                game.playerMoveAlt(curRow, curCol);
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

        broadcast(message);
        Log.d(TAG, "Sending onClick with " + row + "," + col);
    }

    private void sendLongClick(int row, int col) {
        byte[] message = new byte[3];

        message[0] = 'L';
        message[1] = (byte) row;
        message[2] = (byte) col;

        broadcast(message);
        Log.d(TAG, "Sending onLongClick with " + row + "," + col);
    }

    /**
     * Received messages
     * <p/>
     * Bytes received ([Byte1][Byte2]...[ByteN]) = Meaning
     * [C][3][1] = Participant clicked on field (3,1) with 3,1 as array indices, so min would be 0 and max length -1
     * [L][3][1] = Participant long clicked field (3,1) with 3,1 as array indices, so min would be 0 and max length -1 (Game engine handels if question-mark, flag or removed marks)
     * [S] = Starting game (E.g. Creator of the room has clicked on play in the waiting room, so we should also switch to game screen and leave the waiting room)
     * [B] [Tile1Data1][Tile1Data2][Tile1Data3][Tile1Data4] [Tile2Data1][Tile2Data2].... = Board-sync
     *
     * @param realTimeMessage Real time message received
     */
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
        byte[] buf = realTimeMessage.getMessageData();
        String sender = realTimeMessage.getSenderParticipantId();
        Log.d(TAG, "Message received: " + (char) buf[0] + "/" + (int) buf[1]);

        char action = (char) buf[0];

        if (action == 'S') {
            Log.d(TAG, "Received startGame");
            mWaitingRoomFinishedFromCode = true;
            finishActivity(RC_WAITING_ROOM);
            startGame(mParticipants.size());

        } else if (action == 'B') {
            byte[] gameBoardData = new byte[buf.length - 1];
            for (int i = 1; i < buf.length; i++) {
                gameBoardData[i - 1] = buf[i];
            }
            GameBoard syncGameBoard = GameBoard.fromJson(game, new String(gameBoardData));
            game.setGameBoard(syncGameBoard);

        } else {
            int row = (int) buf[1];
            int col = (int) buf[2];

            if (action == 'C') {
                Log.d(TAG, "Received onClick");
                int id = mParticipant2Id.get(realTimeMessage.getSenderParticipantId());
                game.playerMove(id, row, col);
            } else if (action == 'L') {
                Log.d(TAG, "Received onLongClick");
                game.playerMoveAlt(row, col);
            }
        }
    }

    /**
     * Broadcast init. of gameboard
     *
     * @param gameState
     */
    @Override
    public void onGameStateChanged(Game.GameState gameState) {
        super.onGameStateChanged(gameState);

        /*
         * If gamestate switches to RUNNING the gameboard is initialised new
         */
        if (gameState == Game.GameState.RUNNING) {
            Log.d(TAG, "Sending gameboard sync");
            byte[] gameBoardString = game.exportGameBoard();

            byte[] message = new byte[gameBoardString.length + 1];
            message[0] = 'B';

            for (int i = 1; i <= gameBoardString.length; i++) {
                message[i] = gameBoardString[i - 1];
            }

            broadcast(message);
        }
    }
}
