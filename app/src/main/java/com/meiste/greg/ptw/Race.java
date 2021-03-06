/*
 * Copyright (C) 2012-2014 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateUtils;

public final class Race {
    public static final int NAME_SHORT = 0;
    public static final int NAME_LONG = 1;

    private static final long RECENT_TIME_POINTS = DateUtils.HOUR_IN_MILLIS * 4;
    private static final long RECENT_TIME_EXHIBITION = DateUtils.HOUR_IN_MILLIS;

    private int mId;
    private int mRaceNum;
    private String mTrackLong;
    private String mTrackShort;
    private String mName;
    private String mTv;
    private String mSize;
    private long mStart;
    private long mQuestion;
    private String mLayout;
    private String mCityState;

    public static Race getInstance(final Context context, final int id) {
        final Race[] races = Races.get(context);
        if ((races.length > id) && (id >= 0)) {
            return races[id];
        }
        return null;
    }

    public static Race getNext(final Context context, final boolean allowExhibition, final boolean allowInProgress) {
        for (final Race race : Races.get(context)) {
            if (race.isFuture()) {
                if (!allowExhibition && race.isExhibition())
                    continue;

                if (!allowInProgress && race.inProgress())
                    continue;

                return race;
            }
        }

        return null;
    }

    public boolean isFuture() {
        return System.currentTimeMillis() < mStart;
    }

    public boolean inProgress() {
        return (mQuestion < System.currentTimeMillis()) && isFuture();
    }

    public boolean isRecent() {
        final long recent = isExhibition() ? RECENT_TIME_EXHIBITION : RECENT_TIME_POINTS;
        return !isFuture() && ((System.currentTimeMillis() - mStart) < recent);
    }

    public boolean isExhibition() {
        return mRaceNum <= 0;
    }

    public boolean isInChase() {
        return mRaceNum >= 27;
    }

    public int getId() {
        return mId;
    }

    public String getRaceNum() {
        return isExhibition() ? "-" : Integer.toString(mRaceNum);
    }

    public String getTrack(final int length) {
        return (length == NAME_SHORT) ? mTrackShort : mTrackLong;
    }

    public String getName() {
        return mName;
    }

    public String getTv() {
        return mTv;
    }

    public String getStartDate(final Context context) {
        final int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        return DateUtils.formatDateTime(context, mStart, flags);
    }

    public String getStartTime(final Context context) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_NOON | DateUtils.FORMAT_NO_MIDNIGHT;
        return DateUtils.formatDateTime(context, mStart, flags);
    }

    public String getStartDateTime(final Context context) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        flags |= DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_NOON;
        flags |= DateUtils.FORMAT_NO_MIDNIGHT | DateUtils.FORMAT_SHOW_WEEKDAY;
        return DateUtils.formatDateTime(context, mStart, flags);
    }

    public long getStartTimestamp() {
        return mStart;
    }

    @SuppressLint("SimpleDateFormat")
    public String getStartYear() {
        final SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        return yearFormat.format(new Timestamp(mStart));
    }

    public CharSequence getStartRelative(final Context context, final long fudge) {
        return Util.getRelativeTimeSpanString(context, mStart + fudge);
    }

    public String getQuestionDateTime(final Context context) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        flags |= DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_NOON | DateUtils.FORMAT_NO_MIDNIGHT;
        return DateUtils.formatDateTime(context, mQuestion, flags);
    }

    public long getQuestionTimestamp() {
        return mQuestion;
    }

    public String getAbbr() {
        return mLayout;
    }

    public String getTrackSize(final Context context) {
        return context.getString(R.string.details_size, mSize);
    }

    public String getCityState() {
        return mCityState;
    }
}
