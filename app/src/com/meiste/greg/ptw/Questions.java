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
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.meiste.greg.ptw.TabFragmentAdapter.FragmentListener;

public final class Questions extends TabFragment implements View.OnClickListener {
	private int mWinner;
	private int mMostLaps;
	private int mNumLeaders;
	
	private FragmentListener mFragmentListener;
	
	public static Questions newInstance(Context context, FragmentListener fl) {
		Questions fragment = new Questions();
		fragment.setTitle(context.getString(R.string.tab_questions));
		fragment.mFragmentListener = fl;
		
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
			
			TextView q2 = (TextView) v.findViewById(R.id.question2);
			q2.setText(getActivity().getString(R.string.questions_2, "TODO"));
			
			TextView q3 = (TextView) v.findViewById(R.id.question3);
			q3.setText(getActivity().getString(R.string.questions_3, "TODO"));
			
			Spinner mostlaps = (Spinner) v.findViewById(R.id.mostlaps);
			mostlaps.setAdapter(new DriverAdapter(getActivity(), android.R.layout.simple_spinner_item));
			mostlaps.setOnItemSelectedListener(new MostLapsSelectedListener());
			
			Spinner numleaders = (Spinner) v.findViewById(R.id.numleaders);
		    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
		    		getActivity(), R.array.num_leaders, android.R.layout.simple_spinner_item);
		    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		    numleaders.setAdapter(adapter);
		    numleaders.setOnItemSelectedListener(new NumLeadersSelectedListener());
		    
		    Button send = (Button) v.findViewById(R.id.send);
			send.setOnClickListener(this);
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
	
	private class MostLapsSelectedListener implements OnItemSelectedListener {
	    public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
	    	Driver driver = (Driver) parent.getItemAtPosition(pos);
	    	mMostLaps = driver.getNumber();
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	        // Do nothing.
	    }
	}
	
	private class NumLeadersSelectedListener implements OnItemSelectedListener {
	    public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
	    	mNumLeaders = pos;
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	        // Do nothing.
	    }
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Util.log("Sending: a1=" + mWinner + ", a4=" + mMostLaps + ", a5=" + mNumLeaders);
		Toast.makeText(getActivity(), "TODO: Actually send answers",
				Toast.LENGTH_SHORT).show();
		mFragmentListener.onChangedFragment();
	}
}
