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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public final class Questions extends TabFragment {
	private int mWinner;
	
	public static Questions newInstance(Context context) {
		Questions fragment = new Questions();
		fragment.setTitle(context.getString(R.string.tab_questions));
		
		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v;
		Race race = Race.getNext(getActivity(), false, true);
		
		if (race == null) {
			return inflater.inflate(R.layout.questions_no_race, container, false);
		} else if (race.inProgress()) {
			// TODO: Only show form if user hasn't submitted answers yet
			v = inflater.inflate(R.layout.questions, container, false);
			
			Spinner winner = (Spinner) v.findViewById(R.id.winner);
			winner.setAdapter(new DriverAdapter(getActivity(), android.R.layout.simple_spinner_item));
			winner.setOnItemSelectedListener(new WinnerSelectedListener());
		} else {
			v = inflater.inflate(R.layout.questions_not_yet, container, false);
			
			TextView time = (TextView) v.findViewById(R.id.questiontime);
			time.setText(race.getQuestionDateTime());
		}
		
		TextView name = (TextView) v.findViewById(R.id.racename);
		name.setText(race.getName());
		
		TextView track = (TextView) v.findViewById(R.id.racetrack);
		track.setText(race.getTrack(Race.NAME_LONG));
		
		return v;
	}
	
	private class DriverAdapter extends ArrayAdapter<Driver> {
		private Context mContext;
		
		public DriverAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			mContext = context;
			
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}
		
		@Override
		public int getCount() {
			return Driver.getNumDrivers(mContext);
		}
		
		@Override
		public Driver getItem(int position) {
			return new Driver(mContext, position);
		}
	}
	
	private class WinnerSelectedListener implements OnItemSelectedListener {
	    public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
	    	Driver driver = (Driver) parent.getItemAtPosition(pos);
	    	mWinner = driver.getNumber();
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	        // Do nothing.
	    }
	}
}
