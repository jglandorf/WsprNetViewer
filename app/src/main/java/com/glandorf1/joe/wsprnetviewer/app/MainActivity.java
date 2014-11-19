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

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.glandorf1.joe.wsprnetviewer.app.sync.WsprNetViewerSyncAdapter;

public class MainActivity extends ActionBarActivity
        implements WsprFragment.Callback,
        View.OnClickListener,
        PropagationMapsFiltersDialog.OnPropagationMapFiltersListenerView,
        PropagationMapsFiltersDialog.OnPropagationMapFiltersListenerTextView,
        PropagationMapsFiltersDialog.OnPropagationMapFiltersListenerDismiss,
        PropagationMapsSettingsDialog.OnPropagationMapSettingsListenerView,
        PropagationMapsSettingsDialog.OnPropagationMapSettingsListenerTextView,
        PropagationMapsSettingsDialog.OnPropagationMapSettingsListenerDismiss,
        PropagationMapsFiltersWavelengthDialog.OnPropagationMapFiltersWavelengthListenerView,
        PropagationMapsFiltersWavelengthDialog.OnPropagationMapFiltersWavelengthListenerDismiss,
        PropagationMapsFiltersWavelengthDialog.OnPropagationMapFiltersWavelengthListenerTextView
    {
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private boolean mDualPane;  // provision for putting the details fragment next to this fragment for wider screens
    private FragmentStatePagerAdapter adapterViewPager;
    private ViewPager mPager;
    protected Fragment mCurrentFragment = null;
//    protected Fragment[] fragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Display EULA
        new SimpleEula(this).show();
        // WsprFragment is now declared directly in layout xml file.
        if (findViewById(R.id.wspr_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw800dp). If this view is present, then the activity should be
            // in two-pane mode.
            mDualPane = true;

            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()  // getSupportFragmentManager() is this from the support lib?
                        .replace(R.id.wspr_detail_container, new DetailFragment())
                        .commit();
            }
        } else {
            mDualPane = false;
        }
//        WsprFragment wsprFragment = ((WsprFragment) getSupportFragmentManager()
//            .findFragmentById(R.id.fragment_wspr));
//        wsprFragment.setDualPane(mDualPane);

        mPager = (ViewPager) findViewById(R.id.pager);
        adapterViewPager = new MyStatePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(adapterViewPager);
        // The WSPR update rate is set from Settings...Update interval.
        WsprNetViewerSyncAdapter.initializeSyncAdapter(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_about) {
            showAbout();
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_settings_wspr_filters) {
            int menu = mPager.getCurrentItem() != 0 ? R.xml.pref_map_filters : R.xml.pref_wspr_filters;
            startActivity(new Intent(this, SettingsActivity.class)
                    .putExtra(SettingsActivity.SUB_MENU_WSPR_FILTER_KEY, menu));
            return true;
        }
        if (id == R.id.action_settings_wspr_wavelength) {
            int menu = mPager.getCurrentItem() != 0 ? R.xml.pref_map_filters : R.xml.pref_wspr_filters;
            startActivity(new Intent(this, SettingsActivity.class)
                    .putExtra(SettingsActivity.SUB_MENU_WSPR_FILTER_KEY, menu));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(String detailsKey) {
        if (mDualPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a fragment transaction.
            Bundle args = new Bundle();
            args.putString(DetailActivity.DETAILS_KEY, detailsKey);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.wspr_detail_container, fragment)
                    .commit();
        } else {
            // Single-pane mode.
            Intent intent = new Intent(this, DetailActivity.class)
                    .putExtra(DetailActivity.DETAILS_KEY, detailsKey);
            startActivity(intent);
        }
    }

    protected void showAbout() {
        // Inflate the About message contents
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);
        CharSequence version = getString(R.string.app_name);
        try {
            PackageInfo pkgInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            version = version + " " + getString(R.string.version) + pkgInfo.versionName;
        } catch (PackageManager.NameNotFoundException e1) {
            Log.e(this.getClass().getSimpleName(), "Name not found", e1);
        }

        // When linking text, force to always use default color. This works around a pressed color state bug.
         TextView textViewCredits = (TextView) messageView.findViewById(R.id.about_credits);
        int defaultColor = textViewCredits.getTextColors().getDefaultColor();
        textViewCredits.setTextColor(defaultColor);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(version);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

        @Override
        public void onClick(View view) {

        }

        protected final int WSPR_TAB = 0;
        protected final int MAPS_TAB = 1;
        protected final int NUM_TABS = 2;

        public class MyStatePagerAdapter extends FragmentStatePagerAdapter {
        public MyStatePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
//            fragments = new Fragment[]{
//                    new WsprFragment(),
//                    new PropagationMapsFragment()
//            };
        }

        @Override
        public CharSequence getPageTitle(int position) {
//            String s = fragments[position].getClass().getSimpleName(); // this works
//            return (CharSequence)s;
            switch(position) {
                case WSPR_TAB: return "WSPR";
                case MAPS_TAB: return "Map ";
                default: return "tab Huh?!?";
            }
        }

        @Override
        public int getCount() {
//            return fragments.length;
            return NUM_TABS;
        }

        @Override
        public Fragment getItem(int position) {
//            Fragment f = fragments[position];
//            return f;
            switch (position) {
                case WSPR_TAB:
                    return new WsprFragment();
                case MAPS_TAB:
                    return new PropagationMapsFragment();
                default:
                    return null;
            }
        } // getItem()

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (mCurrentFragment != object) {
                mCurrentFragment = (Fragment) object;
            }            super.setPrimaryItem(container, position, object);
        }
        } // MyStatePagerAdapter


    ///////////////////////////////////////////////////////////////////////////
    //////////// maps dialog interface callbacks
    @Override
    public void onPropagationMapFiltersView(View view) {
//        PropagationMapsFragment mapsFragment = (PropagationMapsFragment)fragments[MAPS_TAB];
        PropagationMapsFragment mapsFragment = (PropagationMapsFragment)mCurrentFragment;
        if (mapsFragment != null) {
            mapsFragment.onPropagationMapFiltersView(view);
        }
    }

        @Override
        public boolean onPropagationMapFiltersTextView(TextView textview, int i, KeyEvent keyEvent) {
//            PropagationMapsFragment mapsFragment = (PropagationMapsFragment)fragments[MAPS_TAB];
            PropagationMapsFragment mapsFragment = (PropagationMapsFragment)mCurrentFragment;
            if (mapsFragment != null) {
                return mapsFragment.onPropagationMapFiltersTextView(textview, i, keyEvent);
            }
            return false;
        }

        @Override
        public void onPropagationMapFiltersListenerDismiss() {
            PropagationMapsFragment mapsFragment = (PropagationMapsFragment)mCurrentFragment;
            if (mapsFragment != null) {
                mapsFragment.onPropagationMapFiltersListenerDismiss();
            }
        }

        @Override
        public void onPropagationMapSettingsView(View view) {
//        PropagationMapsFragment mapsFragment = (PropagationMapsFragment)fragments[MAPS_TAB];
            PropagationMapsFragment mapsFragment = (PropagationMapsFragment)mCurrentFragment;
            if (mapsFragment != null) {
                mapsFragment.onPropagationMapSettingsView(view);
            }
        }

        @Override
        public boolean onPropagationMapSettingsTextView(TextView textview, int i, KeyEvent keyEvent) {
//            PropagationMapsFragment mapsFragment = (PropagationMapsFragment)fragments[MAPS_TAB];
            PropagationMapsFragment mapsFragment = (PropagationMapsFragment)mCurrentFragment;
            if (mapsFragment != null) {
                return mapsFragment.onPropagationMapSettingsTextView(textview, i, keyEvent);
            }
            return false;
        }

        @Override
        public void onPropagationMapSettingsListenerDismiss() {
            PropagationMapsFragment mapsFragment = (PropagationMapsFragment)mCurrentFragment;
            if (mapsFragment != null) {
                mapsFragment.onPropagationMapSettingsListenerDismiss();
            }
        }

        @Override
        public void onPropagationMapFiltersWavelengthView(View view) {
            PropagationMapsFragment mapsFragment = (PropagationMapsFragment)mCurrentFragment;
            if (mapsFragment != null) {
                mapsFragment.onPropagationMapFiltersView(view);
            }
        }
        @Override
        public boolean onPropagationMapFiltersWavelengthTextView(TextView textview, int i, KeyEvent keyEvent) {
            PropagationMapsFragment mapsFragment = (PropagationMapsFragment)mCurrentFragment;
            if (mapsFragment != null) {
                return mapsFragment.onPropagationMapFiltersTextView(textview, i, keyEvent);
            }
            return false;
        }
        @Override
        public void onPropagationMapFiltersWavelengthListenerDismiss() {
            PropagationMapsFragment mapsFragment = (PropagationMapsFragment)mCurrentFragment;
            if (mapsFragment != null) {
                mapsFragment.onPropagationMapFiltersListenerDismiss();
            }
        }


    } // MainActivity

