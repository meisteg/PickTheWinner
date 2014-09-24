/*
 * Copyright (C) 2014 Gregory S. Meiste  <http://gregmeiste.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.meiste.greg.ptw.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class PtwProvider extends ContentProvider {

    private static final String AUTHORITY = PtwContract.CONTENT_AUTHORITY;

    /**
     * URI ID for route: /races
     */
    public static final int ROUTE_RACES = 1;

    /**
     * URI ID for route: /races/{ID}
     */
    public static final int ROUTE_RACES_ID = 2;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, "races", ROUTE_RACES);
        sUriMatcher.addURI(AUTHORITY, "races/*", ROUTE_RACES_ID);
    }

    private PtwDatabase mDatabaseHelper;

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new PtwDatabase(getContext());
        return true;
    }

    @Override
    public String getType(final Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ROUTE_RACES:
                return PtwContract.Race.CONTENT_TYPE;
            case ROUTE_RACES_ID:
                return PtwContract.Race.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection,
                        final String[] selectionArgs, final String sortOrder) {
        final String limit = uri.getQueryParameter(PtwContract.QUERY_PARAMETER_LIMIT);
        final SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        final SelectionBuilder builder = new SelectionBuilder();
        final int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case ROUTE_RACES_ID:
                final String id = uri.getLastPathSegment();
                builder.where(PtwContract.Race._ID + "=?", id);
            case ROUTE_RACES:
                builder.table(PtwContract.Race.TABLE_NAME)
                       .where(selection, selectionArgs);
                final Cursor c = builder.query(db, projection, sortOrder, limit);
                // Note: Notification URI must be manually set here for loaders to correctly
                // register ContentObservers.
                c.setNotificationUri(getContext().getContentResolver(), uri);
                return c;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri result;
        switch (match) {
            case ROUTE_RACES:
                final long id = db.insertOrThrow(PtwContract.Race.TABLE_NAME, null, values);
                result = Uri.parse(PtwContract.Race.CONTENT_URI + "/" + id);
                break;
            case ROUTE_RACES_ID:
                throw new UnsupportedOperationException("Insert not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        getContext().getContentResolver().notifyChange(uri, null, false);
        return result;
    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        final SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_RACES:
                count = builder.table(PtwContract.Race.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            case ROUTE_RACES_ID:
                final String id = uri.getLastPathSegment();
                count = builder.table(PtwContract.Race.TABLE_NAME)
                       .where(PtwContract.Race._ID + "=?", id)
                       .where(selection, selectionArgs)
                       .delete(db);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String selection,
                      final String[] selectionArgs) {
        final SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_RACES:
                count = builder.table(PtwContract.Race.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case ROUTE_RACES_ID:
                final String id = uri.getLastPathSegment();
                count = builder.table(PtwContract.Race.TABLE_NAME)
                        .where(PtwContract.Race._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }
}
