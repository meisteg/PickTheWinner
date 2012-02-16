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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

public class RaceActivity extends FragmentActivity {
	public static final String INTENT_ID = "race_id";
	private Race mRace;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_details);
        
        mRace = new Race(this, getIntent().getIntExtra(INTENT_ID, 0));
        
        TextView raceNum = (TextView) findViewById(R.id.race_num);
    	TextView startDate = (TextView) findViewById(R.id.race_date);
    	TextView startTime = (TextView) findViewById(R.id.race_time);
    	TextView name = (TextView) findViewById(R.id.race_name);
    	TextView trackLong = (TextView) findViewById(R.id.race_track);
    	TextView tv = (TextView) findViewById(R.id.race_tv);
    	
    	if (mRace.isExhibition()) {
    		raceNum.setVisibility(View.GONE);
    	} else {
    		raceNum.setText(mRace.getRaceNum());
    	}
		startDate.setText(mRace.getStartDate());
		startTime.setText(mRace.getStartTime());
		name.setText(mRace.getName());
		trackLong.setText(mRace.getTrack(Race.NAME_LONG));
		tv.setText(mRace.getTv());
    }
}