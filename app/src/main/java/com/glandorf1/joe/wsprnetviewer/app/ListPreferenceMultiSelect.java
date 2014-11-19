package com.glandorf1.joe.wsprnetviewer.app;

// Modified from http://blog.350nice.com/wp/wp-content/uploads/2009/07/listpreferencemultiselect.java
// See  http://www.droidweb.com/2009/08/developer-tip-9-enabling-multichoice-checkbox-menus-in-your-android-progra/
//      http://stackoverflow.com/questions/3446683/easy-way-to-make-the-listpreference-multiple-choice-in-android

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * A {@link Preference} that displays a list of entries as
 * a dialog and allows multiple selections
 * <p>
 * This preference will store a string into the SharedPreferences. This string will be the values selected
 * from the {@link #setEntryValues(CharSequence[])} array.
 * </p>
 */
public class ListPreferenceMultiSelect extends ListPreference implements OnClickListener {
    //Need to make sure the SEPARATOR is unique and weird enough that it doesn't match one of the entries.
    //Not using any fancy symbols because this is interpreted as a regex for splitting strings.
    private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";

    private boolean[] mClickedDialogEntryIndices;
    private boolean mClickedDialogEntryIndicesInitialied = false;
    private String mSummary;

    public ListPreferenceMultiSelect(Context context, AttributeSet attrs) {
        super(context, attrs);

        mClickedDialogEntryIndices = new boolean[getEntries().length];
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);
        mClickedDialogEntryIndices = new boolean[entries.length];
    }

    public ListPreferenceMultiSelect(Context context) {
        this(context, null);
    }

//    @Override
//    public void onClick(DialogInterface dialog, int which) {
//        super.onClick(dialog, which);
//    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();

        if (entries == null || entryValues == null || entries.length != entryValues.length ) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array which are both the same length");
        }

        restoreCheckedEntries();
        builder.setMultiChoiceItems(entries, mClickedDialogEntryIndices,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int which, boolean val) {
                        if (which == 0) {
                            ListView lv = ((AlertDialog) dialog).getListView();
                            int size = lv.getCount();
                            for(int i = 0; i < size; i++) {
                                lv.setItemChecked(i, val);
                                mClickedDialogEntryIndices[i] = val;
                            }
                        } else {
                            mClickedDialogEntryIndices[which] = val;
                        }
                    }
                });
    }

    public static String[] parseStoredValue(CharSequence val) {
        if ( "".equals(val) )
            return null;
        else
            return ((String)val).split(SEPARATOR);
    }

    private void restoreCheckedEntries() {
        CharSequence[] entryValues = getEntryValues();

        String[] vals = parseStoredValue(getValue());
        if ( vals != null ) {
            for ( int j=0; j<vals.length; j++ ) {
                String val = vals[j].trim();
                for ( int i=0; i<entryValues.length; i++ ) {
                    CharSequence entry = entryValues[i];
                    if ( entry.equals(val) ) {
                        mClickedDialogEntryIndices[i] = true;
                        break;
                    }
                }
            }
            mClickedDialogEntryIndicesInitialied = true;
        }
    }


    @Override
    public void setSummary(CharSequence summary) {
        String s = "";
        if (mClickedDialogEntryIndicesInitialied) {
            for (int x = 1; x < getEntryValues().length; x++) // ignore the first entry, 'any'
                if (mClickedDialogEntryIndices[x])
                    s += (s.equals("") ? "" : ", ") + getEntries()[x];
        } else {
            s = summary.toString().replace(SEPARATOR, ", ");
            CharSequence[] entries = getEntries();
            CharSequence[] entryValues = getEntryValues();
            for ( int i=1; i<entries.length; i++ ) { // ignore the first entry, 'any'
                s = s.replace(entryValues[i], entries[i]);
            }
        }
        super.setSummary(s);
    }

    @Override
    public void setEntryValues(CharSequence[] entryValues) {
        super.setEntryValues(entryValues);
        restoreCheckedEntries();
    }

    @Override
    public CharSequence getSummary() {
        final CharSequence entry = getEntry();
        if (mSummary == null) {
            return super.getSummary();
        } else {
            String s = mSummary;
            s.replace(SEPARATOR, ", ");
            CharSequence[] entries = getEntries();
            CharSequence[] entryValues = getEntryValues();
            for ( int i=1; i<entries.length; i++ ) { // ignore the first entry, 'any'
                s = s.replace(entryValues[i], entries[i]); // replace with display values
            }
            return s;
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
//        super.onDialogClosed(positiveResult);

        CharSequence[] entryValues = getEntryValues();
        if (positiveResult && entryValues != null) {
            StringBuffer value = new StringBuffer();
            for ( int i=1; i<entryValues.length; i++ ) { // ignore the first entry, 'any'
                if ( mClickedDialogEntryIndices[i] ) {
                    value.append(entryValues[i]).append(SEPARATOR);
                }
            }

            if (callChangeListener(value)) {
                mSummary = value.toString();
                if ( mSummary.length() > 0 )
                    mSummary = mSummary.substring(0, mSummary.length()-SEPARATOR.length());
                setValue(mSummary);
                mSummary = mSummary.replace(SEPARATOR, ", ");
            }
        }
    }
}
