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
package com.glandorf1.joe.wsprnetviewer.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Manages a local database for wspr data.
 */
public class WsprNetDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "wspr.db";

    public WsprNetDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold gridsquares, along with the city name, the latitude and longitude.
        // TODO: The city and country will need to be looked up from a web site, such as www.openstreetmap.org; for now these are empty.
        final String SQL_CREATE_GRIDSQUARE_TABLE = "CREATE TABLE " + WsprNetContract.GridSquareEntry.TABLE_NAME + " (" +
                WsprNetContract.GridSquareEntry._ID                       + " INTEGER PRIMARY KEY," +
                WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING + " TEXT UNIQUE NOT NULL, " +
                WsprNetContract.GridSquareEntry.COLUMN_CITY_NAME          + " TEXT NOT NULL, " +
                WsprNetContract.GridSquareEntry.COLUMN_COUNTRY_NAME       + " TEXT NOT NULL, " +
                WsprNetContract.GridSquareEntry.COLUMN_COORD_LAT          + " REAL NOT NULL, " +
                WsprNetContract.GridSquareEntry.COLUMN_COORD_LONG         + " REAL NOT NULL, " +
                "UNIQUE (" + WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING +") ON CONFLICT IGNORE"+
                " );";


        final String SQL_CREATE_WSPR_TABLE = "CREATE TABLE " + WsprNetContract.SignalReportEntry.TABLE_NAME + " (" +
                // AutoIncrement keeps the data sorted in timestamp order.
                WsprNetContract.SignalReportEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                // the ID of the gridsquare entry associated with this wspr data
                WsprNetContract.SignalReportEntry.COLUMN_LOC_KEY       + " TEXT NOT NULL, " +
                WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT + " TEXT NOT NULL, " +
                WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN   + " TEXT NOT NULL, " +
                WsprNetContract.SignalReportEntry.COLUMN_TX_FREQ_MHZ   + " REAL NOT NULL, " +
                WsprNetContract.SignalReportEntry.COLUMN_RX_SNR        + " REAL NOT NULL," +
                WsprNetContract.SignalReportEntry.COLUMN_RX_DRIFT      + " REAL NOT NULL," +
                WsprNetContract.SignalReportEntry.COLUMN_TX_GRIDSQUARE + " TEXT NOT NULL, " +
                WsprNetContract.SignalReportEntry.COLUMN_TX_POWER      + " REAL NOT NULL, " +
                WsprNetContract.SignalReportEntry.COLUMN_RX_CALLSIGN   + " TEXT NOT NULL, " +
                WsprNetContract.SignalReportEntry.COLUMN_RX_GRIDSQUARE + " TEXT NOT NULL, " +
                WsprNetContract.SignalReportEntry.COLUMN_DISTANCE      + " REAL NOT NULL, " +
                WsprNetContract.SignalReportEntry.COLUMN_AZIMUTH       + " REAL NOT NULL, " +

                // Set up the gridsquare column as a foreign key to gridsquare table.
                // See www.sqlite.org/foreignkeys.html
                " FOREIGN KEY (" + WsprNetContract.SignalReportEntry.COLUMN_LOC_KEY + ") REFERENCES " +
                WsprNetContract.GridSquareEntry.TABLE_NAME + " (" + WsprNetContract.GridSquareEntry._ID + ")"
                // Since the timestamps are not unique, make a combination of columns that will be.
                // See www.sqlite.org/lang_conflict.html and www.sqlite.org/lang_createtable.html .
                + ", UNIQUE (" +
                WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT + ", " +
                WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN + ", " +
                WsprNetContract.SignalReportEntry.COLUMN_TX_GRIDSQUARE + ", " +
                WsprNetContract.SignalReportEntry.COLUMN_RX_CALLSIGN + ", " +
                WsprNetContract.SignalReportEntry.COLUMN_RX_GRIDSQUARE +
                ") ON CONFLICT REPLACE);"
                ;
        sqLiteDatabase.execSQL(SQL_CREATE_GRIDSQUARE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_WSPR_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WsprNetContract.GridSquareEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WsprNetContract.SignalReportEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}