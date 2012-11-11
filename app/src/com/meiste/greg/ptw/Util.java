/*
 * Copyright (C) 2012 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.greg.ptw;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public final class Util {

    public final static String GCM_SENDER_ID = "540948604089";

    private final static String TAG = "PickTheWinner";
    private final static String PREFS_STATE = "state";
    private final static String PREFS_SETUP = "setup";

    public static void log(String msg) {
        if (BuildConfig.DEBUG) Log.d(TAG, msg);
    }

    public static SharedPreferences getState(Context context) {
        return context.getSharedPreferences(PREFS_STATE, Activity.MODE_PRIVATE);
    }

    public static long getAccountSetupTime(Context context) {
        return getState(context).getLong(PREFS_SETUP, 0);
    }

    public static void setAccountSetupTime(Context context) {
        getState(context).edit().putLong(PREFS_SETUP, System.currentTimeMillis()).commit();
    }
}
