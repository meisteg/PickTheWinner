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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.meiste.greg.ptw.GAE.GaeListener;
import com.squareup.otto.Subscribe;

public final class Schedule extends TabFragment implements OnRefreshListener<ListView>, GaeListener  {

    private PullToRefreshListView mPullToRefresh;
    private RaceItemAdapter mAdapter;

    public static Schedule newInstance(Context context) {
        Schedule fragment = new Schedule();
        fragment.setTitle(context.getString(R.string.tab_schedule));

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.schedule, container, false);

        mPullToRefresh = (PullToRefreshListView) v.findViewById(R.id.schedule);
        mPullToRefresh.setOnRefreshListener(this);

        ListView lv = mPullToRefresh.getRefreshableView();
        mAdapter = new RaceItemAdapter(getActivity(), R.layout.schedule_row);
        lv.setAdapter(mAdapter);

        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
                Util.log("Starting activity for race " + id);

                Intent intent = new Intent(getActivity(), RaceActivity.class);
                intent.putExtra(RaceActivity.INTENT_ID, (int)id);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
            }
        });

        BusProvider.getInstance().register(this);
        return v;
    }

    @Override
    public void onDestroyView() {
        BusProvider.getInstance().unregister(this);
        super.onDestroyView();
    }

    @Override
    public void onRefresh(PullToRefreshBase<ListView> refreshView) {
        GAE.getInstance(getActivity()).getPage(this, "schedule");
    }

    @Subscribe
    public void onScheduleUpdate(ScheduleUpdateEvent event) {
        Util.log("Schedule: onScheduleUpdate");
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onFailedConnect(Context context) {
        mPullToRefresh.onRefreshComplete();
        Toast.makeText(context, R.string.failed_connect, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGet(Context context, String json) {
        Races.update(context, json);
        RaceAlarm.reset(context);
        mPullToRefresh.onRefreshComplete();
        BusProvider.getInstance().post(new ScheduleUpdateEvent());
    }

    @Override
    public void onLaunchIntent(Intent launch) {}

    @Override
    public void onConnectSuccess(Context context, String json) {}
}
