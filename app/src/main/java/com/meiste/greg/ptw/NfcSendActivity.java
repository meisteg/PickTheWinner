/*
 * Copyright (C) 2013-2014 Gregory S. Meiste  <http://gregmeiste.com>
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

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NfcSendActivity extends BaseActivity implements OnNdefPushCompleteCallback {

    public static final String EXTRA_PLAYER_REQ = "player_req";
    private static final int MESSAGE_SENT = 1;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check for available NFC Adapter
        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, R.string.enable_nfc, Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            } catch (final ActivityNotFoundException e) {
                // Ignore
            }
            finish();
            return;
        }

        if (!nfcAdapter.isNdefPushEnabled()) {
            Toast.makeText(this, R.string.enable_beam, Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
            } catch (final ActivityNotFoundException e) {
                // Ignore
            }
            finish();
            return;
        }

        if (!getIntent().hasExtra(EXTRA_PLAYER_REQ)) {
            finish();
            return;
        }

        final NdefMessage msg = new NdefMessage(NdefRecord.createMime(
                "application/com.meiste.greg.ptw",
                getIntent().getStringExtra(EXTRA_PLAYER_REQ).getBytes()));
        nfcAdapter.setNdefPushMessage(msg, this);
        nfcAdapter.setOnNdefPushCompleteCallback(this, this);

        setContentView(R.layout.nfc_send);
        setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNdefPushComplete(final NfcEvent event) {
        // A handler is needed to send messages to the activity when this
        // callback occurs, because it happens from a binder thread
        mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
    }

    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(final Message msg) {
            switch (msg.what) {
            case MESSAGE_SENT:
                Toast.makeText(getApplicationContext(), R.string.nfc_sent, Toast.LENGTH_LONG).show();
                finish();
                return true;
            }
            return false;
        }
    });
}