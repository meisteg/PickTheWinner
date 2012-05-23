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

import com.meiste.greg.ptw.GAE.GaeListener;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public final class Standings extends TabFragment implements GaeListener {
    public static final String FILENAME = "standings";

    private boolean mSetupNeeded;
    private boolean mChanged = false;
    private boolean mFailedConnect = false;
    private boolean mConnecting = false;

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

        TextView text = new TextView(getActivity());
        text.setGravity(Gravity.CENTER);
        text.setText("TODO");
        text.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return text;
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
    public void onFailedConnect(Context context) {
        Util.log("Standings: onFailedConnect");

        // mConnecting not set for pull to refresh case
        if (!mConnecting) {
            //mPullToRefresh.onRefreshComplete();
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
            //mAdapter.notifyDataSetChanged();
            //mPullToRefresh.onRefreshComplete();
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
