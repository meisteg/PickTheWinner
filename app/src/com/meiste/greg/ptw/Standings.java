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

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.meiste.greg.ptw.GAE.GaeListener;

public final class Standings extends TabFragment implements OnRefreshListener, GaeListener {
    public static final String FILENAME = "standings";

    private boolean mSetupNeeded;
    private boolean mChanged = false;
    private boolean mFailedConnect = false;
    private boolean mConnecting = false;
    private PullToRefreshListView mPullToRefresh;
    private PlayerAdapter mAdapter;
    private TextView mAfterRace;

    public static Standings newInstance(Context context) {
        Standings fragment = new Standings();
        fragment.setTitle(context.getString(R.string.tab_standings));

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mSetupNeeded = GAE.isAccountSetupNeeded(getActivity());
        mChanged = false;
        setRetainInstance(true);

        if (mSetupNeeded)
            return inflater.inflate(R.layout.no_account, container, false);
        else if (mFailedConnect) {
            mFailedConnect = false;
            View v = inflater.inflate(R.layout.no_connection, container, false);

            Button retry = (Button) v.findViewById(R.id.retry);
            retry.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mChanged = true;
                    notifyChanged();
                }
            });

            return v;
        } else if (!isStandingsPresent() || mConnecting) {
            if (!mConnecting) {
                mConnecting = true;
                GAE.getInstance(getActivity()).getPage(this, "standings");
            }
            return inflater.inflate(R.layout.connecting, container, false);
        }

        View v = inflater.inflate(R.layout.standings, container, false);
        mPullToRefresh = (PullToRefreshListView) v.findViewById(R.id.standings);
        mPullToRefresh.setOnRefreshListener(this);

        ListView lv = mPullToRefresh.getRefreshableView();
        View header = inflater.inflate(R.layout.standings_header, lv, false);
        mAdapter = new PlayerAdapter(getActivity(), R.layout.schedule_row);
        mAfterRace = (TextView) header.findViewById(R.id.after);
        mAfterRace.setText(getActivity().getString(R.string.standings_after, mAdapter.getRaceAfter()));
        lv.addHeaderView(header, null, false);
        lv.setAdapter(mAdapter);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSetupNeeded != GAE.isAccountSetupNeeded(getActivity())) {
            Util.log("Standings: onResume: notifyChanged");
            notifyChanged();
        }
    }

    @Override
    public boolean isChanged() {
        // Must check for account status change separately in case another
        // tab noticed the change and already called notifyChanged().
        return mChanged || (mSetupNeeded != GAE.isAccountSetupNeeded(getActivity()));
    }

    private boolean isStandingsPresent() {
        File file = new File(getActivity().getFilesDir(), FILENAME);
        return file.exists();
    }

    @Override
    public void onRefresh() {
        GAE.getInstance(getActivity()).getPage(this, "standings");
    }

    @Override
    public void onFailedConnect(Context context) {
        Util.log("Standings: onFailedConnect");

        // mConnecting not set for pull to refresh case
        if (!mConnecting) {
            mPullToRefresh.onRefreshComplete();
            Toast.makeText(context, R.string.failed_connect, Toast.LENGTH_SHORT).show();
        }
        // Verify application wasn't closed before callback returned
        else if (getActivity() != null) {
            mConnecting = false;
            mFailedConnect = mChanged = true;
            notifyChanged();
        }
    }

    @Override
    public void onGet(Context context, String json) {
        Util.log("Standings: onGet");

        try {
            FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(json.getBytes());
            fos.close();
        } catch (Exception e) {
            Util.log("Failed to save new standings");
        }

        // mConnecting not set for pull to refresh case
        if (!mConnecting) {
            mAdapter.notifyDataSetChanged();
            mAfterRace.setText(context.getString(R.string.standings_after, mAdapter.getRaceAfter()));
            mPullToRefresh.onRefreshComplete();
        }
        // Verify application wasn't closed before callback returned
        else if (getActivity() != null) {
            mConnecting = false;
            mChanged = true;
            notifyChanged();
        }
    }

    @Override
    public void onLaunchIntent(Intent launch) {}

    @Override
    public void onConnectSuccess(Context context, String json) {}
}
