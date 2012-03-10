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

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TitlePageIndicator;

public class MainActivity extends SherlockFragmentActivity implements Eula.OnEulaAgreedTo {
	
	public static final String INTENT_TAB = "tab_select";
	private final String LAST_TAB = "tab.last";
	
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

		mPager = (ViewPager)findViewById(R.id.pager);
		mPager.setAdapter(new TabFragmentAdapter(getSupportFragmentManager(), this));

		mIndicator = (TitlePageIndicator)findViewById(R.id.indicator);
		mIndicator.setViewPager(mPager);
		mIndicator.setCurrentItem(getTab(getIntent()));
    }
    
    @Override
	public void onPause() {
		super.onPause();

		Util.log("Saving state: tab=" + mPager.getCurrentItem());
		Util.getState(this).edit().putInt(LAST_TAB, mPager.getCurrentItem()).commit();
		
		// Hide dialogs to prevent window leaks on orientation changes
		Eula.hide();
		if ((mLegalDialog != null) && (mLegalDialog.isShowing())) {
			mLegalDialog.dismiss();
		}
	}
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // There is a bug in the Android compatibility library that causes
    	// a null pointer exception when the super's onSaveInstanceState is
    	// called. See: http://stackoverflow.com/questions/8748064
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.menu, menu);
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
		QuestionAlarm.set(this);
	}
	
	private int getTab(Intent intent) {
		// Recent applications caches intent with extras. Only want to listen
		// to INTENT_TAB extra if launched from notification.
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
			int intent_tab = intent.getIntExtra(INTENT_TAB, -1);
			if (intent_tab >= 0) {
				return intent_tab;
			}
		}
		
		return Util.getState(this).getInt(LAST_TAB, 0);
	}
}