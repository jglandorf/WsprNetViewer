package com.glandorf1.joe.wsprnetviewer.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.glandorf1.joe.wsprnetviewer.app.PropagationMapsSettingsDialog.OnPropagationMapSettingsListenerView} interface
 * to handle interaction events.
 * Use the {@link PropagationMapsSettingsDialog#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PropagationMapsSettingsDialog extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnPropagationMapSettingsListenerView mListenerView;
    private OnPropagationMapSettingsListenerTextView mListenerTextView;
    private OnPropagationMapSettingsListenerDismiss mListenerDismiss;
    private RadioButton rbMapHeatSetting, rbMapGreatCircle;
    private RadioButton rbMaxItemsAll, rbMaxItems50, rbMaxItems250;
    private RadioGroup rgMapTypes;
    CheckBox cbEnableMarkersTx, cbEnableMarkersRx;
    //private EditText mEditTextTxCallsign, mEditTextRxCallsign, mEditTextTxGridsquare, mEditTextRxGridsquare;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PropagationMapsSettingsDialog.
     */
    // TODO: Rename and change types and number of parameters
    public static PropagationMapsSettingsDialog newInstance(String param1, String param2) {
        PropagationMapsSettingsDialog fragment = new PropagationMapsSettingsDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public PropagationMapsSettingsDialog() {
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
        getDialog().setTitle(R.string.propagation_maps_settings_label);
        View rootView = inflater.inflate(R.layout.propagation_maps_settings_dialog, container, false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        rgMapTypes = (RadioGroup) rootView.findViewById(R.id.propagation_maps_settings_type);
        rbMapHeatSetting = (RadioButton) rootView.findViewById(R.id.propagation_maps_settings_type_heat);
        rbMapHeatSetting.setOnClickListener(onClick);
        rbMapHeatSetting.setChecked(prefs.getBoolean(rbMapHeatSetting.getTag().toString(),
                Boolean.parseBoolean(getActivity().getString(R.string.pref_maps_settings_default_map_type_heat))));
        rbMapGreatCircle = (RadioButton) rootView.findViewById(R.id.propagation_maps_settings_type_great_circle);
        rbMapGreatCircle.setOnClickListener(onClick);
        rbMapGreatCircle.setChecked(prefs.getBoolean(rbMapGreatCircle.getTag().toString(),
                Boolean.parseBoolean(getActivity().getString(R.string.pref_maps_settings_default_map_type_great_circle))));

        cbEnableMarkersTx = (CheckBox) rootView.findViewById(R.id.propagation_maps_settings_markers_tx);
        cbEnableMarkersTx.setOnClickListener(onClick);
        cbEnableMarkersTx.setChecked(prefs.getBoolean(cbEnableMarkersTx.getTag().toString(), false));
        cbEnableMarkersRx = (CheckBox) rootView.findViewById(R.id.propagation_maps_settings_markers_rx);
        cbEnableMarkersRx.setOnClickListener(onClick);
        cbEnableMarkersRx.setChecked(prefs.getBoolean(cbEnableMarkersRx.getTag().toString(), false));

        // TODO: clean up magic numbers with respect to max # of map items.
        rbMaxItemsAll = (RadioButton) rootView.findViewById(R.id.propagation_maps_settings_max_items_all);
        rbMaxItems50  = (RadioButton) rootView.findViewById(R.id.propagation_maps_settings_max_items_50);
        rbMaxItems250 = (RadioButton) rootView.findViewById(R.id.propagation_maps_settings_max_items_250);
        rbMaxItemsAll.setChecked(prefs.getBoolean(rbMaxItemsAll.getTag().toString(), false));
        rbMaxItems50.setChecked(prefs.getBoolean(rbMaxItems50.getTag().toString(), false));
        rbMaxItems250.setChecked(prefs.getBoolean(rbMaxItems250.getTag().toString(), true));
        rbMaxItemsAll.setOnClickListener(onClick);
        rbMaxItems50.setOnClickListener(onClick);
        rbMaxItems250.setOnClickListener(onClick);
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
                mListenerView.onPropagationMapSettingsView(view);
            }
        }
    };
    TextView.OnEditorActionListener onEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textview, int i, KeyEvent keyEvent) {
            if (mListenerTextView != null) {
                return mListenerTextView.onPropagationMapSettingsTextView(textview, i, keyEvent);
            }
            return false;
        }
    };


    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListenerView = (OnPropagationMapSettingsListenerView) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPropagationMapSettingsListenerView");
        }
        try {
            mListenerTextView = (OnPropagationMapSettingsListenerTextView) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPropagationMapListenerTextView");
        }

        try {
            mListenerDismiss = (OnPropagationMapSettingsListenerDismiss) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPropagationMapListenerDismiss");
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mListenerDismiss != null) {
            mListenerDismiss.onPropagationMapSettingsListenerDismiss();
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
    public interface OnPropagationMapSettingsListenerView {
        public void onPropagationMapSettingsView(View view);
    }
    public interface OnPropagationMapSettingsListenerTextView {
        public boolean onPropagationMapSettingsTextView(TextView textview, int i, KeyEvent keyEvent);
    }
    public interface OnPropagationMapSettingsListenerDismiss {
        public void onPropagationMapSettingsListenerDismiss();
    }
}
