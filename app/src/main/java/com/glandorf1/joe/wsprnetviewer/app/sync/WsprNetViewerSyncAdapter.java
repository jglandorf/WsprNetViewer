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
    private static final String emptyString = "";
    private static final String oneSpaceString = " ";
    private static final String equalSignString = "=";
    private static final String semicolonString = ";";
    private static final boolean DRUPAL = false; // true= use wsprnet.org drupal database; false= use wsprnet.org old database

    // These are the names of the objects that need to be extracted.
    // column# 0             1       2           3    4       5      6     7          8     9       10
    //   Timestamp	        Call	MHz	        SNR	Drift	Grid	Pwr	Reporter	RGrid	km      az
    //   2014-08-25 20:40 	DL8EDC  7.040186 	-4 	 0   	JO31le 	 5 	 LA3JJ/L 	JO59bh 	925     11

    // Gridsquare information
    static final String WSPRNET_IDX_CITY = "city_name";
    static final String WSPRNET_IDX_COUNTRY_NAME = "name";
    //static final String WSPRNET_IDX_COORD = "coord";
    static final String WSPRNET_IDX_COORD_LAT = "lat";
    static final String WSPRNET_IDX_COORD_LONG = "lon";

    // Wspr information html element indices for their 'new' drupal interface.
    //   e.g.: http://wsprnet.org/drupal/wsprnet/spots
    static final int WSPRNET_IDX_TIMESTAMP = 0;
    static final int WSPRNET_IDX_TX_CALLSIGN = 1;
    static final int WSPRNET_IDX_TX_FREQ_MHZ = 2;
    static final int WSPRNET_IDX_RX_SNR = 3;
    static final int WSPRNET_IDX_RX_DRIFT = 4;
    static final int WSPRNET_IDX_TX_GRIDSQUARE = 5;
    static final int WSPRNET_IDX_TX_POWER = 6;
    static final int WSPRNET_IDX_RX_CALLSIGN = 7;
    static final int WSPRNET_IDX_RX_GRIDSQUARE = 8;
    static final int WSPRNET_IDX_DISTANCE = 9;
    static final int WSPRNET_IDX_AZIMUTH = 10;

    // Wspr information html element indices for their 'old' url query interface.
    //   e.g.: http://wsprnet.org/olddb?mode=html&band=all&limit=10&findcall=&findreporter=&sort=date
    static final int WSPRNET_IDX_OLDDB_TIMESTAMP = 0;
    static final int WSPRNET_IDX_OLDDB_TX_CALLSIGN = 1;
    static final int WSPRNET_IDX_OLDDB_TX_FREQ_MHZ = 2;
    static final int WSPRNET_IDX_OLDDB_RX_SNR = 3;
    static final int WSPRNET_IDX_OLDDB_RX_DRIFT = 4;
    static final int WSPRNET_IDX_OLDDB_TX_GRIDSQUARE = 5;
    static final int WSPRNET_IDX_OLDDB_TX_POWER_DBM = 6;
    static final int WSPRNET_IDX_OLDDB_TX_POWER_W = 7;
    static final int WSPRNET_IDX_OLDDB_RX_CALLSIGN = 8;
    static final int WSPRNET_IDX_OLDDB_RX_GRIDSQUARE = 9;
    static final int WSPRNET_IDX_OLDDB_DISTANCE_KM = 10;
    static final int WSPRNET_IDX_OLDDB_DISTANCE_MILES = 11;

    final int wsprnetIdxTimestamp     = (DRUPAL) ? WSPRNET_IDX_TIMESTAMP     : WSPRNET_IDX_OLDDB_TIMESTAMP;
    final int wsprnetIdxTxCallsign    = (DRUPAL) ? WSPRNET_IDX_TX_CALLSIGN   : WSPRNET_IDX_OLDDB_TX_CALLSIGN;
    final int wsprnetIdxTxFreqMhz     = (DRUPAL) ? WSPRNET_IDX_TX_FREQ_MHZ   : WSPRNET_IDX_OLDDB_TX_FREQ_MHZ;
    final int wsprnetIdxRxSnr         = (DRUPAL) ? WSPRNET_IDX_RX_SNR        : WSPRNET_IDX_OLDDB_RX_SNR;
    final int wsprnetIdxRxDrift       = (DRUPAL) ? WSPRNET_IDX_RX_DRIFT      : WSPRNET_IDX_OLDDB_RX_DRIFT;
    final int wsprnetIdxTxGridsquare  = (DRUPAL) ? WSPRNET_IDX_TX_GRIDSQUARE : WSPRNET_IDX_OLDDB_TX_GRIDSQUARE;
    final int wsprnetIdxTxPowerDbm    = (DRUPAL) ? WSPRNET_IDX_TX_POWER      : WSPRNET_IDX_OLDDB_TX_POWER_DBM;
    final int wsprnetIdxRxCallsign    = (DRUPAL) ? WSPRNET_IDX_RX_CALLSIGN   : WSPRNET_IDX_OLDDB_RX_CALLSIGN;
    final int wsprnetIdxRxGridsquare  = (DRUPAL) ? WSPRNET_IDX_RX_GRIDSQUARE : WSPRNET_IDX_OLDDB_RX_GRIDSQUARE;
    final int wsprnetIdxDistanceKm    = (DRUPAL) ? WSPRNET_IDX_DISTANCE      : WSPRNET_IDX_OLDDB_DISTANCE_KM;
    final int wsprnetIdxAzimuth       = (DRUPAL) ? WSPRNET_IDX_AZIMUTH       : -1; // old database doesn't report azimuth

    // Default interval at which to sync with wsprnet.org, in seconds.
    public static final int SYNC_INTERVAL = 60 * 60;  // 1 hour
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 4; // +/- 15 minutes
    private static final double BandFrequencyTolerancePercent = 5.;
    private static Double[] mBandFrequency, mBandFrequencyMin, mBandFrequencyMax;
    private static String[] mBandFrequencyStr, mBandNameStr;
    private static int mBandNameIdx = -1;

    private Context mContext;

    public WsprNetViewerSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d(LOG_TAG, "Creating SyncAdapter");
        mContext = context;
    }


    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        // TODO:  verify if wsprSpotQuery is no longer needed
        String wsprSpotQuery = emptyString;
            wsprSpotQuery = Utility.getPreferredGridsquare(mContext);
        // If there's no gridsquare code, there's nothing to look up.
        if (wsprSpotQuery.length() == 0) {
            wsprSpotQuery = "empty";
        }

        try {
            // Get data from live from website.
            // For url_wsprnet_spots, drupal must be true!
            // For url_wsprnet_spots_old, drupal must be false!
          //String source = mContext.getString(R.string.url_wsprnet_spots);
            String source = mContext.getString(R.string.url_wsprnet_spots_old);
            Document wsprHtmlDoc = getWsprData(mContext, source);
            // TODO: 'maxSpots' is unused in queries until it is known how to submit a direct Drupal query to wspr web site.
            int maxSpots = 1000;
            getWsprDataFromTags(mContext, wsprHtmlDoc, maxSpots, wsprSpotQuery);
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
            // Allow html to come from the old wsprnet.org database (permits URL parameters) or the newer drupal database.
            // Get wspr info from, e.g., http://www.wsprnet.org/drupal/wsprnet/spots; e.g.:
            wsprHtmlDoc = Jsoup.connect(source).get();
            Log.d("getWsprData", "Connected to ["+source+"]");
        } // try
        catch(Throwable t) {
            t.printStackTrace();
        }
        return wsprHtmlDoc;
    } // getWsprData()

    /**
     * Makes sure the frequency band check arrays are set up.
     * Returns true if they are, false if not.
     */
    private boolean frequencyBandCheckSetup(Context context, double tolerancePercent) {
        boolean setUpOk = false;
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
            setUpOk = true;
        }
        return setUpOk;
    }

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
            return emptyString;
        }
    }

    /**
     * Determines if a frequency is in one of the bands in R.array.pref_notify_band_values.
     * Checks if it is also in the notification band-- if it is, then set mBandNameIdx.
     * @param context
     * @param freqMhz - a frequency in MHz as returned by a specific WSPR report
     * @param tolerancePercent - freqMhz is in the band if within +/-tolerance of the band's center frequency
     * @return Returns -1 if not within a valid frequency band; also sets mBandNameIdx.
     */
    public double getFrequencyBandCheck(Context context, double freqMhz, double tolerancePercent) {
        double band = -1;

        if (frequencyBandCheckSetup(context, tolerancePercent)) {
            if ((mBandFrequency != null) && (mBandFrequency.length > 0)) {
                for (int i = 0; i < mBandFrequency.length; i++) {
                    if ((mBandFrequencyMin[i] <= freqMhz) && (freqMhz <= mBandFrequencyMax[i])) {
                        band = mBandFrequency[i];
                        break;
                    }
                }
            }
        }
        return band;
    }

    /**
     * Checks if frequency is in the notification band; sets mBandNameIdx if it is.
     * @param freqMhz - a frequency in MHz as returned by a specific WSPR report
     * @return Returns false if not within a valid frequency band; otherwise true.
     */
    public boolean frequencyBandNotifyCheck(double freqMhz,
                                        double notifyBandMHzMin, double notifyBandMHzMax) {
        double band = -1;
        boolean ok = false;
        if ((mBandFrequency != null) && (mBandFrequency.length > 0)) {
            for (int i = 0; i < mBandFrequency.length; i++) {
                if ((mBandFrequencyMin[i] <= freqMhz) && (freqMhz <= mBandFrequencyMax[i])) {
                    band = mBandFrequency[i];
                    if ((notifyBandMHzMin <= band) && (band <= notifyBandMHzMax)) {
                        mBandNameIdx = i; // save for getFrequencyBandName()
                        ok = true;
                    }
                    break;
                }
            }
        }
        return ok;
    }


    /**
     * Clean up and parse the raw timestamp from the html.
     * Since there may be multiple records with the same timestamp (the resolution is only 1 minute),
     * make sure there is a mechanism to distinguish each of them.
     */
    public String parseTimestamp(String timestampStr) {
        SimpleDateFormat timestampFormatIn = new SimpleDateFormat(Utility.TIMESTAMP_FORMAT_WSPR);
        // TODO: getting parse exceptions-but maybe only in debug mode; SDF may not be suitable for
        //       use in static modules.
        //       Investigate using something like joda-time: http://www.joda.org/joda-time/
        try {
            Date inputTimestamp = timestampFormatIn.parse(timestampStr);
            inputTimestamp.setTime(inputTimestamp.getTime()); // + millisecondOffset); // can add a fake ms value to make timestamp unique
            return WsprNetContract.getDbTimestampString(inputTimestamp);
        } catch (ParseException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            //e.printStackTrace();
            return timestampStr;
        }
    }

    /**
     * Converts Date class to a string representation, used for easy comparison and database lookup.
     * @param timestamp The input timestamp
     * @return a WSPR-format representation of the timestamp.
     */
    public static long getShortTimestamp(Date timestamp){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid timestamp.
        SimpleDateFormat sdf = new SimpleDateFormat(WsprNetContract.TIMESTAMP_FORMAT_DB_SHORT);
        String s = sdf.format(timestamp);
        long i = 0;
        try {
            i = Long.parseLong(s);
        } catch (Exception e) {
            // nothing to do
            i = -1;
        }
        return i;
    }

    /**
     * Parse the Document containing the wspr data in HTML format.
     * Call from 'doInBackground()', etc.
     */
    public void getWsprDataFromTags(Context context, Document wsprHtml, int maxSpots,
                                           String gridsquareSetting)
            throws Throwable {

        // Notification calculations
        double minSNR  = Utility.getNotifyMinSNR(context);
        double notifyBandMHz = Utility.getNotifyBand(context),
               notifyBandMHzMin = notifyBandMHz - 0.001, notifyBandMHzMax = notifyBandMHz + 0.001;
        if (notifyBandMHz < 0.00001) { // zero is a placeholder menu option for 'any'
            notifyBandMHzMin = 0;
            notifyBandMHzMax = 1e300;
        }
        mBandNameIdx = -1; // reset which band was found for notification
        int nHits = 0, nHitsSnr = 0, nHitsBand = 0, nHitsDistance = 0, nHitsTxCall = 0, nHitsRxCall = 0, nHitsTxGrid = 0, nHitsRxGrid = 0;
        double notifyMinTxRxKm = Utility.getNotifyTxRxKm(context);
        // Get the tx/rx notify callsigns, but configure for wildcard matching with regex's.
        String displayTxCallsign = Utility.getNotifyCallsign(context, true),
               displayRxCallsign = Utility.getNotifyCallsign(context, false);
        String displayTxGridsquare = Utility.getNotifyGridsquare(context, true),
               displayRxGridsquare = Utility.getNotifyGridsquare(context, false);
        String notifyTxCallsign = Utility.filterCleanupMatch(displayTxCallsign),
                notifyRxCallsign = Utility.filterCleanupMatch(displayRxCallsign);
        String notifyTxGridsquare = Utility.filterCleanupMatch(displayTxGridsquare),
                notifyRxGridsquare = Utility.filterCleanupMatch(displayRxGridsquare);
        boolean snrOk = false, bandOk = false, distanceOk = false, txCallOk = false, rxCallOk = false, txGridOk = false, rxGridOk = false;
        boolean snrEna = true,
                bandEna = (notifyBandMHz > 0.00001),
                distanceEna = (notifyMinTxRxKm >= 0.001),
                txCallEna = (notifyTxCallsign.length() > 0),
                rxCallEna = (notifyRxCallsign.length() > 0),
                txGridEna = (notifyTxGridsquare.length() > 0),
                rxGridEna = (notifyRxGridsquare.length() > 0);

        // Delete items older than the cutoff period specified in the settings menu.
        // TODO: It might be easier to use System.currentTimeMillis(), which returns time in UTC.  BUT,
        // todo: Date objects seem to work in the local time zone; wasn't able to initialize one to UTC.
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        TimeZone tz = TimeZone.getDefault();
        int offsetUTC = tz.getOffset(cal.getTimeInMillis()) / 1000;
        int seconds = Utility.cutoffSeconds(context);
        cal.add(Calendar.SECOND, -offsetUTC); // current UTC time
        cal.add(Calendar.SECOND, -seconds);   // cutoff time-- ignore items older than <user preference>
        String cutoffTimestamp = WsprNetContract.getDbTimestampString(cal.getTime());
        int d;
        d = context.getContentResolver().delete(WsprNetContract.SignalReportEntry.CONTENT_URI,
                WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT + " <= ?",
                new String[]{cutoffTimestamp});
        Log.v(LOG_TAG, "getWsprDataFromTags: deleted " + Integer.toString(d) + " old items.");

        // Get the cutoff date for notifications (current time +/- the update interval.)
        cal.setTime(new Date());
        seconds = Utility.updateIntervalSeconds(context);
        cal.add(Calendar.SECOND, -offsetUTC); // current UTC time
        cal.add(Calendar.SECOND, -seconds);   // cutoff time-- ignore items older than the update interval
        long cutoffNotifyTimeMin = getShortTimestamp(cal.getTime());
        cal.add(Calendar.SECOND, 2*seconds);
        long cutoffNotifyTimeMax = getShortTimestamp(cal.getTime());
        long iTimestamp = 0;

        try {
            // TODO: get city name, lat/long from gridsquare; determine how to look this up
            String cityName = context.getString(R.string.unknown_city); // generic text until the city/country is looked up
            String countryName = context.getString(R.string.unknown_country);
            double cityLatitude = Utility.gridsquareToLatitude(gridsquareSetting);
            double cityLongitude = Utility.gridsquareToLongitude(gridsquareSetting);

            Log.v(LOG_TAG, cityName + ", with coord: " + cityLatitude + " " + cityLongitude);

            // Insert the gridsquare into the database.
            long locationID = addGridsquare(context, gridsquareSetting, cityName, countryName, cityLatitude, cityLongitude);
            //Elements wsprHeader, wsprHeader1, wsprHeader2; // TODO: someday, match up header name instead of relying on a fixed column #
            Elements wsprData;
            wsprData = getWsprData(wsprHtml, DRUPAL);

            // Get and insert the new wspr information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(wsprData.size());

            for (int i = 0; (i < wsprData.size()) && (i < maxSpots); i++) {
                Elements wsprTDRow = wsprData.get(i).select("td");  // table data row split into <td> elements
                // These are the values that will be collected.
                // column# 0             1       2           3    4       5      6     7          8     9       10
                //   Timestamp	        Call	MHz	        SNR	Drift	Grid	Pwr	Reporter	RGrid	km      az

                String timestamp, txCallsign, txGridsquare, rxCallsign, rxGridsquare;
                Double txFreqMhz, rxSnr, rxDrift, txPower, kmDistance, azimuth;
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
                timestamp = parseTimestamp(wsprTDRow.get(wsprnetIdxTimestamp).text().replace(Utility.NBSP, ' ').replace(" .", ".").replace(".0000", emptyString).trim());
                txCallsign = wsprTDRow.get(wsprnetIdxTxCallsign).text().replace(Utility.NBSP, ' ').trim().toUpperCase();
                txFreqMhz = Double.parseDouble(wsprTDRow.get(wsprnetIdxTxFreqMhz).text().replace(Utility.NBSP, ' ').trim());
                rxSnr = Double.parseDouble(wsprTDRow.get(wsprnetIdxRxSnr).text().replace(Utility.NBSP, ' ').trim());
                rxDrift = Double.parseDouble(wsprTDRow.get(wsprnetIdxRxDrift).text().replace(Utility.NBSP, ' ').trim());
                txGridsquare = wsprTDRow.get(wsprnetIdxTxGridsquare).text().replace(Utility.NBSP, ' ').trim(); // mixed case!
                txPower = Double.parseDouble(wsprTDRow.get(wsprnetIdxTxPowerDbm).text().replace(Utility.NBSP, ' ').trim());
                rxCallsign = wsprTDRow.get(wsprnetIdxRxCallsign).text().replace(Utility.NBSP, ' ').trim().toUpperCase();
                rxGridsquare = wsprTDRow.get(wsprnetIdxRxGridsquare).text().replace(Utility.NBSP, ' ').trim(); // mixed case!
                kmDistance = Double.parseDouble(wsprTDRow.get(wsprnetIdxDistanceKm).text().replace(Utility.NBSP, ' ').trim());
                // Don't bother with 'miles'.
                //miDistance = Double.parseDouble(wsprTDRow.get(WSPRNET_IDX_OLDDB_DISTANCE_MILES).text().replace(Utility.NBSP, ' ').trim());
                // Azimuth is not provided in old database; calculate it ourselves for both old and new (drupal) databases.
                //azimuth = Double.parseDouble(wsprTDRow.get(wsprnetIdxAzimuth).text().replace(Utility.NBSP, ' ').trim());
                azimuth = Utility.latLongToAzimuth(Utility.gridsquareToLatitude(txGridsquare), Utility.gridsquareToLongitude(txGridsquare),
                                                   Utility.gridsquareToLatitude(rxGridsquare), Utility.gridsquareToLongitude(rxGridsquare));

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
                try {
                    iTimestamp = Long.parseLong(timestamp.substring(0, WsprNetContract.TIMESTAMP_FORMAT_DB_SHORT.length()));
//                    if (   (cutoffNotifyTimeMin > 0) && (cutoffNotifyTimeMax > 0)
//                        && (cutoffNotifyTimeMin <= iTimestamp) && (iTimestamp < cutoffNotifyTimeMax)) {
                    if (isWithinTimeInterval(cutoffNotifyTimeMin, cutoffNotifyTimeMax, iTimestamp)) {
                        // getFrequencyBandCheck() will check what band the TX frequency is in.
                        // frequencyBandNotifyCheck will check if it is in the notification band.
                        double bandMHz = getFrequencyBandCheck(context, txFreqMhz, BandFrequencyTolerancePercent);
//                        bandOk = !bandEna || frequencyBandNotifyCheck(bandMHz, notifyBandMHzMin, notifyBandMHzMax);
//                        snrOk = !snrEna || (rxSnr >= minSNR);
//                        distanceOk = !distanceEna || (kmDistance >= notifyMinTxRxKm);
//                        txCallOk = !txCallEna || txCallsign.matches(notifyTxCallsign);
//                        rxCallOk = !rxCallEna || rxCallsign.matches(notifyRxCallsign);
//                        txGridOk = !txGridEna || txGridsquare.toUpperCase().matches(notifyTxGridsquare);
//                        rxGridOk = !rxGridEna || rxGridsquare.toUpperCase().matches(notifyRxGridsquare);
                        // Trivial mechanism to reduce cyclomatic complexity.
                        bandOk     = evaluateOR(!bandEna, frequencyBandNotifyCheck(bandMHz, notifyBandMHzMin, notifyBandMHzMax));
                        snrOk      = evaluateOR(!snrEna, (rxSnr >= minSNR));
                        distanceOk = evaluateOR(!distanceEna, (kmDistance >= notifyMinTxRxKm));
                        txCallOk   = evaluateOR(!txCallEna, txCallsign.matches(notifyTxCallsign));
                        rxCallOk   = evaluateOR(!rxCallEna, rxCallsign.matches(notifyRxCallsign));
                        txGridOk   = evaluateOR(!txGridEna, txGridsquare.toUpperCase().matches(notifyTxGridsquare));
                        rxGridOk   = evaluateOR(!rxGridEna, rxGridsquare.toUpperCase().matches(notifyRxGridsquare));


                        // TODO:  is the Java compiler inlining this?  It's in a loop processing lots of data!
                        if (checkNotifyConditions(bandOk, snrOk, distanceOk, txCallOk, rxCallOk, txGridOk, rxGridOk)) {
                            nHits++;
                            // This used to read, e.g.,
                            //   nHitsBand += (bandEna && bandOk) ? 1 : 0;
                            // This reduces the cyclomatic complexity.
                            // TODO:  is the Java compiler inlining this?  It's in a loop processing lots of data!
                            nHitsBand += convertBooleanANDPairToInt(bandEna, bandOk);
                            nHitsSnr += convertBooleanANDPairToInt(snrEna, snrOk);
                            nHitsDistance += convertBooleanANDPairToInt(distanceEna, distanceOk);
                            nHitsTxCall += convertBooleanANDPairToInt(txCallEna, txCallOk);
                            nHitsRxCall += convertBooleanANDPairToInt(rxCallEna, rxCallOk);
                            nHitsTxGrid += convertBooleanANDPairToInt(txGridEna, txGridEna);
                            nHitsRxGrid += convertBooleanANDPairToInt(rxGridEna, rxGridEna);
                        }
                    }
                } catch (Exception e) {
                  // nothing to do
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
            cal.setTime(new Date());
            cal.add(Calendar.HOUR, 24);
            String tomorrowTimestamp = WsprNetContract.getDbTimestampString(cal.getTime());
            d = context.getContentResolver().delete(WsprNetContract.SignalReportEntry.CONTENT_URI,
                  WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT + " > ?",
                  new String[]{tomorrowTimestamp});
            Log.v(LOG_TAG, "getWsprDataFromTags: deleted " + Integer.toString(d) + " invalid items.");

            // Did any reports meet the notification criteria?
            if (nHits > 0) {
                String bandName = emptyString;
                String description = emptyString;
                description = createNotifyDescription(context, notifyMinTxRxKm,
                                nHitsTxCall, nHitsRxCall, nHitsTxGrid, nHitsRxGrid, nHitsDistance,
                                displayTxCallsign, displayRxCallsign,
                                displayTxGridsquare, displayRxGridsquare);
                bandName = createNotifyBandName(context, notifyBandMHz);
                notifyWspr(context, bandName, description, minSNR);
            }

        } catch(Exception e) {
            Log.d(LOG_TAG, "getWsprDataFromTags exception: " + e.toString());

        }
    } // getWsprDataFromTags()

    private boolean isWithinTimeInterval(long timeMin, long timeMax, long timestamp) {
        return (   (timeMin > 0) && (timeMax > 0)
                && (timeMin <= timestamp) && (timestamp < timeMax));
    }
    private boolean checkNotifyConditions(boolean bandOk, boolean snrOk, boolean distanceOk,
                                          boolean txCallOk, boolean rxCallOk, boolean txGridOk,
                                          boolean rxGridOk) {
        return bandOk && snrOk && distanceOk && txCallOk && rxCallOk && txGridOk && rxGridOk;
    }

    private String createNotifyBandName(Context context, double notifyBandMHz) {
        String bandName;
        if (notifyBandMHz < 0.00001) {
            bandName = "---";
        } else {
            bandName = getFrequencyBandName(context, mBandNameIdx);
        }
        return bandName;
    }

    private String createNotifyDescription(Context context, double notifyMinTxRxKm,
                                           int nHitsTxCall, int nHitsRxCall,
                                           int nHitsTxGrid, int nHitsRxGrid, int nHitsDistance,
                                           String displayTxCallsign, String displayRxCallsign,
                                           String displayTxGridsquare, String displayRxGridsquare) {
        String description = emptyString;

        if (nHitsTxCall > 0) {
            description += oneSpaceString + context.getString(R.string.pref_filter_label_tx_callsign) + equalSignString + displayTxCallsign + semicolonString;
        }
        if (nHitsRxCall > 0) {
            description += oneSpaceString + context.getString(R.string.pref_filter_label_rx_callsign) + equalSignString + displayRxCallsign + semicolonString;
        }
        if (nHitsTxGrid > 0) {
            description += oneSpaceString + context.getString(R.string.pref_filter_label_tx_gridsquare) + equalSignString + displayTxGridsquare + semicolonString;
        }
        if (nHitsRxGrid > 0) {
            description += oneSpaceString + context.getString(R.string.pref_filter_label_rx_gridsquare) + equalSignString + displayRxGridsquare + semicolonString;
        }
        if (nHitsDistance > 0) {
            // TODO: Display either km or miles.  See SettingsActivity.java, onPreferenceChange().
            description += " distance>=" + Utility.formatDistance(context, notifyMinTxRxKm, Utility.isMetric(context) ) + "km;";
        }
        return description;
    }

    // Extract the WSPR data elements from the HTML page.
    private Elements getWsprData(Document wsprHtml, boolean drupal) {
    Elements wsprData;
    if (drupal == true) {
        //wsprHeader = wsprHtml.select("div#block-system-main.block.block-system div.content table tbody tr:eq(0)"); // examine the header
        wsprData = wsprHtml.select("div#block-system-main.block.block-system div.content table tbody tr:gt(0)");
    } else {
        //wsprHeader1 = wsprHtml.select("html body table tbody tr:eq(0)"); // examine the header, first row
        //wsprHeader2 = wsprHtml.select("html body table tbody tr:eq(1)"); // examine the header, second row
        wsprData   = wsprHtml.select("html body table tbody tr:gt(1)");  // remaining rows
    }
    return wsprData;
}

    // Converts boolean AND to: false=0, true=1
    // Trivial method to reduce cyclomatic complexity.
    private int convertBooleanANDPairToInt(boolean b1, boolean b2) {
        return (b1 && b2) ? 1 : 0;
    }
    // Converts boolean OR to boolean
    // Trivial method to reduce cyclomatic complexity.
    private boolean evaluateOR(boolean b1, boolean b2) {
        return (b1 || b2);
    }
    
    
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

            if ((System.currentTimeMillis() - lastNotification) >= prefMillis) {
                // It's been long enough since the last notification; send a new one now.

                int iconId = Utility.getIconResourceForWsprCondition(snr, false);
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
        boolean ret = accountManager.addAccountExplicitly(newAccount, emptyString, null);
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
