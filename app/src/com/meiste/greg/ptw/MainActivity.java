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

import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitlePageIndicator.IndicatorStyle;

public class MainActivity extends FragmentActivity {
	private final String TAG = "PickTheWinner";
	private final boolean DEBUG = true;
	
	public static final String PREFERENCES_STATE = "state";
	private final String PREFERENCE_LAST_TAB = "tab.last";
	
	private TabFragmentAdapter mAdapter;
	private ViewPager mPager;
	private TitlePageIndicator mIndicator;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Eula.show(this);
        
        setContentView(R.layout.main);
        
        final SharedPreferences prefs = getSharedPreferences(PREFERENCES_STATE,
        		Activity.MODE_PRIVATE);
        
        List<TabFragment> fragments = new Vector<TabFragment>();
        fragments.add(TestFragment.newInstance("RULE BOOK"));
        fragments.add(TestFragment.newInstance("QUESTIONS"));
        fragments.add(TestFragment.newInstance("STANDINGS"));
        fragments.add(TestFragment.newInstance("SCHEDULE"));
        fragments.add(TestFragment.newInstance("SUGGEST"));
        mAdapter = new TabFragmentAdapter(getSupportFragmentManager(), fragments);

		mPager = (ViewPager)findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);

		mIndicator = (TitlePageIndicator)findViewById(R.id.indicator);
		mIndicator.setViewPager(mPager);
		mIndicator.setCurrentItem(prefs.getInt(PREFERENCE_LAST_TAB, 0));

		/** TODO: Move style to layout */
		final float density = getResources().getDisplayMetrics().density;
		mIndicator.setBackgroundColor(0x18FF0000);
		mIndicator.setFooterColor(0xFFAA2222);
		mIndicator.setFooterLineHeight(1 * density); //1dp
		mIndicator.setFooterIndicatorHeight(3 * density); //3dp
		mIndicator.setFooterIndicatorStyle(IndicatorStyle.Underline);
		mIndicator.setTextColor(0xAA000000);
		mIndicator.setSelectedColor(0xFF000000);
		mIndicator.setSelectedBold(true);
    }
    
    @Override
	public void onPause() {
		super.onPause();

		if (DEBUG) Log.d(TAG, "Saving state: tab=" + mPager.getCurrentItem());
		
		final SharedPreferences prefs = getSharedPreferences(PREFERENCES_STATE,
        		Activity.MODE_PRIVATE);
		prefs.edit().putInt(PREFERENCE_LAST_TAB, mPager.getCurrentItem()).commit();
	}
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.settings:
				startActivity(new Intent(this, EditPreferences.class));
				return true;

			case R.id.legal:
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	            builder.setTitle(R.string.legal);
	            builder.setCancelable(true);
	            builder.setPositiveButton(R.string.ok, null);
	            builder.setMessage(R.string.legal_content);
	            builder.create().show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}