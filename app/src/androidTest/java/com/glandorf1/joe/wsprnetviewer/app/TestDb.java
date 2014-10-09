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


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mentisavis.wsprnetviewer.app.data.WsprNetContract;
import com.mentisavis.wsprnetviewer.app.data.WsprNetDbHelper;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();
    static final String TEST_GRIDSQUARE = "JO31hf";
    static final String TEST_TIMESTAMP = "2014-08-26 20:52";

    public void testCreateDb() throws Throwable {
        mContext.deleteDatabase(WsprNetDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WsprNetDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }

    public void testInsertReadDb() {

        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        WsprNetDbHelper dbHelper = new WsprNetDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = createHildenGridsquareValues();

        long gridsquareRowId;
        gridsquareRowId = db.insert(WsprNetContract.GridSquareEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue(gridsquareRowId != -1);
        Log.d(LOG_TAG, "New row id: " + gridsquareRowId);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // A cursor is your primary interface to the query results.
        Cursor cursor = db.query(
                WsprNetContract.GridSquareEntry.TABLE_NAME,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        validateCursor(cursor, testValues);

        // Fantastic.  Now that we have a gridsquare, add some WsprNet!
        ContentValues wsprValues = createWsprValues(gridsquareRowId);

        long wsprRowId = db.insert(WsprNetContract.SignalReportEntry.TABLE_NAME, null, wsprValues);
        assertTrue(wsprRowId != -1);

        // A cursor is your primary interface to the query results.
        Cursor wsprCursor = db.query(
                WsprNetContract.SignalReportEntry.TABLE_NAME,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null  // sort order
        );

        validateCursor(wsprCursor, wsprValues);

        dbHelper.close();
    }

    static ContentValues createWsprValues(long gridsquareRowId) {
        // Timestamp           Call    MHz         SNR Drift	Grid	Pwr	Reporter	RGrid	km	az
        // 2014-08-26 20:52	DG1EZ  7.040052 	-23	-1  	JO31hf	0.2	LA3JJ   	JO59bh	925	12  TX: Hilden, Germany; RX: Logneveien, Norway
        ContentValues wsprValues = new ContentValues();
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_LOC_KEY, gridsquareRowId);
        wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT, TEST_TIMESTAMP);
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
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING, TEST_GRIDSQUARE);
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_CITY_NAME, "Hilden");
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_COUNTRY_NAME, "Germany");
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_COORD_LAT, 51.189785);
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_COORD_LONG, 6.962889);
        return testValues;
    }

    static ContentValues createLogneveienGridsquareValues() {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING, TEST_GRIDSQUARE);
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_CITY_NAME, "Logneveien");
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_COUNTRY_NAME, "Norway");
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_COORD_LAT, 59.314785);
        testValues.put(WsprNetContract.GridSquareEntry.COLUMN_COORD_LONG, 10.129556);
        return testValues;
    }

    static void validateCursor(Cursor valueCursor, ContentValues expectedValues) {

        assertTrue(valueCursor.moveToFirst());
        DecimalFormat decimal3 = new DecimalFormat("#,##0.000");

        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse(idx == -1);
            String expectedValue = entry.getValue().toString();
            String actualValue = valueCursor.getString(idx);
            try { // if values look like doubles, round to 3 decimals to avoid epsilon errors
                double exp = Double.parseDouble(expectedValue);
                double act = Double.parseDouble(actualValue);
                expectedValue = decimal3.format(exp);
                actualValue = decimal3.format(act);
            //} catch (ParseException e) {
            } catch (NumberFormatException e) {
                // nothing to do
                //Log.d(LOG_TAG, "validateCursor: parse error: idx= " + String.valueOf(idx) + ", exp= '" + expectedValue + "', act= '" + actualValue + "'");
            }
            Log.d(LOG_TAG, "validateCursor: idx= " + String.valueOf(idx) + ", exp= '" + expectedValue + "', act= '" + actualValue + "'");
            if (!expectedValue.equals(actualValue)) {
                Log.d(LOG_TAG, "validateCursor: idx= " + String.valueOf(idx) + ", exp= '" + expectedValue + "', act= '" + actualValue + "'");
            }
            assertEquals(expectedValue, actualValue);
        }
        valueCursor.close();
    }
}