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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.MenuItem;

import com.meiste.greg.ptw.provider.PtwContract;
import com.meiste.greg.ptw.view.ControllableViewPager;

public class RaceActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String INTENT_ID = "race_id";
    public static final String INTENT_ALARM = "from_alarm";

    private ControllableViewPager mViewPager;
    private RaceFragmentPagerAdapter mAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_details_main);

        mAdapter = new RaceFragmentPagerAdapter();
        mViewPager = (ControllableViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAdapter);

        setDisplayHomeAsUpEnabled(true);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        setIntent(intent);
        if (mAdapter.getCursor() != null) {
            mViewPager.setCurrentItem(getIntent().getExtras().getInt(INTENT_ID), false);
        }
    }

    public void onWorkaroundNeeded() {
        mViewPager.setPagingEnabled(false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int i, final Bundle bundle) {
        return new CursorLoader(this,                                // Context
                PtwContract.Race.CONTENT_URI,                        // URI
                new String[]{PtwContract.Race.COLUMN_NAME_RACE_ID},  // Projection
                null,                                                // Selection
                null,                                                // Selection args
                PtwContract.Race.DEFAULT_SORT);                      // Sort
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> cursorLoader, final Cursor cursor) {
        final Cursor prevCursor = mAdapter.getCursor();
        mAdapter.changeCursor(cursor);
        if ((prevCursor == null) && (cursor != null)) {
            mViewPager.setCurrentItem(getIntent().getExtras().getInt(INTENT_ID), false);
        }
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> cursorLoader) {
        mAdapter.changeCursor(null);
    }

    public class RaceFragmentPagerAdapter extends FragmentStatePagerAdapter {
        private Cursor mCursor;

        public RaceFragmentPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(final int i) {
            // Shouldn't happen
            if (mCursor == null) return null;

            return RaceFragment.newInstance(i);
        }

        @Override
        public int getCount() {
            return (mCursor == null) ? 0 : mCursor.getCount();
        }

        public void changeCursor(final Cursor c) {
            if (mCursor != c) {
                mCursor = c;
                notifyDataSetChanged();
            }
        }

        public Cursor getCursor() {
            return mCursor;
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