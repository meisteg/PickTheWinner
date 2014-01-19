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

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.google.analytics.tracking.android.EasyTracker;
import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.tab.Standings;

public class NfcRecvActivity extends SherlockActivity implements GaeListener {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connecting);

        final TextView tv = (TextView) findViewById(R.id.connecting_text);
        tv.setText(R.string.adding_friend);
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        } else {
            finish();
        }
    }

    @Override
    public void onNewIntent(final Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    private void processIntent(final Intent intent) {
        final Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        final NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        final String json = new String(msg.getRecords()[0].getPayload());
        Util.log("NFC received: " + json);

        if (GAE.isAccountSetupNeeded(getApplicationContext())) {
            Toast.makeText(this, R.string.friend_fail_no_account, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final FriendRequest fReq = FriendRequest.fromJson(json);
        fReq.player.friend = true;
        GAE.getInstance(this).postPage(this, "friend", fReq.toJson());
    }

    private void showStandings() {
        final Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(PTW.INTENT_EXTRA_TAB, 2);
        startActivity(intent);
    }

    @Override
    public void onFailedConnect(final Context context) {
        // Technically should check if friending failed or if standings update
        // failed. An error is an error, so just report friending failed.
        Toast.makeText(this, R.string.friend_fail_generic, Toast.LENGTH_LONG).show();
        showStandings();
        finish();
    }

    @Override
    public void onConnectSuccess(final Context context, final String json) {
        GAE.getInstance(context).getPage(this, "standings");
    }

    @Override
    public void onGet(final Context context, final String json) {
        Standings.update(context, json);
        sendBroadcast(new Intent(PTW.INTENT_ACTION_STANDINGS));
        Toast.makeText(this, R.string.friend_success, Toast.LENGTH_LONG).show();
        showStandings();
        finish();
    }

    @Override
    public void onLaunchIntent(final Intent launch) {}
}