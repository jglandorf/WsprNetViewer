package com.glandorf1.joe.wsprnetviewer.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.GridLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.glandorf1.joe.wsprnetviewer.app.PropagationMapsFiltersWavelengthDialog.OnPropagationMapFiltersWavelengthListenerView} interface
 * to handle interaction events.
 * Use the {@link com.glandorf1.joe.wsprnetviewer.app.PropagationMapsFiltersWavelengthDialog#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PropagationMapsFiltersWavelengthDialog extends DialogFragment {
    private final String LOG_TAG = PropagationMapsFragment.class.getSimpleName();
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnPropagationMapFiltersWavelengthListenerView mListenerView;
    private OnPropagationMapFiltersWavelengthListenerTextView mListenerTextView;
    private OnPropagationMapFiltersWavelengthListenerDismiss mListenerDismiss;
    private CheckBox cbEnableFilter, cbFilterWavelengthAll, cbFilterWavelengthNone;
    private GridLayout checkboxLayout;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PropagationMapsFiltersDialog.
     */
    // TODO: Rename and change types and number of parameters
    public static PropagationMapsFiltersWavelengthDialog newInstance(String param1, String param2) {
        PropagationMapsFiltersWavelengthDialog fragment = new PropagationMapsFiltersWavelengthDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public PropagationMapsFiltersWavelengthDialog() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(R.string.pref_filters_map_label);
        View rootView = inflater.inflate(R.layout.propagation_maps_filters_wavelength_dialog, container, false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // See http://tonyc9000.blogspot.com/2012/06/android-simple-checkbox-list.html
        checkboxLayout = (GridLayout) rootView.findViewById(R.id.propagation_maps_filter_wavelength_gridView);
        String[] cbWavelengthText, cbWavelengthTags;
        cbWavelengthText = getResources().getStringArray(R.array.pref_notify_band_options);
        cbWavelengthTags = getResources().getStringArray(R.array.pref_notify_band_values);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        String labelAny = getResources().getString(R.string.pref_band_label_any);

        for (int i = 0; i < cbWavelengthText.length; i++) {
            CheckBox cb = new CheckBox(getActivity());
            // Create a view ID to assist in possible use of RelativeLayout instead of LinearLayout.  See:
            //   http://stackoverflow.com/questions/1714297/android-view-setidint-id-programmatically-how-to-avoid-id-conflicts
            //   http://stackoverflow.com/questions/8460680/how-can-i-assign-an-id-to-a-view-programmatically/13241629#13241629
            // But that doesn't really help anyway, since addRule() is only in SDK 17+.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                cb.setId(i+1); // ID only has to be unique within this hierarchy.
            } else {
                cb.setId(View.generateViewId());
            }
            cb.setLayoutParams(layoutParams);
            cb.setFocusable(true);
            cb.setPadding(0, 0, 4, 0);
            cb.setTextAppearance(getActivity(), R.style.WsprTextAppearanceMedium);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                cb.setTextColor(getResources().getColor(R.color.white));
                cb.setText("         " + cbWavelengthText[i]); // hack: text begins at left edge of checkbox, so start over to the right a bit
            } else {
                cb.setText(cbWavelengthText[i]);
            }
            // Tag is the key prefix and option suffix:
            //   "pref_map_band_any"
            //   "pref_map_band_17m"
            String key = getResources().getString(R.string.pref_map_band_key_) + cbWavelengthText[i];
            cb.setTag(key);
            // skip the 'any' checkbox
            if (!cbWavelengthText[i].equals(labelAny)) {
                boolean defaultValue = true; // Boolean.parseBoolean(getActivity().getString(R.string.pref_filter_enable_default));
                boolean checked = prefs.getBoolean(cb.getTag().toString(), defaultValue);
                cb.setChecked(checked);
                cb.setOnClickListener(onClick);
            } else {
                cb.setVisibility(View.GONE);
            }
            checkboxLayout.addView(cb);
        }

        cbEnableFilter = (CheckBox) rootView.findViewById(R.id.propagation_maps_filter_wavelength_enable);
        cbEnableFilter.setOnClickListener(onClick);
        cbEnableFilter.setChecked(prefs.getBoolean(cbEnableFilter.getTag().toString(),
                                  Boolean.parseBoolean(getActivity().getString(R.string.pref_filter_enable_default))));
        cbFilterWavelengthAll = (CheckBox) rootView.findViewById(R.id.propagation_maps_filter_wavelength_all);
        cbFilterWavelengthAll.setOnClickListener(onClick);
        cbFilterWavelengthNone = (CheckBox) rootView.findViewById(R.id.propagation_maps_filter_wavelength_none);
        cbFilterWavelengthNone.setOnClickListener(onClick);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            cbFilterWavelengthAll.setText( "      " + cbFilterWavelengthAll.getText()); // hack: text begins at left edge of checkbox, so start over to the right a bit
            cbFilterWavelengthNone.setText("      " + cbFilterWavelengthNone.getText()); // hack: text begins at left edge of checkbox, so start over to the right a bit
        }
        return rootView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    View.OnClickListener onClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mListenerView != null) {
                mListenerView.onPropagationMapFiltersWavelengthView(view);
            }
        }
    };
    TextView.OnEditorActionListener onEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textview, int i, KeyEvent keyEvent) {
            if (mListenerTextView != null) {
                return mListenerTextView.onPropagationMapFiltersWavelengthTextView(textview, i, keyEvent);
            }
            return false;
        }
    };


    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListenerView = (OnPropagationMapFiltersWavelengthListenerView) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPropagationMapFiltersWavelengthListenerView");
        }
        try {
            mListenerTextView = (OnPropagationMapFiltersWavelengthListenerTextView) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPropagationMapFiltersWavelengthListenerTextView");
        }

        try {
            mListenerDismiss = (OnPropagationMapFiltersWavelengthListenerDismiss) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPropagationMapFiltersWavelengthListenerDismiss");
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mListenerDismiss != null) {
            mListenerDismiss.onPropagationMapFiltersWavelengthListenerDismiss();
        }
        super.onDismiss(dialog);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListenerView = null;
        mListenerTextView = null;
        mListenerDismiss = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnPropagationMapFiltersWavelengthListenerView {
        public void onPropagationMapFiltersWavelengthView(View view);
    }
    public interface OnPropagationMapFiltersWavelengthListenerTextView {
        public boolean onPropagationMapFiltersWavelengthTextView(TextView textview, int i, KeyEvent keyEvent);
    }
    public interface OnPropagationMapFiltersWavelengthListenerDismiss {
        public void onPropagationMapFiltersWavelengthListenerDismiss();
    }
}
