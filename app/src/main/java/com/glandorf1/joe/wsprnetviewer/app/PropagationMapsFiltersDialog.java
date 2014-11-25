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
import android.widget.EditText;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.glandorf1.joe.wsprnetviewer.app.PropagationMapsFiltersDialog.OnPropagationMapFiltersListenerView} interface
 * to handle interaction events.
 * Use the {@link PropagationMapsFiltersDialog#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PropagationMapsFiltersDialog extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnPropagationMapFiltersListenerView mListenerView;
    private OnPropagationMapFiltersListenerTextView mListenerTextView;
    private OnPropagationMapFiltersListenerDismiss mListenerDismiss;
    private OnPropagationMapFiltersListenerViewFocusChange mListenerFocusChange;
    private CheckBox cbEnableFilter, cbMatchAll;
    private EditText mEditTextTxCallsign, mEditTextRxCallsign, mEditTextTxGridsquare, mEditTextRxGridsquare;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PropagationMapsFiltersDialog.
     */
    // TODO: Rename and change types and number of parameters
    public static PropagationMapsFiltersDialog newInstance(String param1, String param2) {
        PropagationMapsFiltersDialog fragment = new PropagationMapsFiltersDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public PropagationMapsFiltersDialog() {
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
        View rootView = inflater.inflate(R.layout.propagation_maps_filters_dialog, container, false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        cbEnableFilter = (CheckBox) rootView.findViewById(R.id.propagation_maps_filter_enable);
        cbEnableFilter.setOnClickListener(onClick);
        cbEnableFilter.setChecked(prefs.getBoolean(cbEnableFilter.getTag().toString(),
                                  Boolean.parseBoolean(getActivity().getString(R.string.pref_filter_enable_default))));
        cbMatchAll = (CheckBox) rootView.findViewById(R.id.propagation_maps_filter_match_all);
        cbMatchAll.setOnClickListener(onClick);
        cbMatchAll.setChecked(prefs.getBoolean(cbMatchAll.getTag().toString(),
                              Boolean.parseBoolean(getActivity().getString(R.string.pref_filter_match_all_default))));

        mEditTextTxCallsign = (EditText) rootView.findViewById(R.id.propagation_maps_filter_tx_callsign);
        mEditTextTxCallsign.setOnEditorActionListener(onEditorAction);
        mEditTextTxCallsign.setOnFocusChangeListener(onFocusChange);
        mEditTextTxCallsign.setText(prefs.getString(mEditTextTxCallsign.getTag().toString(), ""));
        //mEditTextTxCallsign.setImeOptions(EditorInfo.IME_ACTION_DONE); // this is how to set it programmatically

        mEditTextRxCallsign = (EditText) rootView.findViewById(R.id.propagation_maps_filter_rx_callsign);
        mEditTextRxCallsign.setOnEditorActionListener(onEditorAction);
        mEditTextRxCallsign.setText(prefs.getString(mEditTextRxCallsign.getTag().toString(), ""));

        mEditTextTxGridsquare = (EditText) rootView.findViewById(R.id.propagation_maps_filter_tx_gridsquare);
        mEditTextTxGridsquare.setOnEditorActionListener(onEditorAction);
        mEditTextTxGridsquare.setText(prefs.getString(mEditTextTxGridsquare.getTag().toString(), ""));

        mEditTextRxGridsquare = (EditText) rootView.findViewById(R.id.propagation_maps_filter_rx_gridsquare);
        mEditTextRxGridsquare.setOnEditorActionListener(onEditorAction);
        mEditTextRxGridsquare.setText(prefs.getString(mEditTextRxGridsquare.getTag().toString(), ""));
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
                mListenerView.onPropagationMapFiltersView(view);
            }
        }
    };
    TextView.OnEditorActionListener onEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textview, int i, KeyEvent keyEvent) {
            if (mListenerView != null) {
                return mListenerTextView.onPropagationMapFiltersTextView(textview, i, keyEvent);
            }
            return false;
        }
    };
    View.OnFocusChangeListener onFocusChange = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean b) {
            if (mListenerFocusChange != null) {
                mListenerFocusChange.onPropagationMapFiltersListenerViewFocusChange(view, b);
            }
        }
    };


    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListenerView = (OnPropagationMapFiltersListenerView) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPropagationMapFiltersListenerView");
        }
        try {
            mListenerTextView = (OnPropagationMapFiltersListenerTextView) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPropagationMapFiltersListenerTextView");
        }

        try {
            mListenerDismiss = (OnPropagationMapFiltersListenerDismiss) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPropagationMapFiltersListenerDismiss");
        }
        try {
            mListenerFocusChange = (OnPropagationMapFiltersListenerViewFocusChange) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPropagationMapFiltersListenerViewFocusChange");
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mListenerDismiss != null) {
            mListenerDismiss.onPropagationMapFiltersListenerDismiss();
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
    public interface OnPropagationMapFiltersListenerView {
        // TODO: Update argument type and name
        public void onPropagationMapFiltersView(View view);
    }
    public interface OnPropagationMapFiltersListenerTextView {
        // TODO: Update argument type and name
        public boolean onPropagationMapFiltersTextView(TextView textview, int i, KeyEvent keyEvent);
    }
    public interface OnPropagationMapFiltersListenerDismiss {
        public void onPropagationMapFiltersListenerDismiss();
    }
    public interface OnPropagationMapFiltersListenerViewFocusChange {
        public void onPropagationMapFiltersListenerViewFocusChange(View view, boolean b);
    }
}
