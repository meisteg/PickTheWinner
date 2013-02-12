/*
 * Copyright (C) 2012-2013 Gregory S. Meiste  <http://gregmeiste.com>
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.meiste.greg.ptw.GAE.GaeListener;

public final class Schedule extends TabFragment implements OnRefreshListener<ListView>, GaeListener  {

    private PullToRefreshListView mPullToRefresh;
    private RaceItemAdapter mAdapter;

    public static Schedule newInstance(final Context context) {
        final Schedule fragment = new Schedule();
        fragment.setTitle(context.getString(R.string.tab_schedule));

        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.schedule, container, false);

        mPullToRefresh = (PullToRefreshListView) v.findViewById(R.id.schedule);
        mPullToRefresh.setOnRefreshListener(this);

        final ListView lv = mPullToRefresh.getRefreshableView();
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
    public void onRefresh(final PullToRefreshBase<ListView> refreshView) {
        GAE.getInstance(getActivity()).getPage(this, "schedule");
    }

    @Override
    public void onFailedConnect(final Context context) {
        mPullToRefresh.onRefreshComplete();
        Toast.makeText(context, R.string.failed_connect, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGet(final Context context, final String json) {
        Races.update(context, json);
        RaceAlarm.reset(context);
        mPullToRefresh.onRefreshComplete();
        context.sendBroadcast(new Intent(PTW.INTENT_ACTION_SCHEDULE));
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
