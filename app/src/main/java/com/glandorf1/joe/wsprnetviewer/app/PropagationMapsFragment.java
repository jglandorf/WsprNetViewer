package com.glandorf1.joe.wsprnetviewer.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.glandorf1.joe.wsprnetviewer.app.data.WsprNetContract;
import com.glandorf1.joe.wsprnetviewer.app.sync.WsprNetViewerSyncAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class PropagationMapsFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener,
        ClusterManager.OnClusterClickListener<WsprMapMarker>,
        ClusterManager.OnClusterInfoWindowClickListener<WsprMapMarker>,
        ClusterManager.OnClusterItemClickListener<WsprMapMarker>,
        ClusterManager.OnClusterItemInfoWindowClickListener<WsprMapMarker>,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMapLongClickListener
{
    private final String LOG_TAG = PropagationMapsFragment.class.getSimpleName();
    private static final int MAPS_LOADER = 1;
    private String mGridsquare, mFilterTxCallsign, mFilterRxCallsign, mFilterTxGridsquare, mFilterRxGridsquare;
    private boolean mFilterAnd, mFiltered;
    private int mMaxItems = -1, mMaxTimeAgoSeconds = -1;
    protected boolean needLoaderRestart = false, needMapRedraw = false;
    private static int mLastNumItems = -1;
    private boolean mIsVisible = false;
    Button buttonFilter, buttonWavelength, buttonSettings;
    ImageButton buttonLocate;
    private int mCountToastSettingsPersonalLocation = 1;
    private int mCountToastSettingsQTH = 4;

    SupportMapFragment mSupportMapFragment;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    float mMapZoomLevel, mMapZoomLevelMin, mMapZoomLevelMax;
    List<Polyline> polylineList = new ArrayList<Polyline>();
    private static final String SELECTED_MARKER = "selected_marker";
    private int mPosition = -1;  // selected item's position
    private HeatmapTileProvider mHeatMapProvider;
    private static final int ALT_HEATMAP_RADIUS = 50;
    private TileOverlay mHeatMapOverlay;
    private ClusterManager<WsprMapMarker> mClusterManager;

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
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != -1) {
            outState.putInt(SELECTED_MARKER, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.propagation_maps, container, false);
        buttonFilter = (Button) rootView.findViewById(R.id.propagation_map_button_filters);
        buttonWavelength = (Button) rootView.findViewById(R.id.propagation_map_button_filter_wavelength);
        buttonSettings = (Button) rootView.findViewById(R.id.propagation_map_button_settings);
        buttonLocate = (ImageButton) rootView.findViewById(R.id.propagation_map_button_locate);
        buttonFilter.setOnClickListener(this);
        buttonWavelength.setOnClickListener(this);
        buttonSettings.setOnClickListener(this);
        buttonLocate.setOnClickListener(this);

//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
//        prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChanged);
        return rootView;
    }

//    SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
//        @Override
//        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//            if(key.equals(getString(R.string.pref_filter_map_enable_key)))
//                getLoaderManager().restartLoader(PropagationMapsFragment.MAPS_LOADER, null, PropagationMapsFragment.this);
//        }
//    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpMapIfNeeded();
        getLoaderManager().initLoader(MAPS_LOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean filterOn = Utility.isFiltered(getActivity());
        // Restart the loader if some of the preferences have changed.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (filterOn // and filter settings have changed
                && (((mFilterTxCallsign != null) && !mFilterTxCallsign.equals((prefs.getString(getActivity().getString(R.string.pref_filter_map_key_tx_callsign), ""))))
                || ((mFilterRxCallsign != null) && !mFilterRxCallsign.equals((prefs.getString(getActivity().getString(R.string.pref_filter_map_key_rx_callsign), ""))))
                || ((mFilterTxGridsquare != null) && !mFilterTxGridsquare.equals((prefs.getString(getActivity().getString(R.string.pref_filter_map_key_tx_gridsquare), ""))))
                || ((mFilterRxGridsquare != null) && !mFilterRxGridsquare.equals((prefs.getString(getActivity().getString(R.string.pref_filter_map_key_rx_gridsquare), ""))))
                || (mFilterAnd != prefs.getBoolean(getActivity().getString(R.string.pref_filter_map_key_match_all), Boolean.parseBoolean(getActivity().getString(R.string.pref_filter_match_all_default))))
        )
                || (mFiltered != prefs.getBoolean(getActivity().getString(R.string.pref_filter_map_enable_key), Boolean.parseBoolean(getActivity().getString(R.string.pref_filter_enable_default)))) // or filter on/off has changed
                || (mMaxItems != Utility.getMaxMapItems(getActivity())) // or max items has changed
                || (mMaxTimeAgoSeconds != Utility.getMaxMapTimeAgoSeconds(getActivity())) // or cutoff time has changed
                ) {
            mLastNumItems = -1; // reset so that notification will appear
            getLoaderManager().restartLoader(MAPS_LOADER, null, this);
        }
        setUpMapIfNeeded();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(MAPS_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.propagation_map_button_filters) {
            buttonFilter.setEnabled(false);
            buttonWavelength.setEnabled(false);
            buttonSettings.setEnabled(false);
            DialogFragment newFragment = PropagationMapsFiltersDialog.newInstance("arg1", "arg2");
            newFragment.show(getFragmentManager(), "propagationMapsFiltersDialog");
        } else if (view.getId() == R.id.propagation_map_button_filter_wavelength) {
            buttonFilter.setEnabled(false);
            buttonWavelength.setEnabled(false);
            buttonSettings.setEnabled(false);
            DialogFragment newFragment = PropagationMapsFiltersWavelengthDialog.newInstance("arg1", "arg2");
            newFragment.show(getFragmentManager(), "propagationMapsFiltersWavelengthDialog");
        } else if (view.getId() == R.id.propagation_map_button_settings) {
            buttonFilter.setEnabled(false);
            buttonWavelength.setEnabled(false);
            buttonSettings.setEnabled(false);
            DialogFragment newFragment = PropagationMapsSettingsDialog.newInstance("arg1", "arg2");
            newFragment.show(getFragmentManager(), "propagationMapsSettingsDialog");
        } else if (view.getId() == R.id.propagation_map_button_locate) {
            showMyLocation(view);
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            mSupportMapFragment = SupportMapFragment.newInstance();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.propagation_map_fragment, mSupportMapFragment);
            fragmentTransaction.commit();
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setMyLocationEnabled(false);
//        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        mClusterManager = new ClusterManager<WsprMapMarker>(getActivity(), mMap);
        mClusterManager.setRenderer(new WsprMapMarkerRenderer());

        CompositeListener composite = new CompositeListener();
        composite.registerListener(mClusterManager);
        composite.registerListener(this);
        mMap.setOnCameraChangeListener(composite);
//        mMap.setOnCameraChangeListener(mClusterManager);
        mMapZoomLevelMin = mMap.getMinZoomLevel();
        mMapZoomLevelMax = mMap.getMaxZoomLevel();

        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);
        mMap.setOnMapLongClickListener(this);

    }

    @Override
    public void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        if (mSupportMapFragment != null) {
            mMap = mSupportMapFragment.getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        String sortOrder = WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT + " DESC";
        String selection = "", selectionBand = "";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mMaxItems = Utility.getMaxMapItems(getActivity());
        mMaxTimeAgoSeconds = Utility.getMaxMapTimeAgoSeconds(getActivity());
        mFiltered = prefs.getBoolean(getActivity().getString(R.string.pref_filter_map_enable_key),
                                     Boolean.parseBoolean(getActivity().getString(R.string.pref_filter_enable_default)));
        if (mFiltered) {
            selection = Utility.getMapsFilterSelectionStringForSql(getActivity());
            selectionBand = Utility.getFilterBandSelectionStringForSql(getActivity(), 5., false);
            if (selectionBand.length() > 0) {
                if (selection.length() > 0) {
                    selection += " and ";
                }
                selection += selectionBand;
            }
            if (mIsVisible && (selection.length() > 0)) {
                // Remind user that items are filtered, in case the result is not what they expect.
                Toast.makeText(getActivity(), getActivity().getString(R.string.toast_filter_items), Toast.LENGTH_SHORT).show();
            }
        }
        if (mMaxItems > 0) {
            Integer maxItems = mMaxItems;
            // TODO: some sources (http://stackoverflow.com/questions/12476302/limit-the-query-in-cursorloader)
            //       suggest using the ORDER clause is a hack, and that the SELECT clause is better.  Couldn't
            //       get it working with the SELECT clause, though.
            sortOrder += " LIMIT " + maxItems.toString();
        }
        if (mMaxTimeAgoSeconds > 0) {
            String selectionTimeAgo = Utility.getMaxMapTimeAgoSelectionStringForSql(getActivity(), mMaxTimeAgoSeconds);
            if (selection.length() > 0) {
                selection += " and ";
            }
            selection += selectionTimeAgo;
        }

        // Save some of the preferences to detect if they've changed in onResume().
        mGridsquare = Utility.getPreferredGridsquare(getActivity());
        mFilterTxCallsign = prefs.getString(getActivity().getString(R.string.pref_filter_map_key_tx_callsign), "");
        mFilterRxCallsign = prefs.getString(getActivity().getString(R.string.pref_filter_map_key_rx_callsign), "");
        mFilterTxGridsquare = prefs.getString(getActivity().getString(R.string.pref_filter_map_key_tx_gridsquare), "");
        mFilterRxGridsquare = prefs.getString(getActivity().getString(R.string.pref_filter_map_key_rx_gridsquare), "");
        mFilterAnd = prefs.getBoolean(getActivity().getString(R.string.pref_filter_map_key_match_all),
                Boolean.parseBoolean(getActivity().getString(R.string.pref_filter_match_all_default)));
//        mFiltered = prefs.getBoolean(getActivity().getString(R.string.pref_filter_map_enable_key),
//                Boolean.parseBoolean(getActivity().getString(R.string.pref_filter_enable_default)));
        Uri wsprUri = WsprNetContract.SignalReportEntry.buildWspr();

        // Create and return a CursorLoader that will take care of creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(), // context
                wsprUri,       // URI
                WsprFragment.WSPR_COLUMNS,  // String[] projection
                selection,    // String selection
                null,          // String[] selectionArgs
                sortOrder      // String sortOrder
        );
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        //Toast.makeText(getActivity(), LOG_TAG + " GC!!!!!!", Toast.LENGTH_LONG).show();
        Log.w(LOG_TAG, "onLowMemory: GC!!!!!!");
        System.gc();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Integer n = (cursor == null) ? 0 : cursor.getCount();
        Log.v(LOG_TAG, "onLoadFinished:  data.getCount()= " + n.toString());
        if ((n == 0) && (mLastNumItems != 0)) {
            // only do this once if there are no items
            if (mIsVisible) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.toast_no_items), Toast.LENGTH_LONG).show();
            }
            // It seems to be much faster to clear the map entirely than to remove the overlay or polylines.
            mMap.clear();
            mClusterManager.clearItems();
            if (!Utility.isFiltered(getActivity())) { // TODO: check total # records in database instead
                WsprNetViewerSyncAdapter.syncImmediately(getActivity());
            }
        } else if (n > 0) {
            if (mIsVisible) {
                String msg = getActivity().getString(R.string.toast_num_items, n);
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean mapTypeHeat = prefs.getBoolean(getActivity().getString(R.string.pref_maps_settings_key_map_type_heat),
                    Boolean.parseBoolean(getActivity().getString(R.string.pref_maps_settings_default_map_type_heat)));
            boolean mapTypeGreatCircle = prefs.getBoolean(getActivity().getString(R.string.pref_maps_settings_key_map_type_great_circle),
                    Boolean.parseBoolean(getActivity().getString(R.string.pref_maps_settings_default_map_type_great_circle)));
            boolean mapMarkersTxEnable = prefs.getBoolean(getActivity().getString(R.string.pref_maps_settings_markers_tx_key), false);
            boolean mapMarkersRxEnable = prefs.getBoolean(getActivity().getString(R.string.pref_maps_settings_markers_rx_key), false);
            boolean isMetric = Utility.isMetric(getActivity());
            boolean mapIntensityRxSnr = prefs.getBoolean(getActivity().getString(R.string.pref_maps_settings_key_map_intensity_snr),
                    true);
            boolean mapIntensitySnrMinusDbm = prefs.getBoolean(getActivity().getString(R.string.pref_maps_settings_key_map_intensity_snr_minus_tx_power),
                    false);

            // It seems to be much faster to clear the map entirely than to remove the overlay or polylines.
            mMap.clear();
            mClusterManager.clearItems();
            System.gc(); // also see onLowMemory(), above

            List<WeightedLatLng> latLngList = new ArrayList<WeightedLatLng>();
            List<WsprMapMarker> mapMarkerList = new ArrayList<WsprMapMarker>();
            if (mapTypeHeat || mapTypeGreatCircle || mapMarkersTxEnable || mapMarkersRxEnable) {
                float[] greatCircleHSV = new float[3];
                int greatCircleColor = Color.RED;
                Color.colorToHSV(greatCircleColor, greatCircleHSV);
                cursor.moveToPosition(-1);
                // Create a weighted latitude longitude list
                while (cursor.moveToNext()) {
                    double latRx = 0, lngRx = 0, rxsnr = 0., txdbm = 0., intensity = 0.;
                    String txgridsquare = cursor.getString(WsprFragment.COL_WSPR_TX_GRIDSQUARE);
                    double latTx = Utility.gridsquareToLatitude(txgridsquare);
                    double lngTx = Utility.gridsquareToLongitude(txgridsquare);
                    rxsnr = cursor.getDouble(WsprFragment.COL_WSPR_RX_SNR);
                    txdbm = cursor.getDouble(WsprFragment.COL_WSPR_TX_POWER);
                    // Summary of TX power for the first 1048575 spots in 2014-09
                    //           TX power        TX power
                    //            dBm             watts
                    // min        -33             5.01187E-07
                    // max         63          1995.262315
                    // avg         31.89159764    1.545822999
                    // std dev      6.96339440    7.682440496
                    // avg-std dev 24.92820324    0.311042922
                    // avg+std dev 38.85499205    7.682440496
                    intensity = rxsnr;
                    if (mapIntensitySnrMinusDbm) {
                        intensity = rxsnr - txdbm;
                    }
                    if (mapTypeHeat) {
                        // TODO: determine if we should display rxsnr or (rxsnr - txdbm).
                        latLngList.add(new WeightedLatLng(new LatLng(latTx, lngTx), intensity));
                    }
                    if (mapTypeGreatCircle || mapMarkersTxEnable || mapMarkersRxEnable) {
                        String rxgridsquare = cursor.getString(WsprFragment.COL_WSPR_RX_GRIDSQUARE);
                        latRx = Utility.gridsquareToLatitude(rxgridsquare);
                        lngRx = Utility.gridsquareToLongitude(rxgridsquare);
                    }
                    if (mapTypeGreatCircle) {
                        // convert snr range of -30 to +20 to saturation of 0.5 to +1.
                        intensity = intensity < -30. ? -30. : intensity;
                        intensity = intensity > +20. ? +20. : intensity;
                        greatCircleHSV[1] = (float) ((intensity + 80) / 100.);
                        // There may not be alpha support in early Android versions;
                        //   Build.VERSION.SDK_INT == 10 for GINGERBREAD_MR1
                        int alpha = (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1)?
                                      (int) (255. * (intensity + 40.) / 60.)
                                    : 255;
                        polylineList.add(mMap.addPolyline((new PolylineOptions())
                                .add(new LatLng(latTx, lngTx), new LatLng(latRx, lngRx))
                                .width(1)
                                .color(Color.HSVToColor(alpha, greatCircleHSV))
                                .geodesic(true)));
                    }
                    if (mapMarkersTxEnable || mapMarkersRxEnable) {
                        double frequency = cursor.getDouble(WsprFragment.COL_WSPR_TX_FREQ_MHZ);
                        String sFrequency = Utility.formatFrequency(getActivity(), frequency, true);
                        String sRxSnr = Utility.formatSnr(getActivity(), rxsnr) + "dB";
                        double km = cursor.getDouble(WsprFragment.COL_WSPR_DISTANCE);
                        String sDistance = Utility.formatDistance(getActivity(), km, isMetric) +
                                (isMetric ?
                                  getActivity().getString(R.string._units_metric_distance)
                                : getActivity().getString(R.string._units_english_distance) );
                        String txCallsign = cursor.getString(WsprFragment.COL_WSPR_TX_CALLSIGN);
                        String rxCallsign = cursor.getString(WsprFragment.COL_WSPR_RX_CALLSIGN);
                        String sTxdbm = Utility.formatSnr(getActivity(), txdbm) + "dBm";
                        String timestamp = cursor.getString(WsprFragment.COL_WSPR_TIMESTAMP);
                        String timestampText = Utility.getFormattedTimestamp(timestamp, Utility.TIMESTAMP_FORMAT_HOURS_MINUTES);
                        if (mapMarkersTxEnable) {
                            String info = getActivity().getString(R.string.string_tx) + ": " +
                                    timestampText + " UTC: " +
                                    sFrequency + "MHz @" + sTxdbm;
                            String description =
                                    txCallsign + "\u2192" + rxCallsign + ": " +
                                    sRxSnr + "/" + sDistance;
                            mapMarkerList.add(new WsprMapMarker(new LatLng(latTx, lngTx),
                                    info, description, timestamp,
                                    Utility.getIconResourceForWsprCondition(rxsnr, true)));
                        }
                        if (mapMarkersRxEnable) {
                            String info = getActivity().getString(R.string.string_rx) + ": " +
                                    timestampText + " UTC: " +
                                    sFrequency + "MHz @" + sRxSnr;
                            String description = rxCallsign + "\u2190" + txCallsign + ": " +
                                    sTxdbm + "/" + sDistance;
                            mapMarkerList.add(new WsprMapMarker(new LatLng(latRx, lngRx),
                                    info, description, timestamp,
                                    Utility.getIconResourceForWsprCondition(rxsnr, true)));
                        }
                    }
                }
                if (mapTypeHeat) {
                    // Draw the heat map
                    if (mHeatMapProvider == null) {
                        // Create a heat map tile provider, passing it the latlngs.
                        mHeatMapProvider = new HeatmapTileProvider.Builder()
                                .weightedData(latLngList)
                                .build();
                        mHeatMapProvider.setRadius(ALT_HEATMAP_RADIUS);
                    }
                    mHeatMapOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mHeatMapProvider));
                }
            }

            // Add markers
            if (mapMarkersTxEnable || mapMarkersRxEnable) {
                mClusterManager.addItems(mapMarkerList);
                mClusterManager.cluster();
            }


        } // have WSPR points
        mLastNumItems = n;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.v(LOG_TAG, "onPropagationMapTextView:  onLoaderReset");
    }


    //    @Override
    public void onPropagationMapFiltersView(View view) {
        boolean updated = false;
        // TODO: replace ...getSimpleName()... with ...instanceof...
        if (view.getClass().getSimpleName().equalsIgnoreCase("CheckBox")) {
            CheckBox cb = (CheckBox) view;
            Object oTag = cb.getTag();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = prefs.edit();
            if (oTag != null) {
                editor.putBoolean(view.getTag().toString(), cb.isChecked());
                updated = true;
            } else {
                // Set the preference for all items within the layout.
                String s = ((CheckBox) view).getText().toString();
                boolean all = s.equals(getResources().getString(R.string.string_select_all));
                GridLayout layout = (GridLayout) view.getParent();
                for (int i = 0; i < layout.getChildCount(); i++) {
                    View child = layout.getChildAt(i);
                    if (child instanceof CheckBox) {
                        if (child.getTag() != null) {
                            CheckBox checkBox = (CheckBox) child;
                            checkBox.setChecked(all);
                            editor.putBoolean(checkBox.getTag().toString(), checkBox.isChecked());
                            updated = true;
                        }
                    }
                }
                cb.setChecked(false);
            }
            if (updated) {
                editor.commit();
                needLoaderRestart = true;
            }
        }
    }

    //    @Override
    public boolean onPropagationMapFiltersTextView(TextView textview, int actionId, KeyEvent keyEvent) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_DONE) { // click= 'done', long-click= 'next'
            if (textview.getClass().getSimpleName().equalsIgnoreCase("EditText")) {
                EditText et = (EditText) textview;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(textview.getTag().toString(), et.getText().toString());
                editor.commit();
            }
            handled = true;
            needLoaderRestart = true;
        }
        return handled;
    }

    public void onPropagationMapFiltersListenerViewFocusChange(View view, boolean b) {
        // Use this routine for debugging.  Unfortunately, a focus change event doesn't occur when dialog is dismissed.
//        if (view instanceof EditText) {
//            if (b) {
//                TextView t = (TextView) view;
//                String text = t.getText().toString();
//                Log.v(LOG_TAG, "onPropagationMapFiltersListenerViewFocusChange:  '" + text + "'");
//                //onPropagationMapFiltersTextView(t, EditorInfo.IME_ACTION_DONE, null);
//                //needLoaderRestart = true; // temporary-- only way, for now, to redraw map is to restart loader
//                //needMapRedraw = true;
//            }
//        }
    }

    //    @Override
    public void onPropagationMapFiltersListenerDismiss() {
        if (needLoaderRestart) {
            Log.v(LOG_TAG, "onPropagationMapFiltersListenerDismiss:  calling restartLoader");
            getLoaderManager().restartLoader(MAPS_LOADER, null, this);
            Log.v(LOG_TAG, "onPropagationMapFiltersListenerDismiss:  called restartLoader");
            needLoaderRestart = false;
        }
        if (needMapRedraw) {
            // initiate map redraw
            Log.v(LOG_TAG, "onPropagationMapSettingsListenerDismiss:  initiate map redraw");
            needMapRedraw = false;
        }
        buttonFilter.setEnabled(true);
        buttonWavelength.setEnabled(true);
        buttonSettings.setEnabled(true);
    }

    //    @Override
    public void onPropagationMapSettingsView(View view) {
        if ((view instanceof CheckBox) || (view instanceof RadioButton)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = prefs.edit();
            if (view instanceof CheckBox) {
                CheckBox b = (CheckBox) view;
                editor.putBoolean(view.getTag().toString(), b.isChecked());
            } else if (view instanceof RadioButton) {
                // Set the preference for all items within the radio group.
                RadioGroup radioGroup = (RadioGroup) view.getParent();
                for (int i = 0; i < radioGroup.getChildCount(); i++) {
                    View child = radioGroup.getChildAt(i);
                    if (child instanceof RadioButton) {
                        RadioButton rb = (RadioButton) child;
                        editor.putBoolean(rb.getTag().toString(), rb.isChecked());
                    }
                }
            }
            editor.commit();
            needLoaderRestart = true; // temporary-- only way, for now, to redraw map is to restart loader
            needMapRedraw = true;
        }
    }

    //    @Override
    public boolean onPropagationMapSettingsTextView(TextView textview, int actionId, KeyEvent keyEvent) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (textview.getClass().getSimpleName().equalsIgnoreCase("EditText")) {
                EditText et = (EditText) textview;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(textview.getTag().toString(), et.getText().toString());
                editor.commit();
            }
            handled = true;
            needLoaderRestart = true;
        }
        return handled;
    }

    //    @Override
    public void onPropagationMapSettingsListenerDismiss() {
        if (needLoaderRestart) {
            Log.v(LOG_TAG, "onPropagationMapSettingsListenerDismiss:  calling restartLoader");
            getLoaderManager().restartLoader(MAPS_LOADER, null, this);
            needLoaderRestart = false;
            Log.v(LOG_TAG, "onPropagationMapSettingsListenerDismiss:  called restartLoader");
        }
        if (needMapRedraw) {
            // TODO: initiate map redraw
            Log.v(LOG_TAG, "onPropagationMapSettingsListenerDismiss:  initiate map redraw");
            needMapRedraw = false;
        }
        buttonFilter.setEnabled(true);
        buttonWavelength.setEnabled(true);
        buttonSettings.setEnabled(true);
    }

    @Override
    public boolean onClusterClick(Cluster<WsprMapMarker> cluster) {
        // Show a toast with some info when the cluster is clicked.
        String info = cluster.getItems().iterator().next().info;
        Toast.makeText(getActivity(), cluster.getSize() + " (including " + info + ")", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<WsprMapMarker> wsprMapMarkerCluster) {
        // Does nothing, but you could go to a list of the users.
    }

    @Override
    public boolean onClusterItemClick(WsprMapMarker wsprMapMarker) {
        // Does nothing, but you could go into the user's profile page, for example.
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(WsprMapMarker wsprMapMarker) {
        // Does nothing, but you could go into the user's profile page, for example.
        String tz = Calendar.getInstance().getTimeZone().getDisplayName(false, TimeZone.SHORT);
        String timeAgoText = Utility.getTimeAgo(getActivity(), wsprMapMarker.timestamp);
        String heading = "";
        String gridsquare = Utility.getPreferredGridsquare(getActivity());
        if (   (gridsquare.length() >= 2)
                && (!gridsquare.equalsIgnoreCase(getResources().getString(R.string.pref_gridsquare_default)))) {
            LatLng qth = new LatLng(Utility.gridsquareToLatitude(gridsquare), Utility.gridsquareToLongitude(gridsquare));
            // Compare to http://googlecompass.com/
            double dHeading = SphericalUtil.computeHeading(qth, wsprMapMarker.mPosition);
            if (dHeading < 0) {
                dHeading += 360.;
            }
            heading = "\n" + getResources().getString(R.string.heading_from_qth) + "= " +
                    Utility.formatAzimuth(getActivity(), dHeading);
        }
        Toast.makeText(getActivity(), timeAgoText + " @" + tz + heading, Toast.LENGTH_LONG).show();
    }


    // Multiple listeners:  http://stackoverflow.com/questions/5465204/how-can-i-set-up-multiple-listeners-for-one-event
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        mMapZoomLevel = cameraPosition.zoom;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        final String gridsquare = Utility.coordToGridsquare(latLng.latitude, latLng.longitude);
        if (gridsquare.length() >= 2) {
            AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
            adb.setTitle(getActivity().getResources().getString(R.string.gridsquare) + " " + gridsquare);
            adb.setMessage("Make this location your QTH?");
            adb.setIcon(android.R.drawable.ic_dialog_alert);
            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // which == -1
//                Integer i = which;
//                Toast.makeText(getActivity(), i.toString() + "POS click!!!!", Toast.LENGTH_LONG).show();
                    if (gridsquare.length() >= 2) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(getActivity().getResources().getString(R.string.pref_gridsquare_key), gridsquare);
                        editor.commit();
                    }
                }
            });

            adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // which == -2
//                Integer i = which;
//                Toast.makeText(getActivity(), i.toString() + "NEG click!!!!", Toast.LENGTH_LONG).show();
                }
            });
            adb.show();
        }
    }

    class CompositeListener implements GoogleMap.OnCameraChangeListener {
        private List<GoogleMap.OnCameraChangeListener> registeredListeners = new ArrayList<GoogleMap.OnCameraChangeListener>();
        public void registerListener (GoogleMap.OnCameraChangeListener listener) {
            registeredListeners.add(listener);
        }
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            for(GoogleMap.OnCameraChangeListener listener:registeredListeners) {
                listener.onCameraChange(cameraPosition);
            }
        }
    }

    /**
     * Draws profile photos inside markers (using IconGenerator).
     * When there are multiple people in the cluster, draw multiple photos (using MultiDrawable).
     */
    private class WsprMapMarkerRenderer extends DefaultClusterRenderer<WsprMapMarker> {
        private final IconGenerator mIconGenerator = new IconGenerator(getActivity());
        private final IconGenerator mClusterIconGenerator = new IconGenerator(getActivity());
        private final ImageView mImageView;
        private final ImageView mClusterImageView;
        private final int mDimension;

        public WsprMapMarkerRenderer() {
            super(getActivity(), mMap, mClusterManager);
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View multiProfile = inflater.inflate(R.layout.wspr_map_marker, null);

            mClusterIconGenerator.setContentView(multiProfile);
            mClusterImageView = (ImageView) multiProfile.findViewById(R.id.wspr_map_marker_image);

            mImageView = new ImageView(getActivity());
            mDimension = (int) getResources().getDimension(R.dimen.wspr_map_marker_image_dimen);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            int padding = (int) getResources().getDimension(R.dimen.wspr_map_marker_image_padding);
            mImageView.setPadding(padding, padding, padding, padding);
            mIconGenerator.setContentView(mImageView);
        }

        @Override
        protected void onBeforeClusterItemRendered(WsprMapMarker marker, MarkerOptions markerOptions) {
            // Draw an icon specific for this marker.
            // Set the info window to show their name.
            mImageView.setImageResource(marker.imageResource);
            Bitmap icon = mIconGenerator.makeIcon();
//            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(marker.info);
            markerOptions.title(marker.info).snippet(marker.description);
        }

//        @Override
//        protected void onBeforeClusterRendered(Cluster<WsprMapMarker> cluster, MarkerOptions markerOptions) {
//            // Draw multiple people.
//            // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
//            List<Drawable> profilePhotos = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
//            int width = mDimension;
//            int height = mDimension;
//
//            for (WsprMapMarker p : cluster.getItems()) {
//                // Draw 4 at most.
//                if (profilePhotos.size() == 4) break;
//                Drawable drawable = getResources().getDrawable(p.profilePhoto);
//                drawable.setBounds(0, 0, width, height);
//                profilePhotos.add(drawable);
//            }
//            MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
//            multiDrawable.setBounds(0, 0, width, height);
//
//            mClusterImageView.setImageDrawable(multiDrawable);
//            Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
//            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
//        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // mMapZoomLevelMin           mMapZoomLevelMax
            //    0 (zoom out) <= level <= 21 (zoom in)
            if (mMapZoomLevel < (mMapZoomLevelMax - 8)) {
                // Always render clusters.
                return cluster.getSize() >= 3;
            } else {
                return false;
            }
        }
    }


    /**
     * Button to get current Location. This demonstrates how to get the current Location as required
     * without needing to register a LocationListener.
     */
    public void showMyLocation(View view) {
        LatLng   latLng = null;
        if (latLng == null) {
            String gridsquare = Utility.getPreferredGridsquare(getActivity());
            if (   (gridsquare.length() >= 2)
                && (!gridsquare.equalsIgnoreCase(getResources().getString(R.string.pref_gridsquare_default)))) {
                latLng = new LatLng(Utility.gridsquareToLatitude(gridsquare), Utility.gridsquareToLongitude(gridsquare));
            } else {
                if (mCountToastSettingsQTH >= 0) {
                    String msg = getResources().getString(R.string.propagation_maps_long_click_to_set_qth);
                    Toast.makeText(getActivity(), msg,
                            (mCountToastSettingsQTH <= 2) ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG)
                            .show();
                    mCountToastSettingsQTH--;
                }
            }
        }
        if (latLng != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

}
