/*
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nafundi.taskforce.collect.android.provider;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.nafundi.taskforce.collect.android.database.ODKSQLiteOpenHelper;
import com.nafundi.taskforce.collect.android.provider.LookupProviderAPI.LookupColumns;

/**
 * 
 */
public class LookupProvider extends ContentProvider {

    private static final String t = "LookupProvider";

    private static final String DATABASE_NAME = "lookup.db";
    private static final int DATABASE_VERSION = 4;
    private static final String LOOKUP_TABLE_NAME = "lookup";

    private static HashMap<String, String> sLookupProjectionMap;

    private static final int LOOKUP = 1;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends ODKSQLiteOpenHelper {

        DatabaseHelper(String databaseName) {
            super(Environment.getExternalStorageDirectory() + "/odk/metadata", databaseName, null, DATABASE_VERSION);
        }


        @Override
        public void onCreate(SQLiteDatabase db) {            
           
         db.execSQL("CREATE TABLE " + LOOKUP_TABLE_NAME + " (" 
            + LookupColumns._ID + " integer primary key, " 
            + LookupColumns.INSTANCE_PATH + " text not null, "
            + LookupColumns.COLUMN_1 + " text, "
            + LookupColumns.COLUMN_2 + " text, "
            + LookupColumns.COLUMN_3 + " text, "
            + LookupColumns.COLUMN_4 + " text, "
            + LookupColumns.COLUMN_5 + " text, "
            + LookupColumns.COLUMN_6 + " text, "
            + LookupColumns.COLUMN_7 + " text, "
            + LookupColumns.COLUMN_8 + " text, "
            + LookupColumns.COLUMN_9 + " text, "
            + LookupColumns.COLUMN_10 + " text);"); 
        }


        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(t, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS lookup");
            onCreate(db);
        }
    }

    private DatabaseHelper mDbHelper;


    @Override
    public boolean onCreate() {
        mDbHelper = new DatabaseHelper(DATABASE_NAME);
        return true;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(LOOKUP_TABLE_NAME);

        switch (sUriMatcher.match(uri)) {
            case LOOKUP:
                qb.setProjectionMap(sLookupProjectionMap);
                break;

//            case INSTANCE_ID:
//                qb.setProjectionMap(sInstancesProjectionMap);
//                qb.appendWhere(InstanceColumns._ID + "=" + uri.getPathSegments().get(1));
//                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Get the database and run the query
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String groupBy = null;
        if (projection != null && projection.length > 0 ) {
        	groupBy = projection[0];
        }
        //Log.e("Carl", "query string = " 
        //+ qb.buildQuery(projection, selection, selectionArgs, null, null, sortOrder, null));
//        Log.e("Carl", "query = " + qb.buildQuery(projection, selection, selectionArgs, groupBy, null, sortOrder, null));
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        //c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }


    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case LOOKUP:
                return LookupColumns.CONTENT_TYPE;

//            case INSTANCE_ID:
//                return InstanceColumns.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }


    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != LOOKUP) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long rowId = db.insert(LOOKUP_TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri lookupUri = ContentUris.withAppendedId(LookupColumns.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(lookupUri, null);
            return lookupUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }
    
    
    /**
     * This method removes the entry from the content provider, and also removes any associated files.
     * files:  form.xml, [formmd5].formdef, formname-media {directory}
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;
        
        switch (sUriMatcher.match(uri)) {
            case LOOKUP:                
                Cursor del = this.query(uri, null, where, whereArgs, null);
                del.moveToPosition(-1);
                while (del.moveToNext()) {
                    //String File = del.getString(del.getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH));
                    //String instanceDir = (new File(instanceFile)).getParent();
                    //deleteFileOrDir(instanceDir);
                }
                del.close();
                count = db.delete(LOOKUP_TABLE_NAME, where, whereArgs);
                break;

//            case INSTANCE_ID:
//                String instanceId = uri.getPathSegments().get(1);
//
//                Cursor c = this.query(uri, null, where, whereArgs, null);
//                // This should only ever return 1 record.  I hope.
//                c.moveToPosition(-1);
//                while (c.moveToNext()) {
//                    String instanceFile = c.getString(c.getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH));
//                    String instanceDir = (new File(instanceFile)).getParent();
//                    deleteFileOrDir(instanceDir);           
//                }
//                c.close();
//                
//                count =
//                    db.delete(INSTANCES_TABLE_NAME,
//                        InstanceColumns._ID + "=" + instanceId
//                                + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
//                        whereArgs);
//                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;

        switch (sUriMatcher.match(uri)) {
            case LOOKUP:
                
                count = db.update(LOOKUP_TABLE_NAME, values, where, whereArgs);
                break;

//            case INSTANCE_ID:
//                String instanceId = uri.getPathSegments().get(1);
//
//                if (values.containsKey(InstanceColumns.STATUS)) {
//                    status = values.getAsString(InstanceColumns.STATUS);
//                    
//                    if (values.containsKey(InstanceColumns.DISPLAY_SUBTEXT) == false) {
//                        Date today = new Date();
//                        String text = getDisplaySubtext(status, today);
//                        values.put(InstanceColumns.DISPLAY_SUBTEXT, text);
//                    }
//                }
//               
//                count =
//                    db.update(INSTANCES_TABLE_NAME, values, InstanceColumns._ID + "=" + instanceId
//                            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
//                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(LookupProviderAPI.AUTHORITY, "lookup", LOOKUP);
//        sUriMatcher.addURI(InstanceProviderAPI.AUTHORITY, "instances/#", INSTANCE_ID);
        
        //sUriMatcher.addURI(InstanceProviderAPI.AUTHORITY, "lookup", INSTANCES);

        sLookupProjectionMap = new HashMap<String, String>();
        sLookupProjectionMap.put(LookupColumns._ID, LookupColumns._ID);
        sLookupProjectionMap.put(LookupColumns.COLUMN_1, LookupColumns.COLUMN_1);
        sLookupProjectionMap.put(LookupColumns.COLUMN_2, LookupColumns.COLUMN_2);
        sLookupProjectionMap.put(LookupColumns.COLUMN_3, LookupColumns.COLUMN_3);
        sLookupProjectionMap.put(LookupColumns.COLUMN_4, LookupColumns.COLUMN_4);
        sLookupProjectionMap.put(LookupColumns.COLUMN_5, LookupColumns.COLUMN_5);
        sLookupProjectionMap.put(LookupColumns.COLUMN_6, LookupColumns.COLUMN_6);
        sLookupProjectionMap.put(LookupColumns.COLUMN_7, LookupColumns.COLUMN_7);
        sLookupProjectionMap.put(LookupColumns.COLUMN_8, LookupColumns.COLUMN_8);
        sLookupProjectionMap.put(LookupColumns.COLUMN_9, LookupColumns.COLUMN_9);
        sLookupProjectionMap.put(LookupColumns.COLUMN_10, LookupColumns.COLUMN_10);
        sLookupProjectionMap.put(LookupColumns.INSTANCE_PATH, LookupColumns.INSTANCE_PATH);
        
    }

}
