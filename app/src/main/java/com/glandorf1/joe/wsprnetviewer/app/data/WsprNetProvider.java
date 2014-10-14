/*
 * Copyright (C) 2014 The Android Open Source Project
 * Modifications Copyright (C) 2014 Joseph D. Glandorf
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
package com.glandorf1.joe.wsprnetviewer.app.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class WsprNetProvider extends ContentProvider {
    private static final String LOG_TAG = WsprNetProvider.class.getSimpleName();

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private WsprNetDbHelper mOpenHelper;
    private static final int WSPR = 100;                               // "content://com.glandorf1.joe.wsprnetviewer.app/wspr"
    private static final int WSPR_ID = 101;                            // "content://com.glandorf1.joe.wsprnetviewer.app/wspr/[RECORD_ID]"
    private static final int WSPR_WITH_GRIDSQUARE_AND_TIMESTAMP = 102; // "content://com.glandorf1.joe.wsprnetviewer.app/wspr/[GRIDSQUARE_ID]/timestamp"
    private static final int GRIDSQUARE = 300;                         // "content://com.glandorf1.joe.wsprnetviewer.app/gridsquare"
    private static final int GRIDSQUARE_ID = 301;                      // "content://com.glandorf1.joe.wsprnetviewer.app/gridsquare/[GRIDSQUARE_ID]"

    private static final SQLiteQueryBuilder sWsprByGridsquareSettingQueryBuilder;

    static{
        sWsprByGridsquareSettingQueryBuilder = new SQLiteQueryBuilder();
        sWsprByGridsquareSettingQueryBuilder.setTables(
                WsprNetContract.SignalReportEntry.TABLE_NAME + " INNER JOIN " +
                        WsprNetContract.GridSquareEntry.TABLE_NAME +
                        " ON " + WsprNetContract.SignalReportEntry.TABLE_NAME +
                        "." + WsprNetContract.SignalReportEntry.COLUMN_LOC_KEY +
                        " = " + WsprNetContract.GridSquareEntry.TABLE_NAME +
                        "." + WsprNetContract.GridSquareEntry._ID);
    }

    private static final String sGridsquareSettingSelection =
            WsprNetContract.GridSquareEntry.TABLE_NAME+
                    "." + WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING + " = ? ";

    private static final String sGridsquareSettingWithStartTimestampSelection =
            WsprNetContract.GridSquareEntry.TABLE_NAME+
                    "." + WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING + " = ? AND " +
                    WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT + " >= ? ";

    private static final String sGridsquareSettingAndTimestampSelection =
            WsprNetContract.GridSquareEntry.TABLE_NAME +
                    "." + WsprNetContract.GridSquareEntry.COLUMN_GRIDSQUARE_SETTING + " = ? AND " +
                    WsprNetContract.SignalReportEntry.COLUMN_TIMESTAMPTEXT + " = ? ";

    private Cursor getWsprByGridsquareSetting(Uri uri, String[] projection, String sortOrder) {
        String gridsquareSetting = WsprNetContract.SignalReportEntry.getGridsquareSettingFromUri(uri);
        String startTimestamp = WsprNetContract.SignalReportEntry.getStartTimestampFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startTimestamp == null) {
            selection = sGridsquareSettingSelection;
            selectionArgs = new String[]{gridsquareSetting};
        } else {
            selectionArgs = new String[]{gridsquareSetting, startTimestamp};
            selection = sGridsquareSettingWithStartTimestampSelection;
        }

        return sWsprByGridsquareSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getWsprByGridsquareSettingAndTimestamp(Uri uri, String[] projection, String sortOrder) {
        String gridsquareSetting = WsprNetContract.SignalReportEntry.getGridsquareSettingFromUri(uri);
        String timestamp = WsprNetContract.SignalReportEntry.getTimestampFromUri(uri);

        return sWsprByGridsquareSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sGridsquareSettingAndTimestampSelection,
                new String[]{gridsquareSetting, timestamp},
                null,
                null,
                sortOrder
        );
    }


    private static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WsprNetContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, WsprNetContract.PATH_WSPR, WSPR);
        matcher.addURI(authority, WsprNetContract.PATH_WSPR + "/*", WSPR_ID);
        matcher.addURI(authority, WsprNetContract.PATH_WSPR + "/*/*", WSPR_WITH_GRIDSQUARE_AND_TIMESTAMP);

        matcher.addURI(authority, WsprNetContract.PATH_GRIDSQUARE, GRIDSQUARE);
        matcher.addURI(authority, WsprNetContract.PATH_GRIDSQUARE + "/#", GRIDSQUARE_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WsprNetDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Given a URI,  determine what kind of request it is, and query the database accordingly.
        Cursor retCursor = null;
        try {
            switch (sUriMatcher.match(uri)) {
                // "wspr/*/*"
                case WSPR_WITH_GRIDSQUARE_AND_TIMESTAMP:
                {
                    retCursor = getWsprByGridsquareSettingAndTimestamp(uri, projection, sortOrder);
                    break;
                }
                // "wspr/*"
                case WSPR_ID: {
                        retCursor = mOpenHelper.getReadableDatabase().query(
                                WsprNetContract.SignalReportEntry.TABLE_NAME,
                                projection,
                                WsprNetContract.SignalReportEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                                null,
                                null,
                                null,
                                sortOrder
                        );
                    break;
                }
                // "wspr"
                case WSPR: {
                    retCursor = mOpenHelper.getReadableDatabase().query(
                            WsprNetContract.SignalReportEntry.TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );
                    break;
                }
                // "gridsquare/*"
                case GRIDSQUARE_ID: {
                    retCursor = mOpenHelper.getReadableDatabase().query(
                            WsprNetContract.GridSquareEntry.TABLE_NAME,
                            projection,
                            WsprNetContract.GridSquareEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                            null,
                            null,
                            null,
                            sortOrder
                    );
                    break;
                }
                // "gridsquare"
                case GRIDSQUARE: {
                    retCursor = mOpenHelper.getReadableDatabase().query(
                            WsprNetContract.GridSquareEntry.TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );
                    break;
                }

                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        } catch (android.database.sqlite.SQLiteException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }

        if (retCursor != null) {
            retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case WSPR_WITH_GRIDSQUARE_AND_TIMESTAMP:
                return WsprNetContract.SignalReportEntry.CONTENT_ITEM_TYPE;
            case WSPR_ID:
                return WsprNetContract.SignalReportEntry.CONTENT_ITEM_TYPE;
            case WSPR:
                return WsprNetContract.SignalReportEntry.CONTENT_TYPE;
            case GRIDSQUARE:
                return WsprNetContract.GridSquareEntry.CONTENT_TYPE;
            case GRIDSQUARE_ID:
                return WsprNetContract.GridSquareEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case WSPR: {
                long _id = db.insert(WsprNetContract.SignalReportEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = WsprNetContract.SignalReportEntry.buildWsprUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case GRIDSQUARE: {
                long _id = db.insert(WsprNetContract.GridSquareEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = WsprNetContract.GridSquareEntry.buildGridsquareUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case WSPR:
                rowsDeleted = db.delete(
                        WsprNetContract.SignalReportEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case GRIDSQUARE:
                rowsDeleted = db.delete(
                        WsprNetContract.GridSquareEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (selection == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case WSPR:
                rowsUpdated = db.update(WsprNetContract.SignalReportEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case GRIDSQUARE:
                rowsUpdated =  db.update(WsprNetContract.GridSquareEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WSPR:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(WsprNetContract.SignalReportEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                    // TODO:  no "catch()" clause?  Should the wspr table be cleared if insert fails when full?
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
