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
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.glandorf1.joe.wsprnetviewer.app.data.WsprNetContract;
import com.glandorf1.joe.wsprnetviewer.app.data.WsprNetDbHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Utility {
    private static final String LOG_TAG = Utility.class.getSimpleName();
    private static final int WSPR_NOTIFICATION_ID = 3004; // TODO: is NOTIFICATION_ID chosen at random?
    private static final String SPACES = "            ";
    public static final char NBSP = 160; // non-blank space character
    // Format used for incoming timestamps, and for friendly representation for display.
    public static final String TIMESTAMP_FORMAT_WSPR = "yyyy-MM-dd HH:mm"; // format of the timestamp data from wsprnet.org
    public static final String TIMESTAMP_FORMAT_HOURS_MINUTES = "HH:mm"; // use "EEE MMM dd" "Day Month day#": Mon Sep 1
    public static final int MAIN_DISPLAY_GRIDSQUARE = 0x01, MAIN_DISPLAY_CALLSIGN = 0x02, MAIN_DISPLAY_GRIDCALL = 0x03;

    // TODO: consolidate this with similar code in the sync adapter
    private static final double mBandFrequencyTolerancePercent = 5.;
    private static String[] mBandFrequencyMin, mBandFrequencyMax;
    private static String[] mBandFrequencyStr, mBandFrequencyKeysStr;


    // Delete all database records.
    public static void deleteAllRecords(Context context) {
        context.getContentResolver().delete(
                WsprNetContract.SignalReportEntry.CONTENT_URI, null, null);
        context.getContentResolver().delete(
                WsprNetContract.GridSquareEntry.CONTENT_URI, null, null);
    }

    // For debugging- dump database to a file.
    //    On device, open OIFileManager, navigate to
    //      home/storage/emulated/0
    //    Touch and hold wspr.db until it is highlighted
    //    From the ActionBar menu, click Send...Gmail, then email the file.
    //    On PC, download wspr.db from email, open it with sqliteBrowser
    //      "C:\Program Files (x86)\SqliteBrowser3\bin\sqlitebrowser.exe"
    public static void exportDB(Context context){
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source=null;
        FileChannel destination=null;
        String currentDBPath = "/data/"+ context.getPackageName() +"/databases/"+ WsprNetDbHelper.DATABASE_NAME;
        currentDBPath = "/data/"+ context.getPackageName() +"/databases/"+ WsprNetDbHelper.DATABASE_NAME;
        String backupDBPath = WsprNetDbHelper.DATABASE_NAME;
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Toast.makeText(context, "DB Exported to: '" + backupDB.getAbsolutePath() + "'", Toast.LENGTH_LONG).show();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the gridsquare entered on the settings screen.
     * @param context
     * @return
     */
    public static String getPreferredGridsquare(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_gridsquare_key), context.getString(R.string.pref_gridsquare_default));
    }

    // Pad "zero" characters to end of gridsquare.
    public static String fixupGridsquare(final String gridsquareIn) {
        final String nullGridsquare = "AA55kk";
        if (gridsquareIn.length() < nullGridsquare.length()) {
            return gridsquareIn +  nullGridsquare.substring(gridsquareIn.length());
        } else {
            return gridsquareIn;
        }
    }
    // See http://en.wikipedia.org/wiki/Maidenhead_Locator_System
    // A shorthand latitude/longitude representation:
    //   JO31le
    //   J - base 18 ('A' - 'R') longitude, 20 degree increments, measured eastward from Greenwich
    //    O - base 18 ('A' - 'R') latitude, 10 degree increments, measured from south to north pole
    //     3 - base 10 longitude fractions, 2 degree increments
    //      1 - base 10 latitude fractions, 1 degree increments
    //       l - ('el') base 24 ('a' - 'x') longitude, 5-minute increments
    //        e -       base 24 ('a' - 'x') latitude, 2.5-minute increments
    /**
     * @param gridsquareIn
     *          maidenhead locator string to be converted
     *          Ex.:  JO31le --> JO31LE -->  -90 + 10 * ('O' - 'A')) --> -90 + 10*14  -->  51.187  *                                      +        (1*('1' - '0'))     + 1*1
     *                                      + 2.5 / 60 *('E' - 'A')      + 2.5/60*4
     *                                      + 2.5 / 60 / 2               + 2.5/60/2
     * @return latitude
     */
    public static double gridsquareToLatitude(final String gridsquareIn) {
        String gridsquare = fixupGridsquare(gridsquareIn).toUpperCase();
        double latitude = 0;
        try {
            latitude = -90 + 10 * (gridsquare.charAt(1) - 'A')
                    + 1 * (gridsquare.charAt(3) - '0')
                    + 2.5 / 60 * (gridsquare.charAt(5) - 'A')
                    + 2.5 / 60 / 2;
        } catch (StringIndexOutOfBoundsException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return latitude; // Latitude.fromDegrees(latitude);
    }

    /**
     * @param gridsquareIn
     *          maidenhead locator string to be converted
     *          Ex.:  JO31le --> JO31LE -->  -180 + 20 * ('J' - 'A')) --> -180 + 20*9  --> 6.958   *                                      +         (2*('3' - '0'))     + 2*3
     *                                      + 5.0 / 60 * ('L' - 'A')      + 5/60*11
     *                                      + 5.0 / 60 / 2                + 5/60/2
     * @return longitude
     */
    public static double gridsquareToLongitude(final String gridsquareIn) {
        String gridsquare = fixupGridsquare(gridsquareIn).toUpperCase();
        double longitude = 0;
        try {
            longitude =     -180 + 20 * (gridsquare.charAt(0) - 'A')
                            + 2 * (gridsquare.charAt(2) - '0')
                            + 5.0 / 60 * (gridsquare.charAt(4) - 'A')
                            + 5.0 / 60 / 2;
        } catch (StringIndexOutOfBoundsException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return longitude; // Longitude.fromDegrees(longitude);
    }

    public static String coordToGridsquare(double latitude, double longitude) {
        String gridsquare = "";
        longitude = longitude + 180.;
        latitude = latitude + 90.;

        gridsquare += (char)((int)'A' + (int)(longitude / 20.));
        gridsquare += (char)((int)'A' + (int)(latitude / 10.));
        gridsquare += (char)((int)'0' + (int)((longitude % 20.)/2.));
        gridsquare += (char)((int)'0' + (int)((latitude % 10.)/1.));
        gridsquare += (char)((int)'a' + (int)((longitude - ((int)(longitude/2.)*2.)) / (5./60.)));
        gridsquare += (char)((int)'a' + (int)((latitude - ((int)(latitude/1)*1)) / (2.5/60.)));
        return gridsquare;
    }

    /** Find azimuth of the great circle given by a pair of latitude/longitude coordinates.
     *
     * @param lat1 - latitude  for point #1
     * @param long1- longitude for point #1
     * @param lat2 - latitude  for point #2
     * @param long2- longitude for point #2
     * @return azimuth
     */
    public static double latLongToAzimuth(double lat1, double long1, double lat2, double long2) {
        double pi = 3.141592653589793;
        double  r = 6371.; // Earth's radius in km
        lat1 = pi * lat1 / 180.;
        lat2 = pi * lat2 / 180.;
        long1 = pi * long1 / 180.;
        long2 = pi * long2 / 180.;
        double azimuth = 0;
//        // See http://www.movable-type.co.uk/scripts/latlong.html
//        //   θ = atan2( sin Δλ ⋅ cos φ2 , cos φ1 ⋅ sin φ2 − sin φ1 ⋅ cos φ2 ⋅ cos Δλ )
//        //   φ is latitude, λ is longitude, R is earth’s radius (mean radius = 6,371km)
        // This gives the same result as above; can optionally calculate distance.
        // See http://www.codeguru.com/cpp/cpp/algorithms/article.php/c5115/Geographic-Distance-and-Azimuth-Calculations.htm
        try {
            double deg90 = pi / 2; // 90 degrees, in radians
            double b = Math.acos(Math.cos(deg90 - lat2) * Math.cos(deg90 - lat1) + Math.sin(deg90 - lat2) * Math.sin(deg90 - lat1) * Math.cos(long2 - long1));
            //double d = r * b; // optionally calculate distance
            double sinb = Math.sin(b);
            if (Math.abs(sinb) > 0.000000000001) {
                azimuth = 180. * Math.asin(Math.sin(deg90 - lat2) * Math.sin(long2 - long1) / sinb) / pi;
                azimuth = (azimuth + 360.) % 360.;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return azimuth;
    } // latLongToAzimuth()

    // from http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the enum corresponding to the main display preference.
     */
    public static int getMainDisplayPreference(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sOption = prefs.getString(context.getString(R.string.pref_main_display_key),
                context.getString(R.string.pref_main_display_gridsquare));
        return (int)(Integer.parseInt(sOption));
    }


    /**
     * Returns true if filters are enabled.
     */
    public static boolean isFiltered(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.pref_filter_enable_key),
                Boolean.parseBoolean(context.getString(R.string.pref_filter_enable_default)));
    }

    /**
     * Returns true if filters are to be ANDed, instead of OR'd.
     */
    public static boolean isFilterAnd(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.pref_filter_key_match_all),
                                Boolean.parseBoolean(context.getString(R.string.pref_filter_match_all_default)));
    }

    /**
     * Returns the TX or RX callsign filter from the preferences, or empty string if not set.
     * @param context
     * @param isTx
     * @return
     */
    public static String getFilterCallsign(Context context, boolean isTx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (isTx) {
            return prefs.getString(context.getString(R.string.pref_filter_key_tx_callsign), "");
        } else {
            return prefs.getString(context.getString(R.string.pref_filter_key_rx_callsign), "");
        }
    }

    /**
     * Returns the TX or RX gridsquare filter from the preferences, or empty string if not set.
     * @param context
     * @param isTx
     * @return
     */
    public static String getFilterGridsquare(Context context, boolean isTx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (isTx) {
            return prefs.getString(context.getString(R.string.pref_filter_key_tx_gridsquare), "");
        } else {
            return prefs.getString(context.getString(R.string.pref_filter_key_rx_gridsquare), "");
        }
    }

    /**
     * Returns the band (wavelength) filter from the preferences, or empty string if not set.
     * @param context
     * @return
     */
    public static String getFilterBand(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_filter_band_key), "");
    }

    /**
     * Trim, convert to upper case, replace wildcard characters with the SQLite wildcards,
     * and removes invalid characters from a filter:  anything other than A-Za-z0-9/%_
     */
    public static String filterCleanupForSQL(String filter) {
        return filter.trim().toUpperCase().replace('*', '%').replace('?', '_').replaceAll("[^A-Za-z0-9/%_]", "");
    }

    /**
     * Trim, convert to upper case, replace wildcard characters with the match() wildcards,
     * and removes invalid characters from a filter:  anything other than A-Za-z0-9/%_
     */
    public static String filterCleanupMatch(String filter) {
        return filter.trim().toUpperCase().replace("?", ".?").replace("*", ".*").replaceAll("[^A-Za-z0-9/\\.\\*]", "");
    }

    /**
     * getFilterSelectionStringForSql
     * Based on the user filter preferences, return a SQL 'selection' string.
     * The user preference pref_filter_enable_key is NOT checked here.
     */
    public static String getFilterSelectionStringForSql(Context context) {
        String mSelection;
        String txCall = Utility.getFilterCallsign(context, true),
                rxCall = Utility.getFilterCallsign(context, false);
        String txGridsquare = Utility.getFilterGridsquare(context,  true),
                rxGridsquare = Utility.getFilterGridsquare(context, false);
        txCall = Utility.filterCleanupForSQL(txCall);
        rxCall = Utility.filterCleanupForSQL(rxCall);
        txGridsquare = Utility.filterCleanupForSQL(txGridsquare);
        rxGridsquare = Utility.filterCleanupForSQL(rxGridsquare);
        mSelection = "";
        // When adding filters, be sure to update onResume(), and save the preference value below, too.
        String prefAndOr = Utility.isFilterAnd(context) ? " and " : " or ";
        String sAndOr = " ";
        if (txCall.length() > 0) {
            sAndOr = (mSelection.length() > 0) ? prefAndOr : "";
            mSelection += sAndOr + "(" + WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN + " like '" + txCall + "')";
        }
        if (rxCall.length() > 0) {
            sAndOr = (mSelection.length() > 0) ? prefAndOr : "";
            mSelection += sAndOr + "(" + WsprNetContract.SignalReportEntry.COLUMN_RX_CALLSIGN + " like '" + rxCall + "')";
        }
        if (txGridsquare.length() > 0) {
            sAndOr = (mSelection.length() > 0) ? prefAndOr : "";
            mSelection += sAndOr + "(" + WsprNetContract.SignalReportEntry.COLUMN_TX_GRIDSQUARE + " like '" + txGridsquare + "')";
        }
        if (rxGridsquare.length() > 0) {
            sAndOr = (mSelection.length() > 0) ? prefAndOr : "";
            mSelection += sAndOr + "(" + WsprNetContract.SignalReportEntry.COLUMN_RX_GRIDSQUARE + " like '" + rxGridsquare + "')";
        }
        // Examples of resulting 'selection' clause:
        //   tx_callsign like 'D%'
        //   (tx_gridsquare like 'D%') and (rx_callsign like 'N%')
        //   (tx_gridsquare like 'D%') or (rx_callsign like 'N%')
        return mSelection;
    }
    /**
     * getMapsFilterSelectionStringForSql
     * Based on the user filter preferences, return a SQL 'selection' string.
     * The user preference pref_filter_enable_key is NOT checked here.
     */
    public static String getMapsFilterSelectionStringForSql(Context context) {
        String mSelection;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String txCall = prefs.getString(context.getString(R.string.pref_filter_map_key_tx_callsign), ""),
                rxCall = prefs.getString(context.getString(R.string.pref_filter_map_key_rx_callsign), "");
        String txGridsquare = prefs.getString(context.getString(R.string.pref_filter_map_key_tx_gridsquare), ""),
                rxGridsquare = prefs.getString(context.getString(R.string.pref_filter_map_key_rx_gridsquare), "");
        txCall = Utility.filterCleanupForSQL(txCall);
        rxCall = Utility.filterCleanupForSQL(rxCall);
        txGridsquare = Utility.filterCleanupForSQL(txGridsquare);
        rxGridsquare = Utility.filterCleanupForSQL(rxGridsquare);
        mSelection = "";
        boolean mFilterAnd = prefs.getBoolean(context.getString(R.string.pref_filter_map_key_match_all),
                Boolean.parseBoolean(context.getString(R.string.pref_filter_match_all_default)));
        // When adding filters, be sure to update onResume(), and save the preference value below, too.
        String prefAndOr = mFilterAnd ? " and " : " or ";
        String sAndOr = " ";
        if (txCall.length() > 0) {
            sAndOr = (mSelection.length() > 0) ? prefAndOr : "";
            mSelection += sAndOr + "(" + WsprNetContract.SignalReportEntry.COLUMN_TX_CALLSIGN + " like '" + txCall + "')";
        }
        if (rxCall.length() > 0) {
            sAndOr = (mSelection.length() > 0) ? prefAndOr : "";
            mSelection += sAndOr + "(" + WsprNetContract.SignalReportEntry.COLUMN_RX_CALLSIGN + " like '" + rxCall + "')";
        }
        if (txGridsquare.length() > 0) {
            sAndOr = (mSelection.length() > 0) ? prefAndOr : "";
            mSelection += sAndOr + "(" + WsprNetContract.SignalReportEntry.COLUMN_TX_GRIDSQUARE + " like '" + txGridsquare + "')";
        }
        if (rxGridsquare.length() > 0) {
            sAndOr = (mSelection.length() > 0) ? prefAndOr : "";
            mSelection += sAndOr + "(" + WsprNetContract.SignalReportEntry.COLUMN_RX_GRIDSQUARE + " like '" + rxGridsquare + "')";
        }
        // Examples of resulting 'selection' clause:
        //   tx_callsign like 'D%'
        //   (tx_gridsquare like 'D%') and (rx_callsign like 'N%')
        //   (tx_gridsquare like 'D%') or (rx_callsign like 'N%')
        return mSelection;
    }

    /**
     * Based on the user filter preferences, return a SQL 'selection' string for the frequency.
     * The user preference pref_filter_enable_key is NOT checked here.
     * @param context
     * @param tolerancePercent - freqMhz is in the band if within +/-tolerance of the band's center frequency
     * @return Returns -1 if not within a valid frequency band; also sets mBandNameIdx.
     */
    public static String getFilterBandSelectionStringForSql(Context context, double tolerancePercent, boolean wsprNotMaps) {
        String mSelection = "";
        if ((mBandFrequencyStr == null) || (mBandFrequencyStr.length <= 0)) {
            mBandFrequencyStr = context.getResources().getStringArray(R.array.pref_notify_band_values);
            mBandFrequencyKeysStr = context.getResources().getStringArray(R.array.pref_notify_band_options);
        }
        if ((mBandFrequencyStr != null) && mBandFrequencyStr.length > 0) {
            if ((mBandFrequencyMin == null) || (mBandFrequencyMin.length <= 0)) {
                mBandFrequencyMin = new String[mBandFrequencyStr.length];
                mBandFrequencyMax = new String[mBandFrequencyStr.length];
                if ((tolerancePercent < 1.) || (tolerancePercent > 20.)) {
                    tolerancePercent = 5.;
                }
                // Set up the min/max frequency limits for each band-- nominally +/-5% around center frequency.
                tolerancePercent = tolerancePercent / 100.;
                for (int i = 0; i < mBandFrequencyStr.length; i++) {
                    Double freq = 0., min = 0., max = 0.;
                    try {
                        freq = Double.parseDouble(mBandFrequencyStr[i]);
                        min = freq - (freq * tolerancePercent);
                        max = freq + (freq * tolerancePercent);
                        mBandFrequencyMin[i] = min.toString();
                        mBandFrequencyMax[i] = max.toString();
                    } catch (NumberFormatException e) {
                        mBandFrequencyMin[i] = "";
                        mBandFrequencyMax[i] = "";
                    };
                }
            }
        }

        // Get the current boolean preference for each frequency band.
        if (   (mBandFrequencyMin != null) && (mBandFrequencyMin.length > 0)
            && (mBandFrequencyMax != null) && (mBandFrequencyMax.length > 0) ) {
            String colFreq = WsprNetContract.SignalReportEntry.COLUMN_TX_FREQ_MHZ;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int count = 0, countMax = 0, i;
            String[] wsprValues = null;
            boolean checked;
            if (wsprNotMaps) {
                // WSPR band preferences come back as a single string that is parsed into a string array.
                CharSequence sPrefs = prefs.getString(context.getString(R.string.pref_filter_band_key), "");
                wsprValues = ListPreferenceMultiSelect.parseStoredValue(sPrefs); // get list of 'checked' prefs
            }
            // For each potential frequency band option, check if there is a preference checked off for it.
            for (i = 0; i < mBandFrequencyMin.length; i++) {
                if (   (mBandFrequencyMin[i].length() > 0)
                        && (mBandFrequencyMax[i].length() > 0)
                        && (!mBandFrequencyMax[i].equals("0.0"))) {
                    countMax++;
                    checked = false;
                    if (wsprNotMaps) {
                        if ((wsprValues == null) || (wsprValues.length == 0)) {
                            break;
                        }
                        // scan the preference array
                        for (String preference: wsprValues) {
                            if ((preference != null) && (mBandFrequencyStr[i] != null) && mBandFrequencyStr[i].equals(preference)) {
                                checked = true;
                                break;
                            }
                        } // option values
                    } else {
                        // Maps band preferences are stored individually.
                        // Tag is the key prefix and option suffix:
                        //   "pref_map_band_any"
                        //   "pref_map_band_17m"
                        String key = context.getString(R.string.pref_map_band_key_) + mBandFrequencyKeysStr[i];
                        boolean def = false; // Boolean.parseBoolean(getActivity().getString(R.string.pref_filter_enable_default));
                        checked = prefs.getBoolean(key, def);
                    }
                    if (checked) {
                        count++;
                        // Example of resulting 'selection' clause:
                        //   (freqMin >= mhz) and (mhz <= freqMax)
                        //   (((3.515 >= mhz) and (mhz <= 3.885)))
                        //   (((3.515 >= mhz) and (mhz <= 3.885)) or ((138.7 >= mhz) and (mhz <= 153.3)))
                        String sel = "((" + mBandFrequencyMin[i] + " <= " + colFreq + ") and (" + colFreq + "<=" + mBandFrequencyMax[i] + "))";
                        if (mSelection.length() <= 0) {
                            mSelection = sel;
                        } else {
                            mSelection += " or " + sel;
                        }
                    }
                }
            }
            if (count == countMax) {
                mSelection = "";  // all items checked ==> turn off filter
            }
        }
        if (mSelection.length() > 0) {
            mSelection = "(" + mSelection + ")";
        }
        return mSelection;
    }

    /**
     * Returns true if metric unit should be used, or false if
     * english units should be used.
     */
    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // ToDo: use strings from strings.xml
        return prefs.getString(context.getString(R.string.pref_units_key), context.getString(R.string.pref_units_metric)).equals(context.getString(R.string.pref_units_metric));
    }

    /**
     * Returns the minimum SNR for wspr notifications.
     */
    public static int getNotifyMinSNR(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sBandId = prefs.getString(context.getString(R.string.pref_notify_min_snr_key),
                context.getString(R.string.pref_min_snr_value_p0));
        return (int)(Integer.parseInt(sBandId));
    }

    /**
     * Returns the frequency band-of-interest for wspr notifications.
     */
    public static double getNotifyBand(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sBandId = prefs.getString(context.getString(R.string.pref_notify_band_key),
                context.getString(R.string.pref_band_value_40m));
        return (double)(Double.parseDouble(sBandId));
    }

    /**
     * Returns the TX or RX callsign notify filter from the preferences, or empty string if not set.
     * @param context
     * @param isTx
     * @return
     */
    public static String getNotifyCallsign(Context context, boolean isTx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (isTx) {
            return prefs.getString(context.getString(R.string.pref_notify_key_tx_callsign), "");
        } else {
            return prefs.getString(context.getString(R.string.pref_notify_key_rx_callsign), "");
        }
    }

    /**
     * Returns the TX or RX gridsquare notify filter from the preferences, or empty string if not set.
     * @param context
     * @param isTx
     * @return
     */
    public static String getNotifyGridsquare(Context context, boolean isTx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (isTx) {
            return prefs.getString(context.getString(R.string.pref_notify_key_tx_gridsquare), "");
        } else {
            return prefs.getString(context.getString(R.string.pref_notify_key_rx_gridsquare), "");
        }
    }

    /**
     * Returns the min TX-to-RX distance for wspr notifications.
     */
    public static double getNotifyTxRxKm(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sValue = prefs.getString(context.getString(R.string.pref_notify_key_min_tx_rx_distance),
                                        "-1");
        try {
            return (Double.parseDouble(sValue));
        } catch (Exception e) {
            return (double) (-1);
        }
    }

    /**
     * Returns number of seconds set to be the cutoff for "recent" wspr reports.
     */
    public static int cutoffSeconds(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sSeconds = prefs.getString(context.getString(R.string.pref_recent_key),
                                        context.getString(R.string.pref_recent_1hour));
        return (int)(Integer.parseInt(sSeconds));
    }

    /**
     * Returns wspr update interval.
     */
    public static int updateIntervalSeconds(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sSeconds = prefs.getString(context.getString(R.string.pref_update_interval_key),
                context.getString(R.string.pref_recent_1hour));
        return (int)(Integer.parseInt(sSeconds));
    }

    public static String formatDistance(Context context, double km, boolean isMetric) {
        if (isMetric) {
            return context.getString(R.string.format_distance_km, km);
        } else {
            double mi = km * 0.621371;
            return context.getString(R.string.format_distance_miles, mi);
        }
    }

    // TODO: add option to display in frequency or wavelength
    public static String formatFrequency(Context context, double mhz, boolean isMHz) {
        double freq;
        if ( !isMHz ) {
            freq = mhz;
        } else {
            freq = mhz;
        }
        return context.getString(R.string.format_frequency, freq);
    }

    public static String formatAzimuth(Context context, double degrees) {
        return context.getString(R.string.format_azimuth, degrees);
    }

    public static String formatPower(Context context, double power) {
        return context.getString(R.string.format_power, power);
    }
    public static String formatRxDrift(Context context, double drift) {
        return context.getString(R.string.format_drift, drift);
    }

    public static String formatSnr(Context context, double snr) {
        return context.getString(R.string.format_snr, snr);
    }

    public static String formatCallsign(Context context, String callsign, boolean isTx) {
        String padded = callsign;
        int max = isTx ? 8 : 10;  // TODO: http://wsprnet.org/drupal/downloads says Rx call is 10, Tx call is 6.
        if (padded.length() < max)
            padded += SPACES.substring(0, max-padded.length());
        if (isTx) {
            return context.getString(R.string.format_tx_callsign, padded);
        } else {
            return context.getString(R.string.format_rx_callsign, padded);
        }
    }

    public static String formatGridsquare(Context context, String gridsquare, boolean isTx) {
        String padded = gridsquare;
        int max = 6;  // http://wsprnet.org/drupal/downloads says gridsquare is either 4 or 6 characters
        if (padded.length() < max)
            padded += SPACES.substring(0, max-padded.length());
        if (isTx) {
            return context.getString(R.string.format_tx_gridsquare, padded);
        } else {
            return context.getString(R.string.format_rx_gridsquare, padded);
        }
    }

    static String formatTimestamp(String timestampString) {
        Date timestamp = WsprNetContract.getTimestampFromDb(timestampString);
        return DateFormat.getDateInstance().format(timestamp);
    }


    /**
     * Convert the database representation of the timestamp into something to display.
     *
     * @param timestampStr The db formatted timestamp string, expected to be of the form specified
     *                in Utility.TIMESTAMP_FORMAT_WSPR
     * @param format a SimpleDateFormat format string
     * @return a user-friendly representation of the timestamp.
     */
    public static String getFormattedTimestamp(String timestampStr, String format) {
        SimpleDateFormat fmtIn = new SimpleDateFormat(WsprNetContract.TIMESTAMP_FORMAT_DB); // '2014-08-25 21:02.0000'
        try {
            Date date = fmtIn.parse(timestampStr);
            SimpleDateFormat fmtOut = new SimpleDateFormat(format); //
            return fmtOut.format(date);
        }
        catch(ParseException e) {
            return timestampStr;
        }
    }

    /**
     * Given a timestamp, returns how long ago the event occurred in hours:minutes.
     * If more than 1 day, return the date string.
     *
     * @param context Context to use for resource localization
     * @param timestampStr The database-format timestamp string, specified by WsprNetContract.TIMESTAMP_FORMAT_DB
     * @return
     */
    public static String getTimeAgo(Context context, String timestampStr) {
        SimpleDateFormat dbTimestampFormat = new SimpleDateFormat(WsprNetContract.TIMESTAMP_FORMAT_DB);
        try {
            Date utcInputTimestamp = dbTimestampFormat.parse(timestampStr);
            Date localNowTimestamp = dbTimestampFormat.parse(WsprNetContract.getDbTimestampString(new Date()));
            Date localInputTimestamp = new Date(utcInputTimestamp.getTime() + TimeZone.getDefault().getOffset(localNowTimestamp.getTime()));
            return DateUtils.getRelativeDateTimeString(context,
                    localInputTimestamp.getTime(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.DAY_IN_MILLIS,
                    0).toString();  // TODO: any flags to shorten the string length?
        } catch (ParseException e) {
            e.printStackTrace();
            // It couldn't process the timestamp correctly.
            return timestampStr;
        }
    }

    /**
     * Given a timestamp, returns true, if it is "recent".
     *
     * @param context Context to use for resource localization
     * @param timestampStr The db formatted timestamp string, expected to be of the form specified
     *                in WsprNetContract.TIMESTAMP_FORMAT_DB
     * @return
     */
    public static boolean isRecent(Context context, String timestampStr, double dCutoffHours) {
        SimpleDateFormat dbTimestampFormat = new SimpleDateFormat(WsprNetContract.TIMESTAMP_FORMAT_DB);
        int cutoffHours = (int)dCutoffHours;
        int cutoffMinutes = (int)(dCutoffHours % 60.);
        try {
            Date inputTimestamp = dbTimestampFormat.parse(timestampStr);
            Date nowTimestamp = new Date();
            // If the timestamp is recent, return true.
            if (WsprNetContract.getDbTimestampString(nowTimestamp).equals(timestampStr)) {
                return true;
            } else {
                // Incoming time is not the current time.  Is it within the recent past (cutoff time)?
                Calendar calCutoff = Calendar.getInstance();
                calCutoff.setTime(nowTimestamp);
                calCutoff.add(Calendar.HOUR, -cutoffHours);
                calCutoff.add(Calendar.MINUTE, -cutoffMinutes);
                Calendar calInput = Calendar.getInstance();
                calInput.setTime(inputTimestamp);
                return calInput.after(calCutoff);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            // It couldn't process the timestamp correctly.
            return false;
        }
    }


    /**
     * Helper method to provide the icon resource id according to the wspr condition id returned
     * by the WSPR call.  Per wsprnet.org website, "typical values are -30 to +20dB."
     * @param rxSnr from WSPR API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWsprCondition(double rxSnr, boolean p) {
        p = !p;
        if      (( 15. <= rxSnr)                  ) {  return p? R.drawable.ic_signal_pos_50 : R.drawable.ic_signal_neg_50; }
        else if (( 10. <= rxSnr) && (rxSnr <  15.)) {  return p? R.drawable.ic_signal_pos_45 : R.drawable.ic_signal_neg_45; }
        else if ((  5. <= rxSnr) && (rxSnr <  10.)) {  return p? R.drawable.ic_signal_pos_40 : R.drawable.ic_signal_neg_40; }
        else if ((  0. <= rxSnr) && (rxSnr <   5.)) {  return p? R.drawable.ic_signal_pos_35 : R.drawable.ic_signal_neg_35; }
        else if (( -5. <= rxSnr) && (rxSnr <   0.)) {  return p? R.drawable.ic_signal_pos_30 : R.drawable.ic_signal_neg_30; }
        else if ((-10. <= rxSnr) && (rxSnr <  -5.)) {  return p? R.drawable.ic_signal_pos_25 : R.drawable.ic_signal_neg_25; }
        else if ((-15. <= rxSnr) && (rxSnr < -10.)) {  return p? R.drawable.ic_signal_pos_20 : R.drawable.ic_signal_neg_20; }
        else if ((-20. <= rxSnr) && (rxSnr < -15.)) {  return p? R.drawable.ic_signal_pos_15 : R.drawable.ic_signal_neg_15; }
        else if ((-25. <= rxSnr) && (rxSnr < -20.)) {  return p? R.drawable.ic_signal_pos_10 : R.drawable.ic_signal_neg_10; }
        else if ((-30. <= rxSnr) && (rxSnr < -25.)) {  return p? R.drawable.ic_signal_pos_05 : R.drawable.ic_signal_neg_05; }
        else {                                         return p? R.drawable.ic_signal_pos_00 : R.drawable.ic_signal_neg_00; }
    }

}  // class Utility
