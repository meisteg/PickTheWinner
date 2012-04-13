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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;

public final class Race {
    public static final int NAME_SHORT = 0;
    public static final int NAME_LONG = 1;

    private Context mContext;

    private int mId;
    private int mRaceNum;
    private String mTrackLong;
    private String mTrackShort;
    private String mName;
    private String mTv;
    private String mSize;
    private long mStart;
    private long mQuestion;
    private Drawable mLayout;

    public Race(Context context, int id) {
        mContext = context;
        Resources res = context.getResources();

        mId = id;
        mRaceNum = res.getIntArray(R.array.schedule_race_nums)[id];
        mTrackLong = res.getStringArray(R.array.schedule_tracks)[id];
        mTrackShort = res.getStringArray(R.array.schedule_tracks_short)[id];
        mName = res.getStringArray(R.array.schedule_races)[id];
        mTv = res.getStringArray(R.array.schedule_tv)[id];
        mSize = res.getStringArray(R.array.schedule_tracks_size)[id];
        mStart = res.getIntArray(R.array.schedule_start_times)[id] * DateUtils.SECOND_IN_MILLIS;
        mQuestion = res.getIntArray(R.array.schedule_question_times)[id] * DateUtils.SECOND_IN_MILLIS;

        TypedArray layouts = res.obtainTypedArray(R.array.schedule_layouts);
        mLayout = layouts.getDrawable(id);
    }

    public static Race getNext(Context context, boolean allowExhibition, boolean allowInProgress) {
        for (int i = 0; i < getNumRaces(context); ++i) {
            Race race = new Race(context, i);

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

    public static int getNumRaces(Context context) {
        return context.getResources().getInteger(R.integer.num_races);
    }

    public boolean isFuture() {
        return System.currentTimeMillis() < mStart;
    }

    public boolean inProgress() {
        return (mQuestion < System.currentTimeMillis()) && isFuture();
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

    public String getTrack(int length) {
        return (length == NAME_SHORT) ? mTrackShort : mTrackLong;
    }

    public String getName() {
        return mName;
    }

    public String getTv() {
        return mTv;
    }

    public String getStartDate() {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        return DateUtils.formatDateTime(mContext, mStart, flags);
    }

    public String getStartTime() {
        int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_NOON_MIDNIGHT;
        return DateUtils.formatDateTime(mContext, mStart, flags);
    }

    public String getStartDateTime() {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        flags |= DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_NOON_MIDNIGHT;
        return DateUtils.formatDateTime(mContext, mStart, flags);
    }

    public long getStartTimestamp() {
        return mStart;
    }

    public String getQuestionDateTime() {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        flags |= DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_NOON_MIDNIGHT;
        return DateUtils.formatDateTime(mContext, mQuestion, flags);
    }

    public long getQuestionTimestamp() {
        return mQuestion;
    }

    public Drawable getLayout() {
        return mLayout;
    }

    public String getTrackSize() {
        return mContext.getString(R.string.details_size, mSize);
    }
}
