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
package com.glandorf1.joe.wsprnetviewer.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.glandorf1.joe.wsprnetviewer.app.MainActivity;
import com.glandorf1.joe.wsprnetviewer.app.R;
import com.glandorf1.joe.wsprnetviewer.app.Utility;
import com.glandorf1.joe.wsprnetviewer.app.data.WsprNetContract;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

public class WsprNetViewerSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final int WSPR_NOTIFICATION_ID = 3004; // TODO: is NOTIFICATION_ID chosen at random?
    private static final String LOG_TAG = WsprNetViewerSyncAdapter.class.getSimpleName();
    // Default interval at which to sync with wsprnet.org, in seconds.
    public static final int SYNC_INTERVAL = 60 * 60;  // 1 hour
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 4; // +/- 15 minutes
    private static final double mBandFrequencyTolerancePercent = 5.;
    private static Double[] mBandFrequency, mBandFrequencyMin, mBandFrequencyMax;
    private static String[] mBandFrequencyStr, mBandNameStr;
    private static int mBandNameIdx = -1, nHits = -1;

    private Context mContext;

    public WsprNetViewerSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d(LOG_TAG, "Creating SyncAdapter");
        mContext = context;
    }


    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        // TODO:  verify if wsprSpotQuery is no longer needed
        String wsprSpotQuery = "";
            wsprSpotQuery = Utility.getPreferredGridsquare(mContext);
        // If there's no gridsquare code, there's nothing to look up.
        if (wsprSpotQuery.length() == 0) {
            return;
        }

        try {
            // Get data from live from website.
            // For url_wsprnet_spots, drupal must be true!
            // For url_wsprnet_spots_old, drupal must be false!
          //String source = mContext.getString(R.string.url_wsprnet_spots);
            String source = mContext.getString(R.string.url_wsprnet_spots_old);
            boolean drupal = false; // true= drupal url; false= old database format
            Document wsprHtmlDoc = getWsprData(mContext, source);
            // TODO: 'maxSpots' is unused in queries until it is known how to submit a direct Drupal query to wspr web site.
            int maxSpots = 1000;
            getWsprDataFromTags(mContext, wsprHtmlDoc, maxSpots, wsprSpotQuery, drupal);
        } catch (Throwable e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return;
    }

    /**
     * Helper method to handle insertion of a new gridsquare in the wspr database.
     *
     * @param gridsquareSetting The gridsquare string used to request updates from the server.
     * @param cityName        A human-readable city name, e.g "Mountain View"
     * @param lat             the latitude of the city
     * @param lon             the longitude of the city
     * @return the row ID of the added gridsquare.
     */
    public long addGridsquare(Context context, String gridsquareSetting, String cityName, String countryName, double lat, double lon) {

        // First, check if the gridsquare with this city name exists in the db
        Cursor cursor = context.getContentResolver().query(
                WsprNetContract.GridSquareEntry.CONTENT_URI,
                new String[]{WsprNetContract.GridSquareEntry._ID},
                WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING + " = ?",
                new String[]{gridsquareSetting},
                null);

        if (cursor.moveToFirst()) {
            int gridsquareIdIndex = cursor.getColumnIndex(WsprNetContract.GridSquareEntry._ID);
            return cursor.getLong(gridsquareIdIndex);
        } else {
            ContentValues gridsquareValues = new ContentValues();
            gridsquareValues.put(WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING, gridsquareSetting);
            gridsquareValues.put(WsprNetContract.GridSquareEntry.COLUMN_CITY_NAME, cityName);
            gridsquareValues.put(WsprNetContract.GridSquareEntry.COLUMN_COUNTRY_NAME, countryName);
            gridsquareValues.put(WsprNetContract.GridSquareEntry.COLUMN_COORD_LAT, lat);
            gridsquareValues.put(WsprNetContract.GridSquareEntry.COLUMN_COORD_LONG, lon);

            Uri gridsquareInsertUri = context.getContentResolver()
                    .insert(WsprNetContract.GridSquareEntry.CONTENT_URI, gridsquareValues);

            return ContentUris.parseId(gridsquareInsertUri);
        }
    }


    /**
     * Obtain the Document containing the wspr data in HTML format.
     * Wspr data is from "source"m which can be a file in this app's private area (for testing),
     * a built-in String resource (for testing), or "live" from the wsprnet.org web site.
     * Call from 'doInBackground()', etc.
     */
    public Document getWsprData(Context context, String source) {
        // initialize document to "empty" page
        Document wsprHtmlDoc = Jsoup.parse(context.getString(R.string.html_empty_page));
        try {
            Log.d("Utility", "Connecting to [" + source + "]");
            // Allow html to come from an external app-private file, resource string, or live web site.
            if (source.startsWith(context.getString(R.string.file_wsprnet_spots_prefix))) {
                // get data from file
//                // TODO: get this working--can't load the file that was manually dropped onto SD card; maybe
//                // todo:   USB debug connection prevents local SD card access?
//                File pdir = context.getExternalFilesDir(null);
//                File[] files = pdir.listFiles();
//                for (File inFile : files) {
//                    if (!inFile.isDirectory()) {
//                        Log.d(LOG_TAG, inFile.getName() + ", " + inFile.getAbsolutePath() + ", " + inFile.getCanonicalPath());
//                    }
//                }
//                String fname = pdir.toString() + source.replaceFirst("file:", "");
//                File input = new File(fname);
//                wsprHtmlDoc = Jsoup.parse(input, "UTF-8");
            } else if ((source.startsWith(context.getString(R.string.html_wsprnet_spots_prefix)))) {
                // get data from internal string
//                String html = context.getString(R.string.html_wsprnet_spots_data11)
//                        + context.getString(R.string.html_wsprnet_spots_data12)
//                        + context.getString(R.string.html_wsprnet_spots_data13)
//                        + context.getString(R.string.html_wsprnet_spots_data14);
//                wsprHtmlDoc = Jsoup.parse(URLDecoder.decode(html, "UTF-8"));
            } else if ((source.startsWith(context.getString(R.string.url_wsprnet_spots)))) {
                // Get wspr info from http://www.wsprnet.org/drupal/wsprnet/spots; e.g.:
                wsprHtmlDoc = Jsoup.connect(source).get();
            } else if ((source.startsWith(context.getString(R.string.url_wsprnet_spots_old_base)))) {
                // Get wspr info from http://www.wsprnet.org/drupal/wsprnet/spots; e.g.:
                wsprHtmlDoc = Jsoup.connect(source).get();
            }
            Log.d("getWsprData", "Connected to ["+source+"]");
        } // try
        catch(Throwable t) {
            t.printStackTrace();
        }
        return wsprHtmlDoc;
    } // getWsprData()

    /**
     * Returns name of frequency band, or "" if not within a valid frequency band.
     * @param context
     * @param idx - pass in 'mBandNameIdx', as set by getFrequencyBandCheck()
     * @return Returns name of frequency band, or "" if not within a valid frequency band.
     */
    public String getFrequencyBandName(Context context, int idx) {
        if ((mBandNameStr == null) || (mBandNameStr.length <= 0)) {
            Resources res = context.getResources();
            mBandNameStr = res.getStringArray(R.array.pref_notify_band_options);
        }
        if ((idx >= 0) && (idx < mBandNameStr.length)) {
            return mBandNameStr[idx];
        } else {
            return "";
        }
    }

    /**
     * Determines if a frequency is in one of the bands in R.array.pref_notify_band_values.
     * Checks if it is also in the notification band-- if it is, then set mBandNameIdx
     * and increment the number of hits, nHits.
     * @param context
     * @param freqMhz - a frequency in MHz as returned by a specific WSPR report
     * @param tolerancePercent - freqMhz is in the band if within +/-tolerance of the band's center frequency
     * @return Returns -1 if not within a valid frequency band; also sets mBandNameIdx, increments nHits.
     */
    public double getFrequencyBandCheck(Context context, double freqMhz, double tolerancePercent,
                                        double notifyBandMHzMin, double notifyBandMHzMax) {
        double band = -1;
        if ((mBandFrequencyStr == null) || (mBandFrequencyStr.length <= 0)) {
            Resources res = context.getResources();
            mBandFrequencyStr = res.getStringArray(R.array.pref_notify_band_values);
        }
        if ((mBandFrequencyStr != null) && mBandFrequencyStr.length > 0) {
            if ((mBandFrequency == null) || (mBandFrequency.length <= 0)) {
                mBandFrequency = new Double[mBandFrequencyStr.length];
                mBandFrequencyMin = new Double[mBandFrequencyStr.length];
                mBandFrequencyMax = new Double[mBandFrequencyStr.length];
                if ((tolerancePercent < 1.) || (tolerancePercent > 20.)) {
                    tolerancePercent = 5.;
                }
                for (int i = 0; i < mBandFrequencyStr.length; i++) {
                    mBandFrequency[i] = Double.parseDouble(mBandFrequencyStr[i]);
                    mBandFrequencyMin[i] = mBandFrequency[i] - (mBandFrequency[i] * (tolerancePercent / 100.));
                    mBandFrequencyMax[i] = mBandFrequency[i] + (mBandFrequency[i] * (tolerancePercent / 100.));
                }
            }
            if ((mBandFrequency != null) && (mBandFrequency.length > 0)) {
                for (int i = 0; i < mBandFrequency.length; i++) {
                    if ((mBandFrequencyMin[i] <= freqMhz) && (freqMhz <= mBandFrequencyMax[i])) {
                        band = mBandFrequency[i];
                        if ((notifyBandMHzMin <= band) && (band <= notifyBandMHzMax)) {
                            mBandNameIdx = i; // save for getFrequencyBandName()
                            nHits++;
                        }
                        break;
                    }
                }
            }
        }
        return band;
    }


    /**
     * Clean up and parse the raw timestamp from the html.
     * Since there may be multiple records with the same timestamp (the resolution is only 1 minute),
     * add a fake millisecond offset to distinguish each of them.
     */
    public String parseTimestamp(String timestampStr) {
        SimpleDateFormat timestampFormatIn = new SimpleDateFormat(Utility.TIMESTAMP_FORMAT_WSPR);
        // TODO: getting parse exceptions-but maybe only in debug mode; SDF may not be suitable for
        //       use in static modules.
        //       Investigate using something like joda-time: http://www.joda.org/joda-time/
        // Since there may be multiple records with the same timestamp (the resolution is only 1 minute),
        // add a fake millisecond offset to distinguish each of them.
        try {
            Date inputTimestamp = timestampFormatIn.parse(timestampStr);
            inputTimestamp.setTime(inputTimestamp.getTime()); // + millisecondOffset); // can add a fake ms value to make timestamp unique
            String dbTimestamp = WsprNetContract.getDbTimestampString(inputTimestamp);
            return dbTimestamp;
        } catch (ParseException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            //e.printStackTrace();
            return timestampStr;
        }
    }


    /**
     * Parse the Document containing the wspr data in HTML format.
     * Call from 'doInBackground()', etc.
     */
    public void getWsprDataFromTags(Context context, Document wsprHtml, int maxSpots,
                                           String gridsquareSetting, boolean drupal)
            throws Throwable {

        // These are the names of the objects that need to be extracted.
        // column# 0             1       2           3    4       5      6     7          8     9       10
        //   Timestamp	        Call	MHz	        SNR	Drift	Grid	Pwr	Reporter	RGrid	km      az
        //   2014-08-25 20:40 	DL8EDC  7.040186 	-4 	 0   	JO31le 	 5 	 LA3JJ/L 	JO59bh 	925     11

        // Gridsquare information
        final String WSPRNET_IDX_CITY = "city_name";
        final String WSPRNET_IDX_COUNTRY_NAME = "name";
        //final String WSPRNET_IDX_COORD = "coord";
        final String WSPRNET_IDX_COORD_LAT = "lat";
        final String WSPRNET_IDX_COORD_LONG = "lon";

        // Wspr information html element indices for their 'new' drupal interface.
        //   e.g.: http://wsprnet.org/drupal/wsprnet/spots
        final int WSPRNET_IDX_TIMESTAMP = 0;
        final int WSPRNET_IDX_TX_CALLSIGN = 1;
        final int WSPRNET_IDX_TX_FREQ_MHZ = 2;
        final int WSPRNET_IDX_RX_SNR = 3;
        final int WSPRNET_IDX_RX_DRIFT = 4;
        final int WSPRNET_IDX_TX_GRIDSQUARE = 5;
        final int WSPRNET_IDX_TX_POWER = 6;
        final int WSPRNET_IDX_RX_CALLSIGN = 7;
        final int WSPRNET_IDX_RX_GRIDSQUARE = 8;
        final int WSPRNET_IDX_DISTANCE = 9;
        final int WSPRNET_IDX_AZIMUTH = 10;

        // Wspr information html element indices for their 'old' url query interface.
        //   e.g.: http://wsprnet.org/olddb?mode=html&band=all&limit=10&findcall=&findreporter=&sort=date
        final int WSPRNET_IDX_OLDDB_TIMESTAMP = 0;
        final int WSPRNET_IDX_OLDDB_TX_CALLSIGN = 1;
        final int WSPRNET_IDX_OLDDB_TX_FREQ_MHZ = 2;
        final int WSPRNET_IDX_OLDDB_RX_SNR = 3;
        final int WSPRNET_IDX_OLDDB_RX_DRIFT = 4;
        final int WSPRNET_IDX_OLDDB_TX_GRIDSQUARE = 5;
        final int WSPRNET_IDX_OLDDB_TX_POWER_DBM = 6;
        final int WSPRNET_IDX_OLDDB_TX_POWER_W = 7;
        final int WSPRNET_IDX_OLDDB_RX_CALLSIGN = 8;
        final int WSPRNET_IDX_OLDDB_RX_GRIDSQUARE = 9;
        final int WSPRNET_IDX_OLDDB_DISTANCE_KM = 10;
        final int WSPRNET_IDX_OLDDB_DISTANCE_MILES = 11;
        
        // Notification calculations
        double minSNR  = Utility.getNotifyMinSNR(context);
        double notifyBandMHz = Utility.getNotifyBand(context),
                notifyBandMHzMin = notifyBandMHz - 0.001, notifyBandMHzMax = notifyBandMHz + 0.001;
        String bandName = "";
        mBandNameIdx = -1; // reset which band was found for notification
        nHits = 0;

        try {
            // TODO: get city name, lat/long from gridsquare; determine how to look this up
            String cityName = context.getString(R.string.unknown_city); // generic text until the city/country is looked up
            String countryName = context.getString(R.string.unknown_country);
            double cityLatitude = Utility.gridsquareToLatitude(gridsquareSetting);
            double cityLongitude = Utility.gridsquareToLongitude(gridsquareSetting);

            Log.v(LOG_TAG, cityName + ", with coord: " + cityLatitude + " " + cityLongitude);

            // Insert the gridsquare into the database.
            long locationID = addGridsquare(context, gridsquareSetting, cityName, countryName, cityLatitude, cityLongitude);
            Elements wsprHeader, wsprHeader1, wsprHeader2; // TODO: someday, match up header name instead of relying on a fixed column #
            Elements wsprData;
            if (drupal == true) {
                wsprHeader = wsprHtml.select("div#block-system-main.block.block-system div.content table tbody tr:eq(0)");
                wsprData = wsprHtml.select("div#block-system-main.block.block-system div.content table tbody tr:gt(0)");
            } else {
                wsprHeader1 = wsprHtml.select("html body table tbody tr:eq(0)");
                wsprHeader2 = wsprHtml.select("html body table tbody tr:eq(1)");
                wsprData   = wsprHtml.select("html body table tbody tr:gt(1)");
            }

            // Get and insert the new wspr information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(wsprData.size());

            for (int i = 0; (i < wsprData.size()) && (i < maxSpots); i++) {
                Elements wsprTDRow = wsprData.get(i).select("td");  // table data row split into <td> elements
                // These are the values that will be collected.
                // column# 0             1       2           3    4       5      6     7          8     9       10
                //   Timestamp	        Call	MHz	        SNR	Drift	Grid	Pwr	Reporter	RGrid	km      az

                String timestamp, txCallsign, txGridsquare, rxCallsign, rxGridsquare;
                Double txFreqMhz, rxSnr, rxDrift, txPower, kmDistance, azimuth;
                if (drupal == true) {
                    // Wspr information  for the 'drupal' url query interface.
                    // Get wspr info from http://www.wsprnet.org/drupal/wsprnet/spots; e.g.:
                    //   <table>
                    //   <tr>  <th's> Timestamp	        Call	MHz	        SNR	Drift	Grid	Pwr	Reporter	RGrid	km      az
                    //   <tr>  <td's> 2014-08-25 20:40 	DL8EDC  7.040186 	-4 	 0   	JO31le 	 5 	 LA3JJ/L 	JO59bh 	925     11
                    //   <tr>  <td's> 2014-08-25 20:40 	DL8EDC  7.040183 	-9 	 0   	JO31le 	 5 	 OZ2ABB 	JO65au 	618     31
                    //   <tr>  <td's> 2014-08-25 20:40 	DL8EDC  7.040178 	-14	 0   	JO31le 	 5 	 OH7FES 	KP53bh 	1919    37
                    //    ... </table>
                    // Note: each item in the header or row is a <td> (but not shown above.)
                    // Use the Firefox plugin Firebug to determine the html structure:
                    //   highlight one of the table rows, right-click on the corresponding <TR> element in the
                    //   plugin, then select "Copy CSS Path"; clipboard contains, e.g.:
                    //     html.js body.html.not-front.not-logged-in.one-sidebar.sidebar-first.page-wsprnet.page-wsprnet-spots div#page div#middlecontainer div#main div#squeeze div#squeeze-content div#inner-content div.region.region-content div#block-system-main.block.block-system div.content table tbody tr
                    //Elements wsprHeader = wsprHtml.select("div#block-system-main.block.block-system div.content table tbody tr:eq(0)");
                    //Elements wsprData = wsprHtml.select("div#block-system-main.block.block-system div.content table tbody tr:gt(0)");
                    //Element wsprOneRow = wsprData.get(0);  // syntax to get specific element #
                    //Elements wsprTDRow = wsprRow.select("th"); // syntax to get header elements
                    //Elements wsprTDRow = wsprRow.select("td"); // syntax to get data elements

                    // Get rid of "&nbsp;" (non-break space character)
                    // Save timestamp as: "yyyyMMddHHmmssSSS"
                    timestamp = parseTimestamp(wsprTDRow.get(WSPRNET_IDX_TIMESTAMP).text()
                            .replace(Utility.NBSP, ' ').replace(" .", ".").replace(".0000", "").trim());
                    txCallsign = wsprTDRow.get(WSPRNET_IDX_TX_CALLSIGN).text().replace(Utility.NBSP, ' ').trim();
                    txFreqMhz = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_TX_FREQ_MHZ).text().replace(Utility.NBSP, ' ').trim());
                    rxSnr = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_RX_SNR).text().replace(Utility.NBSP, ' ').trim());
                    rxDrift = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_RX_DRIFT).text().replace(Utility.NBSP, ' ').trim());
                    txGridsquare = wsprTDRow.get(WSPRNET_IDX_TX_GRIDSQUARE).text().replace(Utility.NBSP, ' ').trim();
                    txPower = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_TX_POWER).text().replace(Utility.NBSP, ' ').trim());
                    rxCallsign = wsprTDRow.get(WSPRNET_IDX_RX_CALLSIGN).text().replace(Utility.NBSP, ' ').trim();
                    rxGridsquare = wsprTDRow.get(WSPRNET_IDX_RX_GRIDSQUARE).text().replace(Utility.NBSP, ' ').trim();
                    kmDistance = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_DISTANCE).text().replace(Utility.NBSP, ' ').trim());
                    azimuth = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_AZIMUTH).text().replace(Utility.NBSP, ' ').trim());
                } else {
                    // Wspr information  for the 'old' url query interface.
                    //   e.g.: http://wsprnet.org/olddb?mode=html&band=all&limit=10&findcall=&findreporter=&sort=date
                    // Save timestamp as: "yyyyMMddHHmmssSSS"
                    timestamp = parseTimestamp(wsprTDRow.get(WSPRNET_IDX_OLDDB_TIMESTAMP).text()
                            .replace(Utility.NBSP, ' ').replace(" .", ".").replace(".0000", "").trim());
                    txCallsign = wsprTDRow.get(WSPRNET_IDX_OLDDB_TX_CALLSIGN).text().replace(Utility.NBSP, ' ').trim();
                    txFreqMhz = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_OLDDB_TX_FREQ_MHZ).text().replace(Utility.NBSP, ' ').trim());
                    rxSnr = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_OLDDB_RX_SNR).text().replace(Utility.NBSP, ' ').trim());
                    rxDrift = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_OLDDB_RX_DRIFT).text().replace(Utility.NBSP, ' ').trim());
                    txGridsquare = wsprTDRow.get(WSPRNET_IDX_OLDDB_TX_GRIDSQUARE).text().replace(Utility.NBSP, ' ').trim();
                    txPower = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_OLDDB_TX_POWER_DBM).text().replace(Utility.NBSP, ' ').trim());
                    rxCallsign = wsprTDRow.get(WSPRNET_IDX_OLDDB_RX_CALLSIGN).text().replace(Utility.NBSP, ' ').trim();
                    rxGridsquare = wsprTDRow.get(WSPRNET_IDX_OLDDB_RX_GRIDSQUARE).text().replace(Utility.NBSP, ' ').trim();
                    kmDistance = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_OLDDB_DISTANCE_KM).text().replace(Utility.NBSP, ' ').trim());
                  //miDistance = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_OLDDB_DISTANCE_MILES).text().replace(Utility.NBSP, ' ').trim());
                    // azimuth not provided; must calculate it ourselves
                    azimuth = Utility.latLongToAzimuth(Utility.gridsquareToLatitude(txGridsquare), Utility.gridsquareToLongitude(txGridsquare),
                                                       Utility.gridsquareToLatitude(rxGridsquare), Utility.gridsquareToLongitude(rxGridsquare));
                }  // parse the html

                // Collect the values together.
                ContentValues wsprValues = new ContentValues();
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_LOC_KEY, locationID);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT, timestamp);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN, txCallsign);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TX_FREQ_MHZ, txFreqMhz);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_RX_SNR, rxSnr);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_RX_DRIFT, rxDrift);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TX_GRIDSQUARE, txGridsquare);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_TX_POWER, txPower);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_RX_CALLSIGN, rxCallsign);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_RX_GRIDSQUARE, rxGridsquare);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_DISTANCE, kmDistance);
                wsprValues.put(WsprNetContract.SignalReportEntry.COLUMN_AZIMUTH, azimuth);
                cVVector.add(wsprValues);

                // Are any reports significant enough to notify the user?
                // For now, notify user if the SNR (signal-to-noise ratio) in a report for a particular
                // band is above a threshold.  The SNR and frequency band are user preferences.
                // TODO: determine the full criteria for notifications.  E.g.:
                //         a specific frequency band has opened up,
                //         maybe to a particular region,
                //         maybe some minimum number of reports at a minimum SNR.
                if (rxSnr >= minSNR) {
                    // getFrequencyBandCheck() will check what band the TX frequency is in, and if
                    // it is in the notification band.
                    double bandMHz = getFrequencyBandCheck(context, txFreqMhz, mBandFrequencyTolerancePercent,
                            notifyBandMHzMin, notifyBandMHzMax);
                }
            } // parse html tags

            // Insert items into database.
            if (cVVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                int ii = context.getContentResolver().bulkInsert(WsprNetContract.SignalReportEntry.CONTENT_URI, cvArray);
                Log.v(LOG_TAG, "getWsprDataFromTags: inserted " + cVVector.size() + "(" + Integer.toString(ii) + ") items");
            }

            // Remove items with an unreasonable timestamp (>24 hours from now); otherwise, they're displayed forever!
            // TODO: don't insert these in the first place!
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.HOUR, 24);
            String tomorrowTimestamp = WsprNetContract.getDbTimestampString(cal.getTime());
            int d = context.getContentResolver().delete(WsprNetContract.SignalReportEntry.CONTENT_URI,
                    WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT + " > ?",
                    new String[]{tomorrowTimestamp});
            Log.v(LOG_TAG, "getWsprDataFromTags: deleted " + Integer.toString(d) + " invalid items.");

            // Delete items older than the cutoff period specified in the settings menu.
            // TODO: It might be easier to use System.currentTimeMillis(), which returns time in UTC.  BUT,
            // todo: Date obejcts seem to work in the local time zone; wasn't able to initialize one to UTC.
            cal.setTime(new Date());
            TimeZone tz = TimeZone.getDefault();
            int offsetUTC = tz.getOffset(cal.getTimeInMillis()) / 1000;
            int seconds = Utility.cutoffSeconds(context);
            cal.add(Calendar.SECOND, -offsetUTC);
            cal.add(Calendar.SECOND, -seconds);
            String cutoffTimestamp = WsprNetContract.getDbTimestampString(cal.getTime());
            d = context.getContentResolver().delete(WsprNetContract.SignalReportEntry.CONTENT_URI,
                    WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT + " <= ?",
                    new String[]{cutoffTimestamp});
            Log.v(LOG_TAG, "getWsprDataFromTags: deleted " + Integer.toString(d) + " old items.");

            // Did any reports meet the notification criteria?
            if (nHits > 0) {
                bandName = getFrequencyBandName(context, mBandNameIdx);
                String description = context.getString(R.string.band_open);
                notifyWspr(context, bandName, description, minSNR);
            }

        } catch(Exception e) {
            Log.d(LOG_TAG, "getWsprDataFromTags exception: " + e.toString());

        }
    } // getWsprDataFromTags()


    // Make a notification to the user about propagation conditions; they'll want to get on the air now!
    // Notifications must be enabled in the user preferences, and don't notify any more often than
    // specified in the preferences (the "discard data after ..." cutoff value does double duty.)
    private static void notifyWspr(Context context, String bandName, String description, double snr) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);

        // Get whether notifications are enabled in preferences.
        boolean notificationsEnabled =
                prefs.getBoolean(displayNotificationsKey,
                        Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if (notificationsEnabled) {
            // Don't notify more often than the user-preference cutoff interval.
            // pref_last_notification is only stored; it is not displayed in the Settings menu.
            // pref_last_notification gets saved below, in editor.putLong(lastNotificationKey, ...).
            long prefMillis = 1000*(long)Utility.cutoffSeconds(context);
            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastNotification = prefs.getLong(lastNotificationKey, 0);
            Date now  = new Date(System.currentTimeMillis());
            Date last = new Date(lastNotification);

            if ((System.currentTimeMillis() - lastNotification) >= prefMillis) {
                // It's been long enough since the last notification; send a new one now.

                int iconId = Utility.getIconResourceForWsprCondition(snr);
                String title = context.getString(R.string.app_name);

                // Define the text of the wspr notification.
                String contentText = String.format(context.getString(R.string.format_notification),
                        description,
                        bandName,
                        Utility.formatSnr(context, snr));

                // NotificationCompatBuilder builds backward-compatible notifications.
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(context)
                                .setSmallIcon(iconId)
                                .setContentTitle(title)
                                .setContentText(contentText);

                // Open this app if the user clicks on the notification.
                Intent resultIntent = new Intent(context, MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) context
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                // mId allows you to update the notification later on.
                mNotificationManager.notify(WSPR_NOTIFICATION_ID, mBuilder.build());

                //refreshing last sync
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(lastNotificationKey, System.currentTimeMillis());
                editor.commit();
            }
        }
    } // notifyWspr()


    /**
     * Helper function to make the adapter sync now.
     *
     * @param context The application context
     */
    public static void syncImmediately(Context context) {
        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        /*
         * Request the sync for the default account, authority, and manual sync settings
         */
        ContentResolver.requestSync(getSyncAccount(context), context.getString(R.string.content_authority), settingsBundle);
    }


    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        // app_name = WsprNetViewer
        // sync_account_type = wsprnetviewer.joe.glandorf1.com
        Account newAccount = new Account(context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {
            // Add the account and account type, no password or user data
            // If successful, return the Account object, otherwise report an error.
        boolean ret = accountManager.addAccountExplicitly(newAccount, "", null);
            if (!ret) {
                return null;
            }

            // If you don't set android:syncable="true" in
            // in your <provider> element in the manifest,
            // then call context.setIsSyncable(account, AUTHORITY, 1)
            // here.
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {

        // Schedule the sync for periodic execution
        WsprNetViewerSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        // Without calling setSyncAutomatically, our periodic sync will not be enabled.
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        // Let's do a sync to get things started.
        syncImmediately(context);
    }


    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}
