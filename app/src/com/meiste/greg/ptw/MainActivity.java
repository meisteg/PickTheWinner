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
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;

import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitlePageIndicator.IndicatorStyle;

public class MainActivity extends FragmentActivity {
	TestFragmentAdapter mAdapter;
	ViewPager mPager;
	PageIndicator mIndicator;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Eula.show(this);
        
        setContentView(R.layout.main);
        
        mAdapter = new TestFragmentAdapter(getSupportFragmentManager());

		mPager = (ViewPager)findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);

		TitlePageIndicator indicator = (TitlePageIndicator)findViewById(R.id.indicator);
		mIndicator = indicator;
		indicator.setViewPager(mPager);

		/** TODO: Move style to layout */
		final float density = getResources().getDisplayMetrics().density;
		indicator.setBackgroundColor(0x18FF0000);
		indicator.setFooterColor(0xFFAA2222);
		indicator.setFooterLineHeight(1 * density); //1dp
		indicator.setFooterIndicatorHeight(3 * density); //3dp
		indicator.setFooterIndicatorStyle(IndicatorStyle.Underline);
		indicator.setTextColor(0xAA000000);
		indicator.setSelectedColor(0xFF000000);
		indicator.setSelectedBold(true);
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
				/** TODO */
				return true;

			case R.id.legal:
				/** TODO */
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}