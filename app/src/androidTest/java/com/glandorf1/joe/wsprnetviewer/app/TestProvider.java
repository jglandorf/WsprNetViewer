/*
 * Copyright (C) 2014 The Android Open Source Project
 * Modifications Copyright (C) 2014 Joseph D. Glandorf
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
package com.mentisavis.wsprnetviewer.app;


import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mentisavis.wsprnetviewer.app.data.WsprNetContract;

// now replacing below with 4.32
public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    // brings our database to an empty state
    public void deleteAllRecords() {
        mContext.getContentResolver().delete(
                WsprNetContract.SignalReportEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                WsprNetContract.GridSquareEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                WsprNetContract.SignalReportEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                WsprNetContract.GridSquareEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    // Since we want each test to start with a clean slate, run deleteAllRecords
    // in setUp (called by the test runner before each test).
    public void setUp() {
        deleteAllRecords();
    }

    public void testInsertReadProvider() {

        ContentValues testValues = TestDb.createHildenGridsquareValues();

        Uri gridsquareUri = mContext.getContentResolver().insert(WsprNetContract.GridSquareEntry.CONTENT_URI, testValues);
        long locationID = ContentUris.parseId(gridsquareUri);

        // Verify we got a row back.
        assertTrue(locationID != -1);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                WsprNetContract.GridSquareEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        TestDb.validateCursor(cursor, testValues);

        // Now see if we can successfully query if we include the row id
        cursor = mContext.getContentResolver().query(
                WsprNetContract.GridSquareEntry.buildGridsquareUri(locationID),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        TestDb.validateCursor(cursor, testValues);

        // Fantastic.  Now that we have a gridsquare, add some wspr's!
        ContentValues wsprValues = TestDb.createWsprValues(locationID);

        Uri wsprInsertUri = mContext.getContentResolver()
                .insert(WsprNetContract.SignalReportEntry.CONTENT_URI, wsprValues);
        assertTrue(wsprInsertUri != null);

        // A cursor is your primary interface to the query results.
        Cursor wsprCursor = mContext.getContentResolver().query(
                WsprNetContract.SignalReportEntry.CONTENT_URI,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        TestDb.validateCursor(wsprCursor, wsprValues);


        // Add the gridsquare values in with the wspr data so that we can make
        // sure that the join worked and we actually get all the values back
        addAllContentValues(wsprValues, testValues);

        // Get the joined WSPR and Gridsquare data
        wsprCursor = mContext.getContentResolver().query(
                WsprNetContract.SignalReportEntry.buildWsprGridsquare(TestDb.TEST_GRIDSQUARE),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestDb.validateCursor(wsprCursor, wsprValues);

        // Get the joined WSPR and Gridsquare data with a start timestamp
        wsprCursor = mContext.getContentResolver().query(
                WsprNetContract.SignalReportEntry.buildWsprGridsquareWithStartTimestamp(
                        TestDb.TEST_GRIDSQUARE, TestDb.TEST_TIMESTAMP),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestDb.validateCursor(wsprCursor, wsprValues);

        // Get the joined WSPR data for a specific timestamp
        wsprCursor = mContext.getContentResolver().query(
                WsprNetContract.SignalReportEntry.buildWsprGridsquareWithTimestamp(TestDb.TEST_GRIDSQUARE, TestDb.TEST_TIMESTAMP),
                null,
                null,
                null,
                null
        );
        TestDb.validateCursor(wsprCursor, wsprValues);
    }

    public void testGetType() {
        // content://com.mentisavis.wsprnetviewer.app/wspr/
        String type = mContext.getContentResolver().getType(WsprNetContract.SignalReportEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.mentisavis.wsprnetviewer.app/wspr
        assertEquals(WsprNetContract.SignalReportEntry.CONTENT_TYPE, type);

        String testGridsquare = "94074";
        // content://com.mentisavis.wsprnetviewer.app/wspr/94074
        type = mContext.getContentResolver().getType(
                WsprNetContract.SignalReportEntry.buildWsprGridsquare(testGridsquare));
        // vnd.android.cursor.dir/com.mentisavis.wsprnetviewer.app/wspr
        assertEquals(WsprNetContract.SignalReportEntry.CONTENT_TYPE, type);

        String testTimestamp = "20140612";
        // content://com.mentisavis.wsprnetviewer.app/wspr/94074/20140612
        type = mContext.getContentResolver().getType(
                WsprNetContract.SignalReportEntry.buildWsprGridsquareWithTimestamp(testGridsquare, testTimestamp));
        // vnd.android.cursor.item/com.mentisavis.wsprnetviewer.app/wspr
        assertEquals(WsprNetContract.SignalReportEntry.CONTENT_ITEM_TYPE, type);

        // content://com.mentisavis.wsprnetviewer.app/gridsquare/
        type = mContext.getContentResolver().getType(WsprNetContract.GridSquareEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.mentisavis.wsprnetviewer.app/gridsquare
        assertEquals(WsprNetContract.GridSquareEntry.CONTENT_TYPE, type);

        // content://com.mentisavis.wsprnetviewer.app/gridsquare/1
        type = mContext.getContentResolver().getType(WsprNetContract.GridSquareEntry.buildGridsquareUri(1L));
        // vnd.android.cursor.item/com.mentisavis.wsprnetviewer.app/gridsquare
        assertEquals(WsprNetContract.GridSquareEntry.CONTENT_ITEM_TYPE, type);
    }

    public void testUpdateGridsquare() {
        // Create a new map of values, where column names are the keys
        ContentValues values = TestDb.createHildenGridsquareValues();

        Uri gridsquareUri = mContext.getContentResolver().
                insert(WsprNetContract.GridSquareEntry.CONTENT_URI, values);
        long locationID = ContentUris.parseId(gridsquareUri);

        // Verify we got a row back.
        assertTrue(locationID != -1);
        Log.d(LOG_TAG, "New row id: " + locationID);

        ContentValues updatedValues = new ContentValues(values);
        updatedValues.put(WsprNetContract.GridSquareEntry._ID, locationID);
        updatedValues.put(WsprNetContract.GridSquareEntry.COLUMN_CITY_NAME, "Santa's Village");
        updatedValues.put(WsprNetContract.GridSquareEntry.COLUMN_COUNTRY_NAME, "North Pole");

        int count = mContext.getContentResolver().update(
                WsprNetContract.GridSquareEntry.CONTENT_URI, updatedValues, WsprNetContract.GridSquareEntry._ID + "= ?",
                new String[] { Long.toString(locationID)});

        assertEquals(count, 1);

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                WsprNetContract.GridSquareEntry.buildGridsquareUri(locationID),
                null,
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null // sort order
        );

        TestDb.validateCursor(cursor, updatedValues);
    }

    // Make sure we can still delete after adding/updating stuff
    public void testDeleteRecordsAtEnd() {
        deleteAllRecords();
    }


    // The target api annotation is needed for the call to keySet -- we wouldn't want
    // to use this in our app, but in a test it's fine to assume a higher target.
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void addAllContentValues(ContentValues destination, ContentValues source) {
        for (String key : source.keySet()) {
            destination.put(key, source.getAsString(key));
        }
    }

    static final String HILDEN_GRIDSQUARE_SETTING = "EN82df";
    static final String HILDEN_WSPR_START_DATE = "2014-08-26 20:52";

    long locationRowID;

    static ContentValues createHildenWsprValues(long locationID) {
        ContentValues wsprValues = new ContentValues();
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_LOC_KEY, locationID);
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT, HILDEN_WSPR_START_DATE);
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN, "DG1EZ");
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TX_FREQ_MHZ, 7.040052);
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_RX_SNR, -23);
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_RX_DRIFT, -1);
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TX_GRIDSQUARE, "JO31hf");
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TX_POWER, 0.2);
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_RX_CALLSIGN, "LA3JJ");
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_RX_GRIDSQUARE, "JO59bh");
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_DISTANCE, 925);
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_AZIMUTH, 12);

        return wsprValues;
    }

    static ContentValues createHildenGridsquareValues() {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING, HILDEN_GRIDSQUARE_SETTING);
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_CITY_NAME, "Hilden");
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_COUNTRY_NAME, "Germany");
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_COORD_LAT, 42.2917);
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_COORD_LONG, -85.5872);

        return testValues;
    }


    // Inserts both the gridsquare and wspr data for the Hilden data set.
    public void insertHildenData() {
        ContentValues hildenGridsquareValues = createHildenGridsquareValues();
        Uri gridsquareInsertUri = mContext.getContentResolver()
                .insert(WsprNetContract.GridSquareEntry.CONTENT_URI, hildenGridsquareValues);
        assertTrue(gridsquareInsertUri != null);

        locationRowID = ContentUris.parseId(gridsquareInsertUri);

        ContentValues hildenWsprValues = createHildenWsprValues(locationRowID);
        Uri wsprInsertUri = mContext.getContentResolver()
                .insert(WsprNetContract.SignalReportEntry.CONTENT_URI, hildenWsprValues);
        assertTrue(wsprInsertUri != null);
    }

    public void testUpdateAndReadWspr() {
        insertHildenData();
        String newCallsign = "DL8EDC";

        // Make an update to one value.
        ContentValues hildenUpdate = new ContentValues();
        hildenUpdate.put(WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN, newCallsign);

        mContext.getContentResolver().update(
                WsprNetContract.SignalReportEntry.CONTENT_URI, hildenUpdate, null, null);

        // A cursor is your primary interface to the query results.
        Cursor wsprCursor = mContext.getContentResolver().query(
                WsprNetContract.SignalReportEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Make the same update to the full ContentValues for comparison.
        ContentValues hildenAltered = createHildenWsprValues(locationRowID);
        hildenAltered.put(WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN, newCallsign);

        TestDb.validateCursor(wsprCursor, hildenAltered);
    }

    public void testRemoveDistanceAndReadWspr() {
        insertHildenData();
        // TODO: determine what 'km = 925' vs. 'km = 1' does.  Is it deleting based on a row index or value?
        int rowsDeleted =
            mContext.getContentResolver().delete(WsprNetContract.SignalReportEntry.CONTENT_URI,
                WsprNetContract.SignalReportEntry.COLUMN_DISTANCE + " = " + locationRowID, null);
            //rowsDeleted =
            //    mContext.getContentResolver().delete(WsprNetContract.SignalReportEntry.CONTENT_URI,
            //            WsprNetContract.SignalReportEntry.COLUMN_DISTANCE + " = " + "925", null);

        // A cursor is your primary interface to the query results.
        Cursor wsprCursor = mContext.getContentResolver().query(
                WsprNetContract.SignalReportEntry.CONTENT_URI, null, null, null, null);

        // Make the same update to the full ContentValues for comparison.
        ContentValues hildenAltered = createHildenWsprValues(locationRowID);
        hildenAltered.remove(WsprNetContract.SignalReportEntry.COLUMN_DISTANCE);

        TestDb.validateCursor(wsprCursor, hildenAltered);
        int idx;
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_RX_DRIFT);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_AZIMUTH);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_RX_CALLSIGN);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_RX_GRIDSQUARE);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_RX_SNR);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_TX_FREQ_MHZ);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_TX_GRIDSQUARE);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_TX_POWER);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_LOC_KEY);
        idx = wsprCursor.getColumnIndex(WsprNetContract.SignalReportEntry.COLUMN_DISTANCE);
        assertEquals(-1, idx);
    }
}