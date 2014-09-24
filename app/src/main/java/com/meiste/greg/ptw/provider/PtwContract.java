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

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Field and table name constants for {@link PtwProvider}.
 */
public class PtwContract {
    private PtwContract() {
    }

    public static final String CONTENT_AUTHORITY = "com.meiste.greg.ptw";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String QUERY_PARAMETER_LIMIT = "limit";

    public static class Race implements BaseColumns {

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.ptw.races";

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.ptw.race";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("races").build();

        public static final Uri CONTENT_SINGLE_URI =
                CONTENT_URI.buildUpon().appendQueryParameter(QUERY_PARAMETER_LIMIT, "1").build();

        public static final String TABLE_NAME = "schedule";

        public static final String COLUMN_NAME_RACE_ID = "race_id";
        public static final String COLUMN_NAME_RACE_NUM = "race_num";
        public static final String COLUMN_NAME_TRACK_LONG = "track_long";
        public static final String COLUMN_NAME_TRACK_SHORT = "track_short";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_TV = "tv";
        public static final String COLUMN_NAME_SIZE = "size";
        public static final String COLUMN_NAME_START = "start";
        public static final String COLUMN_NAME_QUESTION = "question";
        public static final String COLUMN_NAME_LAYOUT = "layout";
        public static final String COLUMN_NAME_CITY_STATE = "city_state";

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_RACE_ID = 1;
        public static final int COLUMN_RACE_NUM = 2;
        public static final int COLUMN_TRACK_LONG = 3;
        public static final int COLUMN_TRACK_SHORT = 4;
        public static final int COLUMN_NAME = 5;
        public static final int COLUMN_TV = 6;
        public static final int COLUMN_SIZE = 7;
        public static final int COLUMN_START = 8;
        public static final int COLUMN_QUESTION = 9;
        public static final int COLUMN_LAYOUT = 10;
        public static final int COLUMN_CITY_STATE = 11;

        public static final String[] PROJECTION_ALL = new String[] {
                _ID,
                COLUMN_NAME_RACE_ID,
                COLUMN_NAME_RACE_NUM,
                COLUMN_NAME_TRACK_LONG,
                COLUMN_NAME_TRACK_SHORT,
                COLUMN_NAME_NAME,
                COLUMN_NAME_TV,
                COLUMN_NAME_SIZE,
                COLUMN_NAME_START,
                COLUMN_NAME_QUESTION,
                COLUMN_NAME_LAYOUT,
                COLUMN_NAME_CITY_STATE};

        public static final String DEFAULT_SORT = COLUMN_NAME_RACE_ID + " ASC";
    }
}