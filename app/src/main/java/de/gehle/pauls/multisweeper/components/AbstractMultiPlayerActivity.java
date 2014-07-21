package de.gehle.pauls.multisweeper.components;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Implements a general functionality of handling connection errors, storing playerId, fetching roomIds, etc.
 */
public abstract class AbstractMultiPlayerActivity extends AbstractGameActivity implements OnInvitationReceivedListener, RoomUpdateListener, RealTimeMessageReceivedListener, RoomStatusUpdateListener {

    private static final String TAG = "AbstractMultiPlayerActivity";

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

    protected boolean mPlaying = false;

    // The participants in the currently active game
    protected ArrayList<Participant> mParticipants = null;
    protected HashMap<String, Integer> mParticipant2Id = null;
    protected String mMyGoogleId = null;


    /**
     * Create a RoomConfigBuilder that's appropriate for your implementation
     *
     * @return Room configuration
     */
    private RoomConfig.Builder makeBasicRoomConfigBuilder() {
        return RoomConfig.builder(this)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
    }

    /**
     * ============================================================
     * GGS Activities
     * ============================================================
     */

    protected void startQuickGameActivity() {
        Bundle am = RoomConfig.createAutoMatchCriteria(MIN_OTHER_PLAYERS, MAX_OTHER_PLAYERS, 0);

        // build the room config:
        RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
        roomConfigBuilder.setAutoMatchCriteria(am);
        RoomConfig roomConfig = roomConfigBuilder.build();

        // create room:
        Games.RealTimeMultiplayer.create(getApiClient(), roomConfig);

        // prevent screen from sleeping during handshake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // go to game screen
        startGame(mParticipants.size());
    }

    protected void startSelectPlayersActivity() {
        Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(getApiClient(), MIN_OTHER_PLAYERS, MAX_OTHER_PLAYERS);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    /**
     * Launch the intent to show the invitation inbox screen
     */
    protected void startInvitationInboxActivity() {
        Intent intent = Games.Invitations.getInvitationInboxIntent(getApiClient());
        startActivityForResult(intent, RC_INVITATION_INBOX);
    }

    /**
     * ============================================================
     * Real time message helpers
     * ============================================================
     */

    @Override
    public void onActivityResult(int request, int response, Intent data) {
        if (request == RC_WAITING_ROOM) {
            // ignore response code if the waiting room was dismissed from code:
            if (mWaitingRoomFinishedFromCode) return;

            if (response == Activity.RESULT_OK) {
                // (start game)
                startGame(mParticipants.size());
            } else if (response == Activity.RESULT_CANCELED) {
                // Waiting room was dismissed with the back button. The meaning of this
                // action is up to the game. You may choose to leave the room and cancel the
                // match, or do something else like minimize the waiting room and
                // continue to connect in the background.

                // in this example, we take the simple approach and just leave the room:
                Games.RealTimeMultiplayer.leave(getApiClient(), this, mRoomId);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else if (response == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                // player wants to leave the room.
                Games.RealTimeMultiplayer.leave(getApiClient(), this, mRoomId);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } else if (request == RC_SELECT_PLAYERS) {
            if (response != Activity.RESULT_OK) {
                // user canceled
                return;
            }

            // get the invitee list
            Bundle extras = data.getExtras();
            final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

            // get auto-match criteria
            Bundle autoMatchCriteria = null;
            int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

            if (minAutoMatchPlayers > 0) {
                autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            } else {
                autoMatchCriteria = null;
            }

            // create the room and specify a variant if appropriate
            RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
            roomConfigBuilder.addPlayersToInvite(invitees);
            if (autoMatchCriteria != null) {
                roomConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
            }
            RoomConfig roomConfig = roomConfigBuilder.build();
            Games.RealTimeMultiplayer.create(getApiClient(), roomConfig);

            // prevent screen from sleeping during handshake
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if (request == RC_INVITATION_INBOX) {
            if (response != Activity.RESULT_OK) {
                // canceled
                return;
            }

            // get the selected invitation
            Bundle extras = data.getExtras();
            Invitation invitation = extras.getParcelable(Multiplayer.EXTRA_INVITATION);

            // accept it!
            RoomConfig roomConfig = makeBasicRoomConfigBuilder()
                    .setInvitationIdToAccept(invitation.getInvitationId())
                    .build();
            Games.RealTimeMultiplayer.join(getApiClient(), roomConfig);

            // prevent screen from sleeping during handshake
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // go to game screen
            startGame(mParticipants.size());
        }
    }

    /**
     * ============================================================
     * Real time message helpers
     * ============================================================
     */

    protected void broadcast(byte[] message) {
        for (Participant p : mParticipants) {
            if (!p.getParticipantId().equals(mMyGoogleId)) {
                sendMessage(p.getParticipantId(), message);
            }
        }
    }

    protected void sendMessage(String id, byte[] message) {
        Games.RealTimeMultiplayer.sendReliableMessage(getApiClient(), null, message, mRoomId, id);
    }

    /**
     * ============================================================
     * Game start/ cancel properties
     * ============================================================
     */

    // returns whether there are enough players to start the game
    protected boolean shouldStartGame(Room room) {
        int connectedPlayers = 0;
        for (Participant p : room.getParticipants()) {
            if (p.isConnectedToRoom()) ++connectedPlayers;
        }
        return connectedPlayers >= MIN_OTHER_PLAYERS + 1;
    }

    @Override
    protected void startGame(int nrOfPlayers) {
        super.startGame(nrOfPlayers);
        mPlaying = true;
    }

    /**
     * Returns whether the room is in a state where the game should be canceled.
     * <p/>
     * Your game-specific cancellation logic here. For example,
     * you might decide to cancel the game if enough people have declined the invitation or left the room.
     * You can check a participant's status with Participant.getStatus().
     * (Also, your UI should have a Cancel button that cancels the game too)
     *
     * @param room The room for the waiting game
     * @return If game should be canceled
     */
    abstract protected boolean shouldCancelGame(Room room);


    /**
     * ============================================================
     * For GGS
     * ============================================================
     */

    @Override
    public void onSignInSucceeded() {
        Log.d(TAG, "Sign-in succeeded.");

        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
        Games.Invitations.registerInvitationListener(getApiClient(), this);

        // if we received an invite via notification, accept it; otherwise, go to main screen
        if (getInvitationId() != null) {
            RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
            roomConfigBuilder.setInvitationIdToAccept(getInvitationId());
            Games.RealTimeMultiplayer.join(getApiClient(), roomConfigBuilder.build());

            // prevent screen from sleeping during handshake
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // go to game screen
            startGame(mParticipants.size());
        }
    }

    /**
     * ============================================================
     * OnConnect/ OnDisconnect -Handlers
     * ============================================================
     */

    @Override
    public void onInvitationReceived(Invitation invitation) {
        // show in-game popup to let user know of pending invitation

        // store invitation for use when player accepts this invitation
        mIncomingInvitationId = invitation.getInvitationId();

        /**
         * If accept popup accept invatation:
         * RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
         roomConfigBuilder.setInvitationIdToAccept(mIncomingInvitationId);
         Games.RealTimeMultiplayer.join(getApiClient(), roomConfigBuilder.build());

         // prevent screen from sleeping during handshake
         getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

         // now, go to game screen
         */
    }

    @Override
    public void onInvitationRemoved(String s) {

    }

    //=================================================

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            // let screen go to sleep
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // show error message, return to main screen.
            return;
        }

        mRoomId = room.getRoomId();

        // get waiting room intent
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(getApiClient(), room, Integer.MAX_VALUE);
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            // let screen go to sleep
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // show error message, return to main screen.
            return;
        }

        mRoomId = room.getRoomId();

        // get waiting room intent
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(getApiClient(), room, Integer.MAX_VALUE);
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    @Override
    public void onLeftRoom(int i, String s) {
        mRoomId = null;
    }

    @Override
    public void onRoomConnected(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            // let screen go to sleep
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // show error message, return to main screen.
        }

        updateRoom(room);
    }

    //=================================================

    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> strings) {
        updateRoom(room);
    }

    @Override
    public void onPeerDeclined(Room room, List<String> peers) {
        // peer declined invitation -- see if game should be canceled
        if (!mPlaying && shouldCancelGame(room)) {
            Games.RealTimeMultiplayer.leave(getApiClient(), null, mRoomId);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        updateRoom(room);
    }

    @Override
    public void onPeerJoined(Room room, List<String> strings) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> peers) {
        // peer left -- see if game should be canceled
        if (!mPlaying && shouldCancelGame(room)) {
            Games.RealTimeMultiplayer.leave(getApiClient(), this, mRoomId);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        updateRoom(room);
    }

    @Override
    public void onConnectedToRoom(Room room) {
        mRoomId = room.getRoomId();
        mParticipants = room.getParticipants();
        onParticipantsUpdated();
        mMyGoogleId = room.getParticipantId(Games.Players.getCurrentPlayerId(getApiClient()));
    }

    @Override
    public void onDisconnectedFromRoom(Room room) {
        // leave the room
        Games.RealTimeMultiplayer.leave(getApiClient(), this, mRoomId);

        // clear the flag that keeps the screen on
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mRoomId = null;

        // show error message and return to main screen
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        if (mPlaying) {
            // add new player to an ongoing game
        } else if (shouldStartGame(room)) {
            // start game!
            startGame(mParticipants.size());
        }
        updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        if (mPlaying) {
            // do game-specific handling of this -- remove player's avatar
            // from the screen, etc. If not enough players are left for
            // the game to go on, end the game and leave the room.
        } else if (shouldCancelGame(room)) {
            // cancel the game
            Games.RealTimeMultiplayer.leave(getApiClient(), this, mRoomId);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        updateRoom(room);
    }

    @Override
    public void onP2PConnected(String s) {

    }

    @Override
    public void onP2PDisconnected(String s) {

    }

    void updateRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
            onParticipantsUpdated();
        }
        /*
        if (mParticipants != null) {
            updatePeerScoresDisplay();
        }*/
    }

    private void onParticipantsUpdated() {
        if (mParticipant2Id == null) {
            mParticipant2Id = new HashMap<String, Integer>(mParticipants.size());
        }
        mParticipant2Id.clear();
        int id = 0;
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyGoogleId)) {
                myId = id;
            }
            mParticipant2Id.put(p.getParticipantId(), id++);
        }
    }
}
