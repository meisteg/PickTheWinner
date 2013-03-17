/*
 * Copyright (C) 2012-2013 Gregory S. Meiste  <http://gregmeiste.com>
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public final class Util {

    private final static String PREFS_STATE = "state";
    private final static String PREFS_SETUP = "setup";

    public static boolean LOGGING_ENABLED = BuildConfig.DEBUG;

    public static void log(final String msg) {
        if (LOGGING_ENABLED) Log.d(PTW.TAG, msg);
    }

    public static SharedPreferences getState(final Context context) {
        return context.getSharedPreferences(PREFS_STATE, Activity.MODE_PRIVATE);
    }

    public static long getAccountSetupTime(final Context context) {
        return getState(context).getLong(PREFS_SETUP, 0);
    }

    public static void setAccountSetupTime(final Context context) {
        getState(context).edit().putLong(PREFS_SETUP, System.currentTimeMillis()).apply();
    }

    public static View getAccountSetupView(final Context context,
            final LayoutInflater inflater, final ViewGroup container) {
        final View v = inflater.inflate(R.layout.no_account, container, false);
        final TextView textView = (TextView) v.findViewById(R.id.setup_text);
        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent intent = new Intent(context, AccountsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                context.startActivity(intent);
            }
        });
        return v;
    }

    /* Android's DateUtils.getRelativeTimeSpanString implementation is broken
     * prior to Android 4.0. To support older Android versions, copy the
     * correct implementation here. However, to minimize the number of internal
     * string resources that would also need to be copied, this function was
     * modified to only support this app's intended usage.
     */
    public static CharSequence getRelativeTimeSpanString(final Context context, final long time) {
        final long now = System.currentTimeMillis();
        final boolean past = (now >= time);
        final long duration = Math.abs(now - time);

        int resId;
        long count;
        if (duration < DateUtils.HOUR_IN_MILLIS) {
            count = duration / DateUtils.MINUTE_IN_MILLIS;
            if (past) {
                resId = R.plurals.num_minutes_ago;
            } else {
                resId = R.plurals.in_num_minutes;
            }
        } else if (duration < DateUtils.DAY_IN_MILLIS) {
            count = duration / DateUtils.HOUR_IN_MILLIS;
            if (past) {
                resId = R.plurals.num_hours_ago;
            } else {
                resId = R.plurals.in_num_hours;
            }
        } else {
            count = getNumberOfDaysPassed(time, now);
            if (past) {
                resId = R.plurals.num_days_ago;
            } else {
                resId = R.plurals.in_num_days;
            }
        }

        final String format = context.getResources().getQuantityString(resId, (int) count);
        return String.format(format, count);
    }

    private synchronized static long getNumberOfDaysPassed(final long date1, final long date2) {
        if (sThenTime == null) {
            sThenTime = new Time();
        }
        sThenTime.set(date1);
        final int day1 = Time.getJulianDay(date1, sThenTime.gmtoff);
        sThenTime.set(date2);
        final int day2 = Time.getJulianDay(date2, sThenTime.gmtoff);
        return Math.abs(day2 - day1);
    }

    private static Time sThenTime;
}
