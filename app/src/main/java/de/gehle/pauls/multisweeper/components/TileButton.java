package de.gehle.pauls.multisweeper.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.Button;

import de.gehle.pauls.multisweeper.R;
import de.gehle.pauls.multisweeper.engine.Tile;

import static de.gehle.pauls.multisweeper.engine.Tile.TileState.COVERED;
import static de.gehle.pauls.multisweeper.engine.Tile.TileState.NUMBER;

/**
 * Representation button of a tile element
 *
 * @author Andi
 */
public class TileButton extends Button {

    /**
     * Drawables support for 3 different players
     */
    private final static int MAX_DRAWABLE_ID = 2;

    private Tile.TileState state = COVERED;
    private int surrounding_mines;

    private int playerId = 0;

    private final static int[] textColors = {
            Color.CYAN, Color.GREEN, Color.RED, Color.LTGRAY,
            Color.BLUE, Color.WHITE, Color.YELLOW, Color.GRAY
    };

    public TileButton(Context context) {
        super(context);
        normalize();
    }

    public void setSurroundingMines(int mines) {
        surrounding_mines = mines;
        if (state == NUMBER) {
            normalize();
            displayNumber();
        }
    }

    public void setState(Tile.TileState state) {
        this.state = state;
        normalize();

        switch (state) {
            case COVERED:
                break;
            case NUMBER:
                displayNumber();
                break;
            case FLAG:
                displayFlag();
                break;
            case UNKNOWN:
                displayUnknown();
                break;
            case MINE:
                displayMine();
                break;
            case BAD_FLAG:
                displayBadFlag();
                break;
            case GOOD_FLAG:
                displayGoodFlag();
                break;
            case EXPLODED_MINE:
                displayExplodedMine();
                break;
            default:
                this.state = COVERED;
        }
    }

    public void setPlayerId(int id) {
        playerId = id;
    }

    /**
     * Returns playerId or 0 if playerId is to high and has no own drawables yet
     *
     * @return playerId || 0
     */
    public int getPlayerId() {
        if (playerId >= 0 && playerId <= MAX_DRAWABLE_ID) {
            return playerId;
        }

        return 0;
    }

    private void normalize() {
        this.setEnabled(true);
        this.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        this.setTypeface(null, Typeface.NORMAL);
        this.setTextColor(Color.WHITE);
        this.setText("");
        this.setBackgroundResource(R.drawable.btn_default_holo_dark);
        this.setPadding(0, 0, 0, 0);
    }

    private void displayNumber() {
        if (surrounding_mines == 0) {
            this.setEnabled(false);
        } else {
            this.setBackgroundResource(R.drawable.btn_default_disabled_holo_dark);
            this.setPadding(0, 0, 0, 0);
            this.setText(Integer.toString(surrounding_mines));
            this.setTextColor(textColors[surrounding_mines - 1]);
        }
    }

    private void displayFlag() {
        Drawable image = getResources().getDrawable(getResources().getIdentifier("flag_player" + getPlayerId(), "drawable", "de.gehle.pauls.multisweeper"));
        image.setBounds(0, 0, 45, 45);
        this.setPadding(8, 0, 0, 0);
        this.setCompoundDrawables(image, null, null, null);
    }

    private void displayUnknown() {
        /*
        this.setText("?");
        this.setTextColor(Color.parseColor("#00d9ff"));
        this.setTypeface(null, Typeface.BOLD);
        */
        Drawable image = getResources().getDrawable(getResources().getIdentifier("questionmark_player" + getPlayerId(), "drawable", "de.gehle.pauls.multisweeper"));
        image.setBounds(0, 0, 45, 45);
        this.setPadding(10, 0, 0, 0);
        this.setCompoundDrawables(image, null, null, null);
    }

    private void displayMine() {
        this.setEnabled(false);
        Drawable image = getResources().getDrawable(R.drawable.mine);
        image.setBounds(0, 0, 45, 45);
        this.setPadding(10, 0, 0, 0);
        this.setCompoundDrawables(image, null, null, null);
    }

    private void displayBadFlag() {
        this.setEnabled(false);
        this.setText(Integer.toString(surrounding_mines));
        if (surrounding_mines > 0) {
            this.setTextColor(textColors[surrounding_mines - 1]);
        }
        Drawable image = getResources().getDrawable(getResources().getIdentifier("badflag_player" + getPlayerId(), "drawable", "de.gehle.pauls.multisweeper"));
        image.setBounds(0, 0, 45, 45);
        this.setPadding(8, 0, 0, 0);
        this.setCompoundDrawables(image, null, null, null);
        this.setCompoundDrawablePadding(-53);
        this.setTextColor(Color.parseColor("#e9e9e9"));
    }

    private void displayGoodFlag() {
        this.setEnabled(false);
        displayFlag();
    }

    private void displayExplodedMine() {
        this.setEnabled(false);
        Drawable image = getResources().getDrawable(R.drawable.mine_exploded);
        image.setBounds(0, 0, 45, 45);
        this.setPadding(8, 0, 0, 0);
        this.setCompoundDrawables(image, null, null, null);
    }
}
