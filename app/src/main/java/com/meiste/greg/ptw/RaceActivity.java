/*
 * Copyright (C) 2012-2014 Gregory S. Meiste  <http://gregmeiste.com>
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.actionbarsherlock.view.MenuItem;
import com.meiste.greg.ptw.view.ControllableViewPager;

public class RaceActivity extends BaseActivity {
    public static final String INTENT_ID = "race_id";
    public static final String INTENT_ALARM = "from_alarm";

    private ControllableViewPager mViewPager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_details_main);

        mViewPager = (ControllableViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new RaceFragmentPagerAdapter());
        mViewPager.setCurrentItem(getIntent().getExtras().getInt(INTENT_ID));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        setIntent(intent);
        mViewPager.setCurrentItem(getIntent().getExtras().getInt(INTENT_ID));
    }

    public void onWorkaroundNeeded() {
        mViewPager.setPagingEnabled(false);
    }

    public class RaceFragmentPagerAdapter extends FragmentStatePagerAdapter {
        public RaceFragmentPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(final int i) {
            return RaceFragment.newInstance(i);
        }

        @Override
        public int getCount() {
            return Races.get(getApplicationContext()).length;
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            final Intent homeIntent = new Intent(this, MainActivity.class);
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