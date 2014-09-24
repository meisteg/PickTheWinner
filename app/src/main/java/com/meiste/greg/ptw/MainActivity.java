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

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tagmanager.Container;
import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.GtmHelper.OnContainerAvailableListener;
import com.meiste.greg.ptw.provider.PtwContract;
import com.meiste.greg.ptw.sync.SyncAdapter;
import com.meiste.greg.ptw.tab.RuleBook;

public class MainActivity extends BaseActivity implements OnContainerAvailableListener {

    private boolean isRunning = false;
    private Account mSyncAccount;
    private Object mSyncObserverHandle;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isRunning = true;

        setContentView(R.layout.connecting);
        GtmHelper.getInstance(getApplicationContext()).getContainer(this);
    }

    @Override
    public void onContainerAvailable(final Context context, final Container container) {
        if (!container.getBoolean(GtmHelper.KEY_GAME_ENABLED)) {
            Util.log("Application has been remotely disabled!");
            setContentView(R.layout.disabled);
        } else if (isInitNeeded()) {
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
        final ContentResolver cr = getContentResolver();
        final Cursor c = cr.query(PtwContract.Race.CONTENT_SINGLE_URI, null, null, null,
                PtwContract.Race.DEFAULT_SORT);
        final int racesFound = c.getCount();
        c.close();

        long minTime = 0;
        try {
            minTime = new SimpleDateFormat("yyyy", Locale.US).parse("2014").getTime();
        } catch (final ParseException e) {
        }

        final Race[] races = Races.get(this);
        if (races.length <= 0) {
            Util.log("No valid (deprecated) schedule found");
            return true;
        } else if (races[0].getStartTimestamp() < minTime) {
            Util.log("Race schedule is too old!");
            return true;
        } else if (!RuleBook.isValid(this, minTime)) {
            Util.log("Rules missing or are too old");
            return true;
        } else if (racesFound <= 0) {
            Util.log("No valid schedule found");
            return true;
        }
        return false;
    }

    private void startGameActivity() {
        if (isRunning) {
            startActivity(getIntent().setClass(this, GameActivity.class));
            overridePendingTransition(0,0);
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
            GAE.getInstance(getApplicationContext()).getPage(mRulesListener, "rule_book");
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

    private final GaeListener mRulesListener = new GaeListener() {
        @Override
        public void onGet(final Context context, final String json) {
            Util.log("Rule Book init success!");
            RuleBook.update(context, json);

            mSyncAccount = SyncAdapter.requestSync(context, SyncAdapter.FLAG_SCHEDULE, true);
            if (mSyncAccount != null) {
                // Watch for sync state changes
                mSyncObserverHandle = ContentResolver.addStatusChangeListener(
                        ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, mSyncStatusObserver);
            } else {
                Util.log("Failed to request network sync");
                showError();
            }
        }

        @Override
        public void onFailedConnect(final Context context) {
            Util.log("Rule Book init failure!");
            showError();
        }

        @Override
        public void onLaunchIntent(final Intent launch) {}

        @Override
        public void onConnectSuccess(final Context context, final String json) {}
    };

    private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(final int which) {
            // Test the ContentResolver to see if the sync adapter is active.
            final boolean syncActive = ContentResolver.isSyncActive(
                    mSyncAccount, PtwContract.CONTENT_AUTHORITY);
            final boolean syncPending = ContentResolver.isSyncPending(
                    mSyncAccount, PtwContract.CONTENT_AUTHORITY);

            if (!syncActive && !syncPending) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSyncObserverHandle != null) {
                            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
                            mSyncObserverHandle = null;
                        }
                        mSyncAccount = null;

                        if (isInitNeeded()) {
                            showError();
                        } else {
                            startGameActivity();
                        }
                    }
                });
            }
        }
    };
}