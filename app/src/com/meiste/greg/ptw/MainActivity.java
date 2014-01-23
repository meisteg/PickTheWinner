/*
 * Copyright (C) 2014 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.meiste.greg.ptw.GAE.GaeListener;

public class MainActivity extends SherlockActivity {

    private boolean isRunning = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isRunning = true;

        if (isInitNeeded()) {
            Util.log("Initialization required");
            beginInit();
        } else {
            Util.log("No initialization needed. Starting game...");
            startGameActivity();
        }
    }

    @Override
    protected void onPause() {
        // Activity running with "no history" set, so it is safe to set flag
        // to false here. It needs to be set ASAP to handle corner case where
        // activity is dismissed just before the server responds.
        isRunning = false;
        super.onPause();
    }

    private boolean isInitNeeded() {
        long minTime = 0;
        try {
            minTime = new SimpleDateFormat("yyyy", Locale.US).parse("2014").getTime();
        } catch (final ParseException e) {
        }

        final Race[] races = Races.get(this);
        if (races.length <= 0) {
            Util.log("No valid schedule found");
            return true;
        } else if (races[0].getStartTimestamp() < minTime) {
            Util.log("Race schedule is too old!");
            return true;
        }
        return false;
    }

    private void startGameActivity() {
        if (isRunning) {
            startActivity(getIntent().setClass(this, GameActivity.class));
            finish();
        }
    }

    private void beginInit() {
        setContentView(R.layout.connecting);

        final TextView tv = (TextView) findViewById(R.id.connecting_text);
        tv.setVisibility(View.GONE);

        GAE.getInstance(getApplicationContext()).getPage(mScheduleListener, "schedule");
    }

    private void showError() {
        if (isRunning) {
            setContentView(R.layout.no_connection);

            final Button retry = (Button) findViewById(R.id.retry);
            retry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    Util.log("Retrying initialization");
                    beginInit();
                }
            });
        }
    }

    private final GaeListener mScheduleListener = new GaeListener() {
        @Override
        public void onGet(final Context context, final String json) {
            Util.log("Schedule init success!");
            Races.update(context, json);
            startGameActivity();
        }

        @Override
        public void onFailedConnect(final Context context) {
            Util.log("Schedule init failure!");
            showError();
        }

        @Override
        public void onLaunchIntent(final Intent launch) {}

        @Override
        public void onConnectSuccess(final Context context, final String json) {}
    };
}