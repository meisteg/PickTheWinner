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

import com.viewpagerindicator.TitlePageIndicator;

public class MainActivity extends FragmentActivity implements Eula.OnEulaAgreedTo {
	public static final String PREFERENCES_STATE = "state";
	private final String PREFERENCE_LAST_TAB = "tab.last";
	
	private TabFragmentAdapter mAdapter;
	private ViewPager mPager;
	private TitlePageIndicator mIndicator;
	private AlertDialog mLegalDialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Eula.show(this)) {
            onEulaAgreedTo();
        }
        
        setContentView(R.layout.main);
        
        final SharedPreferences prefs = getSharedPreferences(PREFERENCES_STATE,
        		Activity.MODE_PRIVATE);
        
        List<TabFragment> fragments = new Vector<TabFragment>();
        fragments.add(RuleBook.newInstance(getApplicationContext()));
        fragments.add(TestFragment.newInstance("QUESTIONS"));
        fragments.add(TestFragment.newInstance("STANDINGS"));
        fragments.add(Schedule.newInstance(getApplicationContext()));
        fragments.add(Suggest.newInstance(getApplicationContext()));
        mAdapter = new TabFragmentAdapter(getSupportFragmentManager(), fragments);

		mPager = (ViewPager)findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);

		mIndicator = (TitlePageIndicator)findViewById(R.id.indicator);
		mIndicator.setViewPager(mPager);
		mIndicator.setCurrentItem(prefs.getInt(PREFERENCE_LAST_TAB, 0));
    }
    
    @Override
	public void onPause() {
		super.onPause();

		Util.log("Saving state: tab=" + mPager.getCurrentItem());
		
		final SharedPreferences prefs = getSharedPreferences(PREFERENCES_STATE,
        		Activity.MODE_PRIVATE);
		prefs.edit().putInt(PREFERENCE_LAST_TAB, mPager.getCurrentItem()).commit();
		
		// Hide dialogs to prevent window leaks on orientation changes
		Eula.hide();
		if ((mLegalDialog != null) && (mLegalDialog.isShowing())) {
			mLegalDialog.dismiss();
		}
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
	            mLegalDialog = builder.create();
	            mLegalDialog.show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onEulaAgreedTo() {
		RaceAlarm.set(this);
	}
}