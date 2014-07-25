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

import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.gcm.Gcm;

public class AccountsActivity extends BaseActivity implements GaeListener {

    private static final int REQUEST_LAUNCH_INTENT = 0;
    private static final String KEY_ACCOUNT_NAME = "account";

    // EXTRA_ACCOUNT_TYPES was added in API 18 (Android 4.3), but works
    // fine on many older versions and is simply ignored by the rest.
    @SuppressLint("InlinedApi")
    private final Intent mAccountIntent = new Intent(Settings.ACTION_ADD_ACCOUNT)
    .putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {
            GAE.ACCOUNT_TYPE
    });

    private String mAccountName;
    private GAE mGae;
    private boolean mShouldFinish;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        final Button exitButton = (Button) findViewById(R.id.close_button);
        exitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                finish();
            }
        });

        mGae = GAE.getInstance(getApplicationContext());
        final List<String> accounts = mGae.getGoogleAccounts();
        if (accounts.size() == 0) {
            // Show a dialog and invoke the "Add Account" activity if requested
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.needs_account);

            if (mAccountIntent.resolveActivity(getPackageManager()) != null) {
                builder.setPositiveButton(R.string.add_account, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        startActivity(mAccountIntent);
                        finish();
                    }
                });
            }

            builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    finish();
                }
            });

            builder.setIcon(android.R.drawable.stat_sys_warning);
            builder.setTitle(R.string.attention);
            builder.setCancelable(false);
            builder.show();
        } else {
            int selectPosition = 0;
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final String currAccount = prefs.getString(EditPreferences.KEY_ACCOUNT_EMAIL, null);
            if (!TextUtils.isEmpty(currAccount)) {
                for (int i = 0; i < accounts.size(); i++) {
                    if (accounts.get(i).equals(currAccount)) {
                        Util.log("Defaulting to current account: " + currAccount);
                        selectPosition = i;
                        break;
                    }
                }
            }

            final ListView listView = (ListView) findViewById(R.id.select_account);
            listView.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_single_choice, accounts));
            listView.setItemChecked(selectPosition, true);

            final Button connectButton = (Button) findViewById(R.id.connect_button);
            connectButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final int selectedPos = listView.getCheckedItemPosition();
                    final TextView account = (TextView) listView.getChildAt(selectedPos);
                    mAccountName = (String) account.getText();
                    setContentView(R.layout.connecting);
                    mGae.connect(AccountsActivity.this, mAccountName);
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mShouldFinish) {
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACCOUNT_NAME, mAccountName);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAccountName = savedInstanceState.getString(KEY_ACCOUNT_NAME);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (mAccountIntent.resolveActivity(getPackageManager()) != null) {
            menu.add(Menu.NONE, R.string.add_account, Menu.NONE, R.string.add_account)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case R.string.add_account:
            // To make the transition to the system activity look correct,
            // delay the finish() call of this activity.
            mShouldFinish = true;
            startActivity(mAccountIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_LAUNCH_INTENT) {
            Util.log("onActivityResult: resultCode=" + resultCode + ", mAccountName=" + mAccountName);

            if ((resultCode == RESULT_OK) && (mAccountName != null))
                mGae.connect(this, mAccountName);
            else
                onFailedConnect(this);
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