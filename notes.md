## Procedures
### Release Procedure
1.  Test the Debug version.
2.  Ensure "write to SD card" permission is disabled in app/src/main/AndroidManifest.xml:</br>
    ```xml
    <!-- uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" /-->
    ```
3.  Increment versionCode and versionName in both `app\build.gradle` and `app\src\main\AndroidManifest.xml`
4.  Build...Generate signed APK.
5.  Test the Release version.
6.  Add a sanitized version of the commit message to `versions.txt`:
>The main screen can now display grid square, callsign, or grid square+callsign. See the Settings menu.
7.  Tag and commit to local repo.  Tags should be in the format: `wspr-v6`
    where `6` is the value of `versionCode` in app/build.gradle (and in the manifest.)
    Also be sure to set the `Author` field (the Git GUI default is empty.)  Enter a commit message that
    indicates the version has been incremented:
>Added multiple main screen layouts.  The main screen can now display gridsquare, callsign, or gridsquare+callsign.  Made grid/call column slightly wider.</br>
>Avoid azimuth NaN calculation error.</br>
>Incremented version number`
8.  Upload to GitHub.
9.  Make an on-site backup copy of repo.
10. Upload to Play Store.  Include the *sanitized* version of the commit message from `versions.txt`.
>The main screen can now display grid square, callsign, or grid square+callsign. See the Settings menu.

</br>
</br>

## How To's
### Add a Preference
1. app/Utility.java</br>
    Add a 'getter' function for the preference, for example, see getMainDisplayPreference()

2. app/SettingsActivity.java</br>
    In onCreate(), add a call to bindPreferenceSummaryToValue():
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_main_display_key)));

3. res/xml/pref_general.xml</br>
    Add the preference menu:
    ```xml
    <ListPreference
        android:title="@string/pref_main_display_label"
        android:key="@string/pref_main_display_key"
        android:defaultValue="@string/pref_main_display_gridsquare"
        android:entryValues="@array/pref_main_display_values"
        android:entries="@array/pref_main_display_options" />
    ```

4. res/values/arrays.xml</br>
    Only if the menu item is a list array, add the options- and values-arrays:
    ```xml
    <string-array name="pref_main_display_options">
        <item>@string/pref_main_display_label_grid</item>
        <item>@string/pref_main_display_label_callsign</item>
        <item>@string/pref_main_display_label_grid_call</item>
    </string-array>
    <string-array name="pref_main_display_values">
        <item>@string/pref_main_display_gridsquare</item>
        <item>@string/pref_main_display_callsign</item>
        <item>@string/pref_main_display_grid_call</item>
    </string-array>
    ```

5. res/values/strings.xml</br>
    Add the menu key, label(s), and values(s):
    ```xml
    <!-- Key name for main display preference in SharedPreferences [CHAR LIMIT=NONE] -->
    <string name="pref_main_display_key" translatable="false">main_display</string>
    
    <!-- Label for the main display data preference [CHAR LIMIT=30] -->
    <string name="pref_main_display_label">Main display</string>
    <!-- Label for grid-only option in main display preference [CHAR LIMIT=25] -->
    <string name="pref_main_display_label_grid">Gridsquare</string>
    <!-- Label for callsign-only option in main display preference [CHAR LIMIT=25] -->
    <string name="pref_main_display_label_callsign">Callsign</string>
    <!-- Label for grid+callsign option in main display preference [CHAR LIMIT=25] -->
    <string name="pref_main_display_label_grid_call">Grid+callsign</string>    
    
    <!-- Value in SharedPreferences for gridsquare main display option [CHAR LIMIT=NONE] -->
    <string name="pref_main_display_gridsquare" translatable="false">gridsquare</string>
    <!-- Value in SharedPreferences for callsign main display option [CHAR LIMIT=NONE] -->
    <string name="pref_main_display_callsign" translatable="false">callsign</string>
    <!-- Value in SharedPreferences for grid+callsign main display option [CHAR LIMIT=NONE] -->
    <string name="pref_main_display_grid_call" translatable="false">grid_call</string>
    ```

