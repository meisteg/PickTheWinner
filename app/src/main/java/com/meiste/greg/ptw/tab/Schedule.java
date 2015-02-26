/*
 * Copyright (C) 2012-2015 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.greg.ptw.tab;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.meiste.greg.ptw.BuildConfig;
import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.Race;
import com.meiste.greg.ptw.RaceActivity;
import com.meiste.greg.ptw.RaceItemAdapter;
import com.meiste.greg.ptw.Util;
import com.meiste.greg.ptw.provider.PtwContract;
import com.meiste.greg.ptw.sync.SyncAdapter;

public final class Schedule extends TabFragment implements
        OnRefreshListener, LoaderManager.LoaderCallbacks<Cursor>  {

    private SwipeRefreshLayout mSwipeRefreshWidget;
    private RaceItemAdapter mAdapter;
    private boolean mNeedScroll = true;
    private Account mSyncAccount;
    private Object mSyncObserverHandle;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        setRetainInstance(true);
        final View v = inflater.inflate(R.layout.list, container, false);

        mSwipeRefreshWidget = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mSwipeRefreshWidget.setOnRefreshListener(this);
        mSwipeRefreshWidget.setColorSchemeResources(R.color.refresh1, R.color.refresh2,
                R.color.refresh3, R.color.refresh4);
        mSwipeRefreshWidget.setEnabled(BuildConfig.DEBUG);

        final ListView lv = (ListView) v.findViewById(R.id.content);
        mAdapter = new RaceItemAdapter(getActivity());
        lv.setAdapter(mAdapter);

        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View v, final int pos, final long id) {
                Util.log("Starting activity for race " + pos);

                final Intent intent = new Intent(getActivity(), RaceActivity.class);
                intent.putExtra(RaceActivity.INTENT_ID, pos);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
            }
        });

        if (mNeedScroll) {
            final Race race = Race.getNext(getActivity(), true, true);
            if (race != null) {
                // If possible set previous race so "recent" race is shown (if applicable)
                final int id = race.getId();
                lv.setSelection(id > 0 ? id - 1 : id);
            }
            mNeedScroll = false;
        }

        getLoaderManager().initLoader(0, null, this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSyncStatusObserver.onStatusChanged(0);

        // Watch for sync state changes
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    @Override
    public void onRefresh() {
        mSyncAccount = SyncAdapter.requestSync(getActivity(), SyncAdapter.FLAG_SCHEDULE, true);
        if (mSyncAccount != null) {
            // Prevent multiple refresh attempts
            mSwipeRefreshWidget.setEnabled(false);
        } else {
            mSwipeRefreshWidget.setRefreshing(false);
            mSwipeRefreshWidget.setEnabled(BuildConfig.DEBUG);
            Toast.makeText(getActivity(), R.string.failed_connect, Toast.LENGTH_SHORT).show();
        }
    }

    private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(final int which) {
            getActivity().runOnUiThread(new Runnable() {
                // The SyncAdapter runs on a background thread. To update the
                // UI, onStatusChanged() runs on the UI thread.
                @Override
                public void run() {
                    if (mSyncAccount == null) {
                        mSwipeRefreshWidget.setRefreshing(false);
                        mSwipeRefreshWidget.setEnabled(BuildConfig.DEBUG);
                    } else {
                        // Test the ContentResolver to see if the sync adapter is active.
                        final boolean syncActive = ContentResolver.isSyncActive(
                                mSyncAccount, PtwContract.CONTENT_AUTHORITY);
                        final boolean syncPending = ContentResolver.isSyncPending(
                                mSyncAccount, PtwContract.CONTENT_AUTHORITY);
                        if (!syncActive && !syncPending) {
                            mSwipeRefreshWidget.setRefreshing(false);
                            mSwipeRefreshWidget.setEnabled(BuildConfig.DEBUG);
                            mSyncAccount = null;
                        }
                    }
                }
            });
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return new CursorLoader(getActivity(),    // Context
                PtwContract.Race.CONTENT_URI,     // URI
                PtwContract.Race.PROJECTION_ALL,  // Projection
                null,                             // Selection
                null,                             // Selection args
                PtwContract.Race.DEFAULT_SORT);   // Sort
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }
}
