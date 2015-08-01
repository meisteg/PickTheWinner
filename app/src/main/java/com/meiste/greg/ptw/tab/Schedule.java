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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import com.meiste.greg.ptw.PTW;
import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.Race;
import com.meiste.greg.ptw.RaceActivity;
import com.meiste.greg.ptw.RaceItemAdapter;
import com.meiste.greg.ptw.Races;
import com.meiste.greg.ptw.backend.Backend;
import com.meiste.greg.ptw.backend.ResultGet;
import com.meiste.greg.ptw.backend.ResultCallback;

import timber.log.Timber;

public final class Schedule extends TabFragment implements OnRefreshListener, ResultCallback<ResultGet> {

    private SwipeRefreshLayout mSwipeRefreshWidget;
    private RaceItemAdapter mAdapter;
    private boolean mNeedScroll = true;

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
        mAdapter = new RaceItemAdapter(getActivity(), R.layout.schedule_row);
        lv.setAdapter(mAdapter);

        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View v, final int pos, final long id) {
                Timber.i("Starting activity for race %d", id);

                final Intent intent = new Intent(getActivity(), RaceActivity.class);
                intent.putExtra(RaceActivity.INTENT_ID, (int)id);
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

        final IntentFilter filter = new IntentFilter(PTW.INTENT_ACTION_SCHEDULE);
        getActivity().registerReceiver(mScheduleUpdateReceiver, filter);
        return v;
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(mScheduleUpdateReceiver);
        super.onDestroyView();
    }

    @Override
    public void onRefresh() {
        // Prevent multiple refresh attempts
        mSwipeRefreshWidget.setEnabled(false);

        Backend.getInstance(getActivity()).getPage("schedule").setResultCallback(this);
    }

    private final BroadcastReceiver mScheduleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(PTW.INTENT_ACTION_SCHEDULE)) {
                Timber.d("onReceive: Schedule Updated");
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onResult(final ResultGet result) {
        Timber.v("onResult");
        if (getActivity() != null) {
            if (result.isSuccess()) {
                Races.update(getActivity(), result.getJson());
            } else {
                Toast.makeText(getActivity(), R.string.failed_connect, Toast.LENGTH_SHORT).show();
            }

            mSwipeRefreshWidget.setRefreshing(false);
            mSwipeRefreshWidget.setEnabled(BuildConfig.DEBUG);
        }
    }
}
