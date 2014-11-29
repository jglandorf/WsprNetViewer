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

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.glandorf1.joe.wsprnetviewer.app.data.WsprNetContract;
import com.glandorf1.joe.wsprnetviewer.app.sync.WsprNetViewerSyncAdapter;

/**
 * Encapsulates getting the wspr data and displaying it in a ListView layout.
 */
public class WsprFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private final String LOG_TAG = WsprFragment.class.getSimpleName();
    private static final String SELECTED_KEY = "selected_position";
    private WsprAdapter mWsprAdapter;
    private static final int WSPR_LOADER = 0;
    private String mGridsquare, mFilterTxCallsign, mFilterRxCallsign, mFilterTxGridsquare, mFilterRxGridsquare, mFilterBand;
    private boolean mFilterAnd, mFiltered;
    private static int mLastNumItems = -1;
    private boolean mIsVisible = false;
    private ListView mListView;
    private TextView mTVGridCallHeader;
    private int mPosition = ListView.INVALID_POSITION;  // selected item's position
    private boolean mDualPane = false; // provision for putting the details fragment next to this fragment for wider screens

    // For the wspr view, show only a subset of the stored data.
    // Specify the columns we need; this is the 'projection' parameter passed to query().
    public static final String[] WSPR_COLUMNS = {
            // Fully qualify the id with a table name in case the content provider joins the
            // gridsquare & wspr tables in the background--both have an _id column.
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
    };

    // These indices are tied to WSPR_COLUMNS.  If WSPR_COLUMNS changes, these must change.
    public static final int COL_WSPR_ID = 0;
    public static final int COL_WSPR_TIMESTAMP     = 1;
    public static final int COL_WSPR_TX_CALLSIGN   = 2;
    public static final int COL_WSPR_TX_FREQ_MHZ   = 3;
    public static final int COL_WSPR_RX_SNR        = 4;
    public static final int COL_WSPR_RX_DRIFT      = 5;
    public static final int COL_WSPR_TX_GRIDSQUARE = 6;
    public static final int COL_WSPR_TX_POWER      = 7;
    public static final int COL_WSPR_RX_CALLSIGN   = 8;
    public static final int COL_WSPR_RX_GRIDSQUARE = 9;
    public static final int COL_WSPR_DISTANCE      =10;
    public static final int COL_WSPR_AZIMUTH       =11;
    public static final int COL_WSPR_CITY_NAME     =12;
    public static final int COL_WSPR_COUNTRY_NAME  =13;
    public static final int COL_WSPR_COORD_LAT     =14;
    public static final int COL_WSPR_COORD_LONG    =15;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(String timestamp);
    }

    public WsprFragment() {
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (this.isVisible()) { // This will likely always be true.
            // This doesn't work when a preference dialog was covering the fragment!
            if (!isVisibleToUser) {
                Log.v(LOG_TAG, "setUserVisibleHint:  Becoming INvisible");
            } else {
                Log.v(LOG_TAG, "setUserVisibleHint:  Becoming visible");
            }
        }
        mIsVisible = isVisibleToUser;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.wsprfragment, menu);
        if (BuildConfig.DEBUG == true) {
            menu.findItem(R.id.action_debug).setVisible(true);
            menu.findItem(R.id.action_debug).setEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        // The action_debug menu (refresh, dump_db, clear_db) is only turned on for BuildConfig.DEBUG.
        if (id == R.id.action_refresh) {
            updateWspr();
            return true;
        }
        if (id == R.id.action_dumpdb) {
            Utility.exportDB(getActivity());
            return true;
        }
        if (id == R.id.action_cleardb) {
            Utility.deleteAllRecords(getActivity());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mWsprAdapter = new WsprAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the grid/call header
        mTVGridCallHeader = (TextView) rootView.findViewById(R.id.textview_list_header_gridsquare);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_wspr);
        mListView.setAdapter(mWsprAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = mWsprAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    ((Callback) getActivity())
                            .onItemSelected(cursor.getString(COL_WSPR_ID)); // TODO: change to record ID!!
                }
                mPosition = position;
            }
        });

        // Get previously-selected position from bundle, restore list selection during onLoadFinished.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO: should this be getSupportLoaderManager?
        getLoaderManager().initLoader(WSPR_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void updateWspr() {
      // invoke the sync adapter service
        WsprNetViewerSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public void onResume() {
        // Will come here when the Settings menu is dismissed.
        super.onResume();
        boolean filterOn = Utility.isFiltered(getActivity());
        // Restart the loader if some of the preferences have changed.
        if (filterOn // and filter settings have changed
                    && (   ((mFilterTxCallsign   != null) && !mFilterTxCallsign.equals((Utility.getFilterCallsign(getActivity(),  true))))
                        || ((mFilterRxCallsign   != null) && !mFilterRxCallsign.equals((Utility.getFilterCallsign(getActivity(), false))))
                        || ((mFilterTxGridsquare != null) && !mFilterTxGridsquare.equals((Utility.getFilterGridsquare(getActivity(), true))))
                        || ((mFilterRxGridsquare != null) && !mFilterRxGridsquare.equals((Utility.getFilterGridsquare(getActivity(), false))))
                        || ((mFilterBand         != null) && !mFilterBand.equals((Utility.getFilterBand(getActivity()))))
                        || (mFilterAnd != Utility.isFilterAnd(getActivity()))
                       )
            || (mFiltered != Utility.isFiltered(getActivity())) // or filter on/off has changed
            || (mWsprAdapter.mainDisplayFormat !=  Utility.getMainDisplayPreference(getActivity())) // or layout has changed
           ) {
            mLastNumItems = -1; // reset so that notification will appear
            getLoaderManager().restartLoader(WSPR_LOADER, null, this); // TODO: should this be getSupportLoaderManager?
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // Sort order:  Descending, by timestamp.
        String sortOrder = WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT + " DESC";
        String mSelection = "", mSelectionBand = "";
        if (Utility.isFiltered(getActivity())) {
            mSelection = Utility.getFilterSelectionStringForSql(getActivity());
            mSelectionBand = Utility.getFilterBandSelectionStringForSql(getActivity(), 5., true);
            if (mSelectionBand.length() > 0) {
                if (mSelection.length() > 0) {
                    mSelection += " and ";
                }
                mSelection += mSelectionBand;
            }
            if (mIsVisible && (mSelection.length() > 0)) {
                // Remind user that items are filtered, in case the result is not what they expect.
                Toast.makeText(getActivity(), getActivity().getString(R.string.toast_filter_items), Toast.LENGTH_SHORT).show();
            }
        }

        int mainDisplayPreference = Utility.getMainDisplayPreference(getActivity());
        if (mWsprAdapter.mainDisplayFormat != mainDisplayPreference) {
            // Update the gridsquare/callsign heading text based on the display layout.
            switch (mainDisplayPreference) {
                case Utility.MAIN_DISPLAY_CALLSIGN: // fit everything into 2 lines of display
                    mTVGridCallHeader.setText(getActivity().getString(R.string.callsign));
                    mTVGridCallHeader.setTextColor(getResources().getColor(R.color.wspr_brown));
                    break;
                case Utility.MAIN_DISPLAY_GRIDCALL: // fit everything into 4 lines of display
                    String g = getActivity().getString(R.string.grid);
                    String c = getActivity().getString(R.string.call);
                    Spannable s = new SpannableString(g + "/" + c);
                    // In "Grid/Call", make "grid" black, "Call" brown, and "/" somewhere between.
                    // Release: For "Grid", set the span to be 1 extra character.
                    s.setSpan(new ForegroundColorSpan(Color.BLACK), 0, g.length()+0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    // Debug: For the "/", set the span to be 2 characters; it will won't display in the color if the span is only 1 character.
                    s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.wspr_brown2)), g.length()+0, g.length()+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.wspr_brown)), g.length()+1, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    mTVGridCallHeader.setText(s);
                    break;
                case Utility.MAIN_DISPLAY_GRIDSQUARE: // fit everything into 2 lines of display
                default:
                    mTVGridCallHeader.setText(getActivity().getString(R.string.gridsquare));
                    mTVGridCallHeader.setTextColor(Color.BLACK);
            }

        }


        // Save some of the preferences to detect if they've changed in onResume().
        mGridsquare = Utility.getPreferredGridsquare(getActivity());
        mWsprAdapter.mainDisplayFormat = mainDisplayPreference;
        mFilterTxCallsign = Utility.getFilterCallsign(getActivity(), true);
        mFilterRxCallsign = Utility.getFilterCallsign(getActivity(), false);
        mFilterTxGridsquare = Utility.getFilterGridsquare(getActivity(), true);
        mFilterRxGridsquare = Utility.getFilterGridsquare(getActivity(), false);
        mFilterAnd = Utility.isFilterAnd(getActivity());
        mFiltered = Utility.isFiltered(getActivity());
        mFilterBand = Utility.getFilterBand(getActivity());
        Uri wsprUri = WsprNetContract.SignalReportEntry.buildWspr();

        // Create and return a CursorLoader that will take care of creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(), // context
                wsprUri,       // URI
                WSPR_COLUMNS,  // String[] projection
                mSelection,    // String selection
                null,          // String[] selectionArgs
                sortOrder      // String sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Integer n = (cursor == null)? 0 : cursor.getCount();
        Log.v(LOG_TAG, "onLoadFinished:  data.getCount()= " + n.toString());
        if ((n == 0) && (mLastNumItems != 0)) {
            // only do this once if there are no items
            if (mIsVisible) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.toast_no_items), Toast.LENGTH_LONG).show();
            }
            mWsprAdapter.swapCursor(cursor);
            mPosition = ListView.INVALID_POSITION;
            if (!Utility.isFiltered(getActivity())) { // TODO: check total # records in database instead
                WsprNetViewerSyncAdapter.syncImmediately(getActivity());
            }
        } else if (n > 0) {
            if (mIsVisible) {
            String msg = getActivity().getString(R.string.toast_num_items, n);
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
            }
            mWsprAdapter.swapCursor(cursor);
            if (mPosition != ListView.INVALID_POSITION) {
                mListView.smoothScrollToPosition(mPosition);
            }
        }
        mLastNumItems = n;

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mWsprAdapter.swapCursor(null);
    }

    public boolean getDualPane() {
        return mDualPane;
    }
}
