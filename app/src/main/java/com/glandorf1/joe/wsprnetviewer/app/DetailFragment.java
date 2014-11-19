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

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.glandorf1.joe.wsprnetviewer.app.data.WsprNetContract;
import com.glandorf1.joe.wsprnetviewer.app.data.WsprNetCustomView;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String WSPR_SHARE_HASHTAG = " #WsprNetViewerApp";

    private static final String GRIDSQUARE_KEY = "gridsquare";

    private ShareActionProvider mShareActionProvider;
    private String mGridsquare;
    private String mWspr;
    private String mDetailsKeyStr;
    private String mRxGridsquare, mTxGridsquare;

    private static final int DETAIL_LOADER = 0;

    private static final String[] WSPR_COLUMNS = {
            WsprNetContract.SignalReportEntry.TABLE_NAME + "." + WsprNetContract.SignalReportEntry._ID,
            WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT,
            WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN,
            WsprNetContract.SignalReportEntry.COLUMN_TX_FREQ_MHZ,
            WsprNetContract.SignalReportEntry.COLUMN_RX_SNR,
            WsprNetContract.SignalReportEntry.COLUMN_RX_DRIFT,
            WsprNetContract.SignalReportEntry.COLUMN_TX_GRIDSQUARE,
            WsprNetContract.SignalReportEntry.COLUMN_TX_POWER,
            WsprNetContract.SignalReportEntry.COLUMN_RX_CALLSIGN,
            WsprNetContract.SignalReportEntry.COLUMN_RX_GRIDSQUARE,
            WsprNetContract.SignalReportEntry.COLUMN_DISTANCE,
            WsprNetContract.SignalReportEntry.COLUMN_AZIMUTH
            // TODO: because the two tables (gridsquare, wspr) are joined, determine if
            //       including the following items can look up the city/country along with the
            //       wspr data.
            //       For now, if it is included, the error occurs when making the query():
            //         android.database.sqlite.SQLiteException: no such column: gridsquare_setting (code 1): , while compiling:
            //         SELECT wspr._id, timestamp, tx_callsign, mhz, rx_snr, rx_drift, tx_gridsquare, dBm, rx_callsign, rx_gridsquare, km, azimuth, gridsquare_setting, city_name, country_name, coord_lat, coord_long FROM wspr WHERE _id = '4100' ORDER BY _id
//             ,
//            // This can work because the WsprNetProvider returns gridsquare data joined with
//            // wspr data, even though they're stored in two different tables.
//            WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING,
//            WsprNetContract.GridSquareEntry.COLUMN_CITY_NAME,
//            WsprNetContract.GridSquareEntry.COLUMN_COUNTRY_NAME,
//            WsprNetContract.GridSquareEntry.COLUMN_COORD_LAT,
//            WsprNetContract.GridSquareEntry.COLUMN_COORD_LONG
    };

    private ImageView mIconView;
    private TextView mTimeAgoView;
    private TextView mTimestampView;
    private TextView mTxCallsignView;
    private TextView mTxFreqMhzView;
    private TextView mTxGridsquareView;
    private TextView mTxPowerView;
    private TextView mRxSnrView;
    private TextView mRxDriftView;
    private TextView mDistanceView;
    private TextView mDistanceLabelView;
    private TextView mAzimuthView;
    private TextView mRxCallsignView;
    private TextView mRxGridsquareView;
    private WsprNetCustomView mDetailMap;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(GRIDSQUARE_KEY, mGridsquare);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mDetailsKeyStr = arguments.getString(DetailActivity.DETAILS_KEY);
        }

        if (savedInstanceState != null) {
            mGridsquare = savedInstanceState.getString(GRIDSQUARE_KEY);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mTimestampView = (TextView) rootView.findViewById(R.id.detail_timestamp_textview);
        mTimeAgoView = (TextView) rootView.findViewById(R.id.detail_timeago_textview);
        mTxCallsignView = (TextView) rootView.findViewById(R.id.detail_txcallsign_textview);
        mTxFreqMhzView = (TextView) rootView.findViewById(R.id.detail_txfreqmhz_textview);
        mTxGridsquareView = (TextView) rootView.findViewById(R.id.detail_txgridsquare_textview);
        mTxPowerView = (TextView) rootView.findViewById(R.id.detail_txpower_textview);
        mRxSnrView = (TextView) rootView.findViewById(R.id.detail_rxsnr_textview);
        mRxDriftView = (TextView) rootView.findViewById(R.id.detail_rxdrift_textview);
        mDistanceView = (TextView) rootView.findViewById(R.id.detail_distance_textview);
        mDistanceLabelView = (TextView) rootView.findViewById(R.id.detail_distance_label_textview);
        mAzimuthView = (TextView) rootView.findViewById(R.id.detail_azimuth_textview);
        mRxCallsignView = (TextView) rootView.findViewById(R.id.detail_rxcallsign_textview);
        mRxGridsquareView = (TextView) rootView.findViewById(R.id.detail_rxgridsquare_textview);
        mDetailMap = (WsprNetCustomView) rootView.findViewById(R.id.detail_map);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.DETAILS_KEY) &&
                mGridsquare != null &&
                !mGridsquare.equals(Utility.getPreferredGridsquare(getActivity()))) {
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this); // TODO: should this be getSupportLoaderManager?
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mWspr != null) {
            mShareActionProvider.setShareIntent(createShareWsprIntent());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_map_tx) {
            openGridsquareInMap(mTxGridsquare, "Tx: " + mTxGridsquare);
            return true;
        }
        if (id == R.id.action_map_rx) {
            openGridsquareInMap(mRxGridsquare, "Rx: " + mRxGridsquare);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openGridsquareInMap(String gridsquare, String label) {
        if (gridsquare.length() > 0) {
            Double lat = Utility.gridsquareToLatitude(gridsquare);
            Double lon = Utility.gridsquareToLongitude(gridsquare);
            // Use the URI 'geo' scheme to show a gridsquare location on a map.
            // See http://developer.android.com/guide/components/intents-common.html#Maps
            //   "geo:<lat>,<long>?q=<lat>,<long>(LabelName)"
            Uri geoGridsquare = Uri.parse("geo:" + lat.toString() + "," + lon.toString() + "?").buildUpon()
                    .appendQueryParameter("q", lat.toString() + "," + lon.toString() + "(" + label + ")")
                    // TODO: zoom level doesn't seem to do anything
                    //       ex: geo:?q=37.9375%2C-122.29166666666666(Tx%3A%20CM87uw)&z=4
                    .appendQueryParameter("z", "4")
                    .build();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(geoGridsquare);
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                Log.d(LOG_TAG, "Call '" + geoGridsquare.toString() + "'.");
                startActivity(intent);
            } else {
                Log.d(LOG_TAG, "Couldn't call '" + geoGridsquare.toString() + "', no receiving apps installed!");
            }
        }
    }


    private Intent createShareWsprIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mWspr +"\n" + WSPR_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mGridsquare = savedInstanceState.getString(GRIDSQUARE_KEY);
        }

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.DETAILS_KEY)) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = "_id";

        mGridsquare = Utility.getPreferredGridsquare(getActivity());
        Uri wsprForGridsquareUri = WsprNetContract.SignalReportEntry.buildWsprUri(Long.parseLong(mDetailsKeyStr));

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                wsprForGridsquareUri,
                WSPR_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            // Read wspr SNR from cursor, display it and use it to select icon
            double rxSnr = data.getDouble(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_RX_SNR));
            mIconView.setImageResource(Utility.getIconResourceForWsprCondition((double) (rxSnr), true));
            mRxSnrView.setText(Utility.formatSnr(getActivity(), rxSnr));
            // For accessibility, add a content description to the icon field
            mIconView.setContentDescription(Double.toString(rxSnr) + " dBm");

            // Read timestamp from cursor and update views for day of week and timestamp
            String timestamp = data.getString(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT));
            String timeAgoText = Utility.getTimeAgo(getActivity(), timestamp);
            String timestampText = Utility.getFormattedTimestamp(timestamp, Utility.TIMESTAMP_FORMAT_HOURS_MINUTES);
            mTimeAgoView.setText(timeAgoText);
            mTimestampView.setText(timestampText);


            boolean isMetric = Utility.isMetric(getActivity());

            // Read TX power, RX drift from cursor and update view
            double txpower = data.getDouble(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_TX_POWER));
            String txpowerString = Utility.formatPower(getActivity(), txpower);
            mTxPowerView.setText(txpowerString);
            double rxDrift = data.getDouble(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_RX_DRIFT));
            String rxDriftString = Utility.formatRxDrift(getActivity(), rxDrift);
            mRxDriftView.setText(rxDriftString);

            // Read TX frequency from cursor and update view
            double txMhz = data.getDouble(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_TX_FREQ_MHZ));
            String txMhzString = Utility.formatFrequency(getActivity(), txMhz, true); // TODO: option to display wavelength
            mTxFreqMhzView.setText(txMhzString);


            // Read callsigns from cursor and update view
            // TODO: decide if callsigns need any special formatting
            String txCallsign = data.getString(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN))
                    .replace(Utility.NBSP, ' ').trim();
            mTxCallsignView.setText(Utility.formatCallsign(getActivity(), txCallsign, true));
            String rxCallsign = data.getString(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_RX_CALLSIGN))
                    .replace(Utility.NBSP, ' ').trim();
            mRxCallsignView.setText(Utility.formatCallsign(getActivity(), rxCallsign, false));

            // Read km distance and azimuth from cursor and update view.
            // If the 'units' preference changes, this auto-magically gets updated.
            double km = data.getDouble(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_DISTANCE));
            mDistanceView.setText(Utility.formatDistance(getActivity(), km, isMetric));
            if (!Utility.isMetric(getActivity())) {
                mDistanceLabelView.setText(getString(R.string._units_english_distance));
            }
            double degrees = data.getDouble(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_AZIMUTH));
            degrees = degrees < 0 ? 0 : degrees;
            mAzimuthView.setText(Utility.formatAzimuth(getActivity(), degrees));

            // Read gridsquares from cursor and update view
            mRxGridsquare = data.getString(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_RX_GRIDSQUARE));
            String gridsquare = Utility.formatGridsquare(getActivity(), mRxGridsquare, false);
            mRxGridsquareView.setText(gridsquare);
            mTxGridsquare = data.getString(data.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_TX_GRIDSQUARE));
                   gridsquare = Utility.formatGridsquare(getActivity(), mTxGridsquare, true);
            mTxGridsquareView.setText(gridsquare);
            // Update lat/long/azimuth for the map
            mDetailMap.setLatLongAzimuth(
                    Utility.gridsquareToLatitude(mTxGridsquare),
                    Utility.gridsquareToLongitude(mTxGridsquare),
                    Utility.gridsquareToLatitude(mRxGridsquare),
                    Utility.gridsquareToLongitude(mRxGridsquare),
                    degrees);

            // These are for testing:
//            mDetailMap.setLatLongAzimuth(
//                    Utility.gridsquareToLatitude("AA"),
//                    Utility.gridsquareToLongitude("AA"),
//                    Utility.gridsquareToLatitude("KK"),
//                    Utility.gridsquareToLongitude("KK"),
//                    degrees);

            // These are for testing:
//            Double lat, lon;
//            lat = Utility.gridsquareToLatitude("AA00");
//            lon = Utility.gridsquareToLongitude("AA00");
//            Log.v(LOG_TAG, "lat/long(AA00)= " + lat.toString() + ", " + lon.toString() );
//            lat = Utility.gridsquareToLatitude("GG");
//            lon = Utility.gridsquareToLongitude("GG");
//            Log.v(LOG_TAG, "lat/long(GG00)= " + lat.toString() + ", " + lon.toString() );
//            lat = Utility.gridsquareToLatitude("II");
//            lon = Utility.gridsquareToLongitude("II");
//            Log.v(LOG_TAG, "lat/long(II00)= " + lat.toString() + ", " + lon.toString() );
//            lat = Utility.gridsquareToLatitude("RR");
//            lon = Utility.gridsquareToLongitude("RR");
//            Log.v(LOG_TAG, "lat/long(RR00)= " + lat.toString() + ", " + lon.toString() );
//
//            lat = Utility.gridsquareToLatitude("DD");
//            lon = Utility.gridsquareToLongitude("DD");
//            Log.v(LOG_TAG, "lat/long(DD00)= " + lat.toString() + ", " + lon.toString() );
//            lat = Utility.gridsquareToLatitude("OD");
//            lon = Utility.gridsquareToLongitude("OD");
//            Log.v(LOG_TAG, "lat/long(OD00)= " + lat.toString() + ", " + lon.toString() );
//            lat = Utility.gridsquareToLatitude("OO");
//            lon = Utility.gridsquareToLongitude("OO");
//            Log.v(LOG_TAG, "lat/long(OO00)= " + lat.toString() + ", " + lon.toString() );
//            lat = Utility.gridsquareToLatitude("DO");
//            lon = Utility.gridsquareToLongitude("DO");
//            Log.v(LOG_TAG, "lat/long(DO00)= " + lat.toString() + ", " + lon.toString() );

             // Set up a string for a "share intent":
             mWspr = String.format("WSPR @%s UTC: %s MHz/%s dBm SNR\n" +
                                   " tx/rx grid: %s/%s\n" +
                                   " tx/rx call: %s/%s",
                     timestampText, txMhzString, rxSnr,
                     mTxGridsquare, mRxGridsquare,
                     txCallsign, rxCallsign);

            // If onCreateOptionsMenu has already happened, update the share intent.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareWsprIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }
}