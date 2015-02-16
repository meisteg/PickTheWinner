/*
 * Copyright (C) 2014-2015 Gregory S. Meiste  <http://gregmeiste.com>
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

import android.accounts.AccountManager;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import com.google.android.gms.analytics.GoogleAnalytics;

public abstract class BaseActivity extends ActionBarActivity {

    public static final int REQUEST_ACCOUNT_CODE = 1316;

    @Override
    public void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_ACCOUNT_CODE && resultCode == RESULT_OK) {
            final String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            final Intent intent = new Intent(this, AccountsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(AccountsActivity.EXTRA_ACCOUNT_NAME, accountName);
            startActivity(intent);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected void setDisplayHomeAsUpEnabled(final boolean showHomeAsUp) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(showHomeAsUp);
        }
    }
}
