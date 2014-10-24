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

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.glandorf1.joe.wsprnetviewer.app.data.WsprNetContract;
import com.glandorf1.joe.wsprnetviewer.app.sync.WsprNetViewerSyncAdapter;

/**
 * A {@link android.preference.PreferenceActivity} that presents a set of application settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static final String LOG_TAG = SettingsActivity.class.getSimpleName();

    // since we use the preference change initially to populate the summary
    // field, we'll ignore that change at start of the activity
    // ToDo: is that why 'setDefaultValues()' is never called in this app?
    //       See 'getDefaultSharedPreferences()', below.
    boolean mPreferenceBindingInProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);

        // Fix (hack) for gray text on black background on Android 2.3.4; see http://stackoverflow.com/questions/3164862/black-screen-in-inner-preferencescreen
        getWindow().setBackgroundDrawableResource(R.color.white); // ??
        PreferenceScreen b = (PreferenceScreen) findPreference(getString(R.string.pref_notifications_key));
        b.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                PreferenceScreen a = (PreferenceScreen) preference;
                a.getDialog().getWindow().setBackgroundDrawableResource(R.color.white);
                return false;
            }
        });

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_gridsquare_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_main_display_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_enable_notifications_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notify_min_snr_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notify_band_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_recent_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_update_interval_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_filter_enable_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notify_key_tx_callsign)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notify_key_rx_callsign)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notify_key_tx_gridsquare)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notify_key_rx_gridsquare)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notify_key_min_tx_rx_distance)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_filter_key_match_all)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_filter_key_tx_callsign)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_filter_key_rx_callsign)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_filter_key_tx_gridsquare)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_filter_key_rx_gridsquare)));
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        String key = preference.getKey();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
        if (!sp.contains(key))
        {
            return;
        }
        Object o = null;
        mPreferenceBindingInProgress = true;
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);
        try {
            o = sp.getString(key, "");
        } catch (ClassCastException e0) {
            try {
                o = sp.getBoolean(key, false);
            } catch (ClassCastException e1) {
                try {
                    o = sp.getInt(key, -1);
                } catch (ClassCastException e2) {
                    try {
                        o = sp.getFloat(key, -1);
                    } catch (ClassCastException e3) {
                        try {
                            o = sp.getLong(key, -1);
                        } catch (ClassCastException e4) {
                            Log.e(LOG_TAG, e4.getMessage(), e4);
                        }
                    }
                }
            }
        }

        // Trigger the listener immediately with the preference's current value.
        if (o != null)
            onPreferenceChange(preference, o);
        mPreferenceBindingInProgress = false;
    }

    // http://developer.android.com/reference/android/preference/Preference.OnPreferenceChangeListener.html#onPreferenceChange%28android.preference.Preference,%20java.lang.Object%29
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        // are we starting the preference activity?
        // ToDo: why does this get called at various stages of Settings menu 'inflation'?
        //       Because it gets called from onCreate()...bindPreferenceSummaryToValue().
        //       There is no callback for the checkbox ('wspr notifications')!
        //       "Called when a Preference has been changed by the user. This is called before the state of the Preference is about to be updated and before the state is persisted."
        if ( !mPreferenceBindingInProgress ) {
            // TODO: get city name, lat/long from gridsquare; determine how to look this up
            if (preference.getKey().equals(getString(R.string.pref_gridsquare_key))) {
                WsprNetViewerSyncAdapter.syncImmediately(this);
            } else {
                // notify code that wspr may be impacted
                getContentResolver().notifyChange(WsprNetContract.SignalReportEntry.CONTENT_URI, null);
            }
        }

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
                if (preference.getKey().equals(getString(R.string.pref_update_interval_key))) {
                    int interval = Integer.parseInt(stringValue);
                    WsprNetViewerSyncAdapter.configurePeriodicSync(getBaseContext(),
                            interval, (int)(interval/4)); // TODO: implement a setting for sync flextime
                }
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            if (preference.getKey().equals(getString(R.string.pref_notify_key_min_tx_rx_distance))) {
                // TODO: Display either km or miles-- this is not the correct function to achieve this.
                boolean metric = true; // Utility.isMetric(getBaseContext());
                double distance = -1;
                try {
                    distance = (Double.parseDouble(stringValue));
                } catch (Exception e) {
                    distance = -1;
                }
                if (distance >= 0.001) { // at least 1 meter or 0.001 mile
                    String sDist = Utility.formatDistance(getBaseContext(), distance, metric);
                    preference.setSummary(sDist);
                } else {
                    preference.setSummary("");
                }
            } else {
                preference.setSummary(stringValue);
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}