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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite backend for @{link PtwProvider}.
 *
 * Provides access to an disk-backed, SQLite datastore which is utilized by PtwProvider. This
 * database should never be accessed by other parts of the application directly.
 */
class PtwDatabase extends SQLiteOpenHelper {
    /** Schema version. */
    public static final int DATABASE_VERSION = 1;

    /** Filename for SQLite file. */
    public static final String DATABASE_NAME = "ptw.db";

    /** SQL statement to create "entry" table. */
    private static final String SQL_CREATE_SCHEDULE =
            "CREATE TABLE " + PtwContract.Race.TABLE_NAME + " (" +
                    PtwContract.Race._ID + " INTEGER PRIMARY KEY," +
                    PtwContract.Race.COLUMN_NAME_RACE_ID + " INTEGER," +
                    PtwContract.Race.COLUMN_NAME_RACE_NUM + " INTEGER," +
                    PtwContract.Race.COLUMN_NAME_TRACK_LONG + " TEXT," +
                    PtwContract.Race.COLUMN_NAME_TRACK_SHORT + " TEXT," +
                    PtwContract.Race.COLUMN_NAME_NAME + " TEXT," +
                    PtwContract.Race.COLUMN_NAME_TV + " TEXT," +
                    PtwContract.Race.COLUMN_NAME_SIZE + " TEXT," +
                    PtwContract.Race.COLUMN_NAME_START + " INTEGER," +
                    PtwContract.Race.COLUMN_NAME_QUESTION + " INTEGER," +
                    PtwContract.Race.COLUMN_NAME_LAYOUT + " TEXT," +
                    PtwContract.Race.COLUMN_NAME_CITY_STATE + " TEXT)";

    /** SQL statement to drop "entry" table. */
    private static final String SQL_DELETE_SCHEDULE =
            "DROP TABLE IF EXISTS " + PtwContract.Race.TABLE_NAME;

    public PtwDatabase(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SCHEDULE);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        // Upgrade policy is to simply to discard the data and start over
        db.execSQL(SQL_DELETE_SCHEDULE);
        onCreate(db);
    }
}