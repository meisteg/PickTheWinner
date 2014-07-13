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
import com.meiste.greg.ptw.GAE;
import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.PTW;
import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.Race;
import com.meiste.greg.ptw.RaceActivity;
import com.meiste.greg.ptw.RaceItemAdapter;
import com.meiste.greg.ptw.Races;
import com.meiste.greg.ptw.Util;

public final class Schedule extends TabFragment implements OnRefreshListener, GaeListener  {

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
                Util.log("Starting activity for race " + id);

                final Intent intent = new Intent(getActivity(), RaceActivity.class);
                intent.putExtra(RaceActivity.INTENT_ID, (int)id);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
            }
        });

        if (mNeedScroll) {
            final Race race = Race.getNext(getActivity(), true, true);
            if (race != null) {
                lv.setSelection(race.getId());
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

        GAE.getInstance(getActivity()).getPage(this, "schedule");
    }

    @Override
    public void onFailedConnect(final Context context) {
        mSwipeRefreshWidget.setRefreshing(false);
        mSwipeRefreshWidget.setEnabled(BuildConfig.DEBUG);
        Toast.makeText(context, R.string.failed_connect, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGet(final Context context, final String json) {
        Races.update(context, json);
        mSwipeRefreshWidget.setRefreshing(false);
        mSwipeRefreshWidget.setEnabled(BuildConfig.DEBUG);
    }

    @Override
    public void onLaunchIntent(final Intent launch) {}

    @Override
    public void onConnectSuccess(final Context context, final String json) {}

    private final BroadcastReceiver mScheduleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(PTW.INTENT_ACTION_SCHEDULE)) {
                Util.log("Schedule.onReceive: Schedule Updated");
                mAdapter.notifyDataSetChanged();
            }
        }
    };
}
