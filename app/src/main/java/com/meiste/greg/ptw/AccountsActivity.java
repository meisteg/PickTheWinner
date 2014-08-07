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
package com.meiste.greg.ptw;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.gcm.Gcm;

public class AccountsActivity extends BaseActivity implements GaeListener {

    private static final int REQUEST_LAUNCH_INTENT = 0;

    public static final String EXTRA_ACCOUNT_NAME = "account";

    private String mAccountName;
    private GAE mGae;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connecting);

        mGae = GAE.getInstance(getApplicationContext());
        mAccountName = getIntent().getStringExtra(EXTRA_ACCOUNT_NAME);
        if (!TextUtils.isEmpty(mAccountName)) {
            mGae.connect(this, mAccountName);
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_LAUNCH_INTENT) {
            Util.log("onActivityResult: resultCode=" + resultCode + ", mAccountName=" + mAccountName);

            if ((resultCode == RESULT_OK) && (mAccountName != null)) {
                mGae.connect(this, mAccountName);
            } else {
                onFailedConnect(this);
            }
        }
    }

    @Override
    public void onFailedConnect(final Context context) {
        // If the connect succeeds but the get history fails, don't show error toast
        if (GAE.isAccountSetupNeeded(context))
            Toast.makeText(this, R.string.failed_connect, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onLaunchIntent(final Intent launch) {
        startActivityForResult(launch, REQUEST_LAUNCH_INTENT);
    }

    @Override
    public void onConnectSuccess(final Context context, final String json) {
        Util.setAccountSetupTime(context);
        mGae.getPage(this, "history");
    }

    @Override
    public void onGet(final Context context, final String json) {
        PlayerHistory.fromJson(json).commit(context);
        Gcm.register(context);
        finish();
    }
}