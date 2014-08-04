package de.gehle.pauls.multisweeper.components;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.gehle.pauls.multisweeper.R;

/**
 * Created by dima on 04.08.2014.
 */
public class PlacementBoardAdapter extends ArrayAdapter<AbstractMultiPlayerActivity.Placement> {

    private Context context;
    private int layoutId;
    private AbstractMultiPlayerActivity.Placement[] data;

    public PlacementBoardAdapter(Context context,
                                 int layoutId,
                                 AbstractMultiPlayerActivity.Placement[] data) {
        super(context, layoutId, data);
        this.context = context;
        this.layoutId = layoutId;
        this.data = data;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        View row = inflater.inflate(layoutId, parent, false);

        AbstractMultiPlayerActivity.Placement item = data[position];

        LinearLayout llayout = (LinearLayout) row.findViewById(R.id.linearLayout_placement);
        if (item.place % 2 == 0) {
            llayout.setBackgroundColor(Color.GRAY);
        } else {
            llayout.setBackgroundColor(Color.LTGRAY);
        }

        TextView name = (TextView) row.findViewById(R.id.textView_placement_name);
        name.setText(item.player);

        TextView result = (TextView) row.findViewById(R.id.textView_placement_result);
        result.setText("" + item.place + " with " + item.score + " points.");

        return row;
    }
}
