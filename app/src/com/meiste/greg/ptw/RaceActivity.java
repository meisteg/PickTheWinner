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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

public class RaceActivity extends SherlockActivity {
	public static final String INTENT_ID = "race_id";
	public static final String INTENT_ALARM = "from_alarm";
	private Race mRace;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_details);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        mRace = new Race(this, getIntent().getIntExtra(INTENT_ID, 0));
        
        TextView raceNum = (TextView) findViewById(R.id.race_num);
        TextView inTheChase = (TextView) findViewById(R.id.race_in_chase);
    	TextView trackSize = (TextView) findViewById(R.id.race_track_size);
    	TextView startTime = (TextView) findViewById(R.id.race_time);
    	TextView name = (TextView) findViewById(R.id.race_name);
    	TextView trackLong = (TextView) findViewById(R.id.race_track);
    	TextView tv = (TextView) findViewById(R.id.race_tv);
    	ImageView img = (ImageView) findViewById(R.id.race_img);
    	
    	if (mRace.isExhibition()) {
    		raceNum.setVisibility(View.GONE);
    	} else {
    		raceNum.setText(getString(R.string.race_num, mRace.getRaceNum()));
    		
    		if (mRace.isInChase())
    			inTheChase.setVisibility(View.VISIBLE);
    	}
		trackSize.setText(mRace.getTrackSize());
		startTime.setText(mRace.getStartDateTime());
		name.setText(mRace.getName());
		trackLong.setText(mRace.getTrack(Race.NAME_LONG));
		tv.setText(getString(R.string.details_tv, mRace.getTv()));
		img.setImageDrawable(mRace.getLayout());
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == android.R.id.home) {
            Intent homeIntent = new Intent(this, MainActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
            
            if (getIntent().getBooleanExtra(INTENT_ALARM, false)) {
            	// Finish activity so back button works as expected
                finish();
            }
            return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
}