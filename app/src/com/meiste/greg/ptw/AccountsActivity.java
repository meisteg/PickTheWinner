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

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.meiste.greg.ptw.GAE.GaeListener;

public class AccountsActivity extends SherlockActivity implements GaeListener {

    private static final int REQUEST_LAUNCH_INTENT = 0;
    private int mAccountSelectedPosition = 0;
    private String mAccountName;
    private GAE mGae;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mGae = new GAE(this, this);
        List<String> accounts = mGae.getGoogleAccounts();
        if (accounts.size() == 0) {
            // Show a dialog and invoke the "Add Account" activity if requested
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.needs_account);
            builder.setPositiveButton(R.string.add_account, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
                    finish();
                }
            });
            builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.setIcon(android.R.drawable.stat_sys_warning);
            builder.setTitle(R.string.attention);
            builder.show();
        } else {
            final ListView listView = (ListView) findViewById(R.id.select_account);
            listView.setAdapter(new ArrayAdapter<String>(this, R.layout.account, accounts));
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setItemChecked(mAccountSelectedPosition, true);

            final Button connectButton = (Button) findViewById(R.id.connect);
            connectButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mAccountSelectedPosition = listView.getCheckedItemPosition();
                    TextView account = (TextView) listView.getChildAt(mAccountSelectedPosition);
                    mAccountName = (String) account.getText();
                    setContentView(R.layout.connecting);
                    mGae.connect(mAccountName);
                }
            });

            final Button exitButton = (Button) findViewById(R.id.exit);
            exitButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LAUNCH_INTENT) {
            if (resultCode == RESULT_OK)
                mGae.connect(mAccountName);
            else
                onFailedConnect();
        }
    }

    @Override
    public void onFailedConnect() {
        Toast.makeText(this, R.string.failed_connect, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onLaunchIntent(Intent launch) {
        startActivityForResult(launch, REQUEST_LAUNCH_INTENT);
    }

    @Override
    public void onConnectSuccess(Context context) {
        finish();
    }

    @Override
    public void onGet(Context context, String json) {}
}