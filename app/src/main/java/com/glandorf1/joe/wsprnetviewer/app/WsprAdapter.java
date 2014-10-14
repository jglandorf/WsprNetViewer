/*
 * Copyright (C) 2014 Joseph D. Glandorf
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.glandorf1.joe.wsprnetviewer.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link com.glandorf1.joe.wsprnetviewer.app.WsprAdapter} exposes a list of WSPR data
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class WsprAdapter extends CursorAdapter {

    // TODO: turn these into 'enums'
    private static final int VIEW_TYPE_COUNT = 2;
    private static final int VIEW_TYPE_RECENT = 0;
    private static final int VIEW_TYPE_OLDER = 1;
    // Flag to determine if we want to use a separate view for "recent reports".
    private boolean mUseDualPane = false;

    /**
     * Cache of the children views for a wspr list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView timestampView;
        public final TextView txgridsquareView;
        public final TextView rxgridsquareView;
//        public final TextView txcallsignView;
//        public final TextView rxcallsignView;
        public final TextView txfreqmhzView;
        public final TextView rxsnrView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            timestampView = (TextView) view.findViewById(R.id.list_item_timestamp_textview);
            rxgridsquareView = (TextView) view.findViewById(R.id.list_item_rxgridsquare_textview);
            txgridsquareView = (TextView) view.findViewById(R.id.list_item_txgridsquare_textview);
//            rxcallsignView = (TextView) view.findViewById(R.id.list_item_rxcallsign_textview);
//            txcallsignView = (TextView) view.findViewById(R.id.list_item_txcallsign_textview);
            txfreqmhzView = (TextView) view.findViewById(R.id.list_item_txfreqmhz_textview);
            rxsnrView = (TextView) view.findViewById(R.id.list_item_rxsnr_textview);
            // TODO: implement custom view - not here, remove this
          //txgridsquareCustomView = (WsprNetCustomView) view.findViewById(R.id.view_txgridsquare_customview);
        }
    }

    public WsprAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public void setUseDualPane(boolean useDualPane) {
        mUseDualPane = useDualPane;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int layoutId = R.layout.list_item_wspr;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // Read timestamp from cursor
        String timestampString = cursor.getString(WsprFragment.COL_WSPR_TIMESTAMP);

        // Read wspr condition ID from cursor
        int viewType = getItemViewType(cursor.getPosition());
        // Get wspr icon
        viewHolder.iconView.setImageResource(Utility.getIconResourceForWsprCondition(cursor.getDouble(WsprFragment.COL_WSPR_RX_SNR)));

        // Find TextView and set formatted timestamp on it
        viewHolder.timestampView.setText(Utility.getFormattedTimestamp(timestampString, Utility.TIMESTAMP_FORMAT_HOURS_MINUTES));

        // Read wspr data from cursor, then set text boxes
        String txgridsquare = cursor.getString(WsprFragment.COL_WSPR_TX_GRIDSQUARE);
        viewHolder.txgridsquareView.setText(txgridsquare);
        String rxgridsquare = cursor.getString(WsprFragment.COL_WSPR_RX_GRIDSQUARE);
        viewHolder.rxgridsquareView.setText(rxgridsquare);

        // Not enough space in the layout for these.
//        String txcallsign = cursor.getString(WsprFragment.COL_WSPR_TX_CALLSIGN);
//        viewHolder.txcallsignView.setText(txcallsign);
//        String rxcallsign = cursor.getString(WsprFragment.COL_WSPR_RX_CALLSIGN);
//        viewHolder.rxcallsignView.setText(rxcallsign);

        // Read user preference for metric or English units
        boolean isMetric = Utility.isMetric(context);

        // Read frequency from cursor
        double frequency = cursor.getDouble(WsprFragment.COL_WSPR_TX_FREQ_MHZ);
        viewHolder.txfreqmhzView.setText(Utility.formatFrequency(context, frequency, true));

        // Read RX SNR from cursor
        double rxsnr = cursor.getDouble(WsprFragment.COL_WSPR_RX_SNR);
        viewHolder.rxsnrView.setText(Utility.formatSnr(context, rxsnr));
        // For accessibility, add a content description to the icon field
        // TODO: improve accessibility message for the icon
        viewHolder.iconView.setContentDescription(rxsnr + " dB");

        // TODO: display distance here??
        // Read distance from cursor
        double km = cursor.getDouble(WsprFragment.COL_WSPR_DISTANCE);
      //viewHolder.distanceView.setText(Utility.formatDistance(context, km, isMetric));
    }

    //@Override
    public int getItemViewType(int position) {
        // TODO: Determine if we need an alternate listview for newer vs. older items; get isRecent() working if we do.
        //       Figure out if this can be implemented with the standard override function.
        //return (Utility.isRecent(mContext, timestamp, Utility.cutoffHours(mContext)) || mUseDualPane) ? VIEW_TYPE_RECENT : VIEW_TYPE_OLDER;
        return VIEW_TYPE_OLDER;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
}