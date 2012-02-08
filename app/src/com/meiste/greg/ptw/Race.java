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
import android.text.format.DateUtils;

public final class Race {
	private Context mContext;
	
	private int mRaceNum;
	private String mTrack;
	private String mName;
	private String mTv;
	private long mStart;
	private long mQuestion;
	
	public Race(Context context, int id) {
		mContext = context;
		Resources res = context.getResources();
		
		mRaceNum = res.getIntArray(R.array.schedule_race_nums)[id];
		mTrack = res.getStringArray(R.array.schedule_tracks)[id];
		mName = res.getStringArray(R.array.schedule_races)[id];
		mTv = res.getStringArray(R.array.schedule_tv)[id];
		mStart = res.getIntArray(R.array.schedule_start_times)[id] * DateUtils.SECOND_IN_MILLIS;
		mQuestion = res.getIntArray(R.array.schedule_question_times)[id] * DateUtils.SECOND_IN_MILLIS;
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
	
	public String getRaceNum() {
		return isExhibition() ? "-" : Integer.toString(mRaceNum);
	}
	
	public String getTrack() {
		return mTrack;
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
}
