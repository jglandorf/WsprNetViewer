package com.glandorf1.joe.wsprnetviewer.app;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import java.util.concurrent.TimeUnit;

// UI tests.
// At the moment, this is more of a proof-of-concept for Robotium.
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private Solo solo;
    public MainActivityTest() {
        super(MainActivity.class);
    }
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
    }

    public void testDisplayWhiteBox() {
        boolean result;
        // Open the 'Settings' menu activity, then wait for it.
        solo.clickOnActionBarItem(R.id.action_settings);
        result = solo.waitForActivity("SettingsActivity");
        assertTrue(result);
        if (result) {
            waitSeconds(4);
            // Now that the Settings menu is open, attempt to write a preference.
            // Need to find a mechanism to edit preferences...
            // There is a method described here
            //   http://stackoverflow.com/questions/11246968/testing-a-preferenceactivity-with-robotium
            // to alter a preference checkbox which might work.
//            //Access First value (edit-filed) and putting firstNumber value in it
//            EditTextPreference vEditText = (EditTextPreference) solo.getView(R.id.pref_gridsquare_id);
//            String textOld = vEditText.getText().toString();
//            String textNew = "AB12cd";
//            solo.clearEditText(vEditText);
//            waitSeconds(1);
//            solo.enterText(vEditText, textNew);
//            waitSeconds(1);
//            assertEquals(textNew, vEditText.getText().toString());
//            solo.clearEditText(vEditText);
//            waitSeconds(1);
//            solo.enterText(vEditText, textOld);
//            assertEquals(textOld, vEditText.getText().toString());
//            waitSeconds(1);
            solo.goBack();
        }
    }

    @Override
    protected void tearDown() throws Exception{
        solo.finishOpenedActivities();
    }

    private void waitSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            //Handle exception
        }
    }
}