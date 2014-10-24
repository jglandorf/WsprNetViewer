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

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Defines table and column names for the WsprNet database.
 */
public class WsprNetContract {
    private static final String LOG_TAG = WsprNetContract.class.getSimpleName();

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.glandorf1.joe.wsprnetviewer.app";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths to be appended to the base content URI
    // E.g., content://com.glandorf1.joe.wsprnetviewer.app/wspr/
    public static final String PATH_WSPR = "wspr";
    public static final String PATH_GRIDSQUARE = "gridsquare";

    // Format used for storing timestamps in the database and for converting into simple date objects for comparisons.
    public static final String TIMESTAMP_FORMAT_DB = "yyyyMMddHHmmssSSS"; //don't use the 'Z': "yyyyMMddHHmm:ss.SSSZ"
    public static final String TIMESTAMP_FORMAT_DB_SHORT = "yyyyMMddHHmm"; //don't use the 'Z': "yyyyMMddHHmm:ss.SSSZ"

    /**
     * Converts Date class to a string representation, used for easy comparison and database lookup.
     * @param timestamp The input timestamp
     * @return a DB-friendly representation of the timestamp, using the format defined in TIMESTAMP_FORMAT_DB.
     */
    public static String getDbTimestampString(Date timestamp){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid timestamp.
        SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT_DB);
        String s = sdf.format(timestamp);
        return s;
    }

    /**
     * Converts a timestampText to a long Unix time representation
     * @param timestampText the input timestamp string
     * @return the Timestamp object
     */
    public static Date getTimestampFromDb(String timestampText) {
        SimpleDateFormat dbTimestampFormat = new SimpleDateFormat(TIMESTAMP_FORMAT_DB);
        try {
            return dbTimestampFormat.parse(timestampText);
        } catch ( ParseException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /* Inner class that defines the table contents of the gridsquare table */
    public static final class GridSquareEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_GRIDSQUARE).build();

        public static final String CONTENT_TYPE      = "vnd.android.cursor.dir/"  + CONTENT_AUTHORITY + "/" + PATH_GRIDSQUARE;
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_GRIDSQUARE;

        // Table name
        public static final String TABLE_NAME = "gridsquare";

        // The rx_gridsquare setting string is what will be sent to the db as the gridsquare query.
        public static final String COLUMN_GRIDSQUARE_SETTING = "gridsquare_setting";

        // More recognizable location, based on gridsquare.
        // TODO:  use google API to get city/country from latitude/longitude
        public static final String COLUMN_CITY_NAME = "city_name";
        public static final String COLUMN_COUNTRY_NAME = "country_name";

        // In order to flag the gridsquare on the map, store the latitude and longitude.
        public static final String COLUMN_COORD_LAT = "coord_lat";
        public static final String COLUMN_COORD_LONG = "coord_long";

        public static Uri buildGridsquareUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    /* Inner class that defines the table contents of the wspr table */
    public static final class SignalReportEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_WSPR).build();

        public static final String CONTENT_TYPE      = "vnd.android.cursor.dir/"  + CONTENT_AUTHORITY + "/" + PATH_WSPR;
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_WSPR;

        //   Timestamp	        Call	MHz	        SNR	Drift	Grid	Pwr	Reporter	RGrid	km      az
        //   2014-08-25 20:40 	DL8EDC  7.040186 	-4 	 0   	JO31le 	 5 	 LA3JJ/L 	JO59bh 	925     11
        public static final String TABLE_NAME = "wspr";

        // Column with the foreign key into the location table.
        public static final String COLUMN_LOC_KEY = "location_id";
        // Timestamp, stored as Text with format yyyy-MM-dd hh:mm
        public static final String COLUMN_TIMESTAMPTEXT = "timestamp";
        // Station callsign transmitting the beacon
        public static final String COLUMN_TX_CALLSIGN = "tx_callsign";
        // Transmit frequency, MHz
        public static final String COLUMN_TX_FREQ_MHZ = "mhz";
        // Signal report (signal-to-noise ratio), can be used to select an icon.
        public static final String COLUMN_RX_SNR = "rx_snr";
        // Signal report, drift. The measured drift of the transmitted signal as seen by the receiver, in Hz/minute.
        public static final String COLUMN_RX_DRIFT = "rx_drift";
        // location of transmitting station
        public static final String COLUMN_TX_GRIDSQUARE = "tx_gridsquare";
        // Transmit power, dBm
        public static final String COLUMN_TX_POWER = "dBm";
        // Station callsign receiving the beacon
        public static final String COLUMN_RX_CALLSIGN = "rx_callsign";
        // location of receiving station
        public static final String COLUMN_RX_GRIDSQUARE = "rx_gridsquare";
        // "Great circle" distance in Km from transmitting station to receiving station.
        public static final String COLUMN_DISTANCE = "km";
        // Azimuth in degrees.
        public static final String COLUMN_AZIMUTH = "azimuth";

        public static Uri buildWspr() {
            return CONTENT_URI;
        }

        public static Uri buildWsprUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildWsprGridsquare(String gridsquareSetting) {
            return CONTENT_URI.buildUpon().appendPath(gridsquareSetting).build();
        }

        public static Uri buildWsprGridsquareWithStartTimestamp(String gridsquareSetting, String startTimestamp) {
            return CONTENT_URI.buildUpon().appendPath(gridsquareSetting)
                    .appendQueryParameter(COLUMN_TIMESTAMPTEXT, startTimestamp).build();
        }

        public static Uri buildWsprGridsquareWithTimestamp(String gridsquareSetting, String timestamp) {
            return CONTENT_URI.buildUpon().appendPath(gridsquareSetting).appendPath(timestamp).build();
        }

        public static String getGridsquareSettingFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getTimestampFromUri(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static String getStartTimestampFromUri(Uri uri) {
            return uri.getQueryParameter(COLUMN_TIMESTAMPTEXT);
        }

    }
}
