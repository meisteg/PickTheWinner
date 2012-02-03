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
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public final class Race {
	private int mRaceNum;
	private String mTrack;
	private String mName;
	private String mTv;
	private Time mStartTime;
	private Time mQuestionTime;
	
	public Race(Context context, int id) {
		Resources res = context.getResources();
		
		mRaceNum = res.getIntArray(R.array.schedule_race_nums)[id];
		mTrack = res.getStringArray(R.array.schedule_tracks)[id];
		mName = res.getStringArray(R.array.schedule_races)[id];
		mTv = res.getStringArray(R.array.schedule_tv)[id];
		
		mStartTime = new Time();
		String localTimeZone = mStartTime.timezone;
		mStartTime.parse(res.getStringArray(R.array.schedule_start_times)[id]);
		mStartTime.switchTimezone(localTimeZone);
		
		mQuestionTime = new Time();
		mQuestionTime.parse(res.getStringArray(R.array.schedule_question_times)[id]);
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
		Time now = new Time();
		now.setToNow();
		
		return now.before(mStartTime);
	}
	
	public boolean inProgress() {
		Time now = new Time();
		now.setToNow();
		
		return now.after(mQuestionTime) && isFuture();
	}
	
	public boolean isExhibition() {
		return mRaceNum <= 0;
	}
	
	public String getTrack() {
		return mTrack;
	}
	
	public String getName() {
		return mName;
	}
	
	public View getView(LayoutInflater inflater) {
		View v = inflater.inflate(R.layout.schedule_row, null);
		
		TextView raceNum = (TextView) v.findViewById(R.id.race_num);
		raceNum.setText(isExhibition() ? "-" : Integer.toString(mRaceNum));
		
		TextView startTime = (TextView) v.findViewById(R.id.race_date);
		startTime.setText(mStartTime.format3339(true));
		
		TextView track = (TextView) v.findViewById(R.id.race_track);
		track.setText(mTrack);
		
		return v;
	}
}
