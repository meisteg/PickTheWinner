/*
 * Copyright (C) 2012-2013 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.greg.ptw.gcm;

import java.util.concurrent.Semaphore;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.meiste.greg.ptw.EditPreferences;
import com.meiste.greg.ptw.GAE;
import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.MainActivity;
import com.meiste.greg.ptw.PTW;
import com.meiste.greg.ptw.PlayerAdapter;
import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.RaceAlarm;
import com.meiste.greg.ptw.Races;
import com.meiste.greg.ptw.Standings;
import com.meiste.greg.ptw.Util;

public class GcmIntentService extends IntentService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int PI_REQ_CODE = 426801;
    private static final int BACKOFF_MILLI_SECONDS = 2000;

    private static final String MSG_KEY = "collapse_key";
    private static final String MSG_KEY_SYNC = "ptw_sync";

    private final Semaphore sem = new Semaphore(0);
    private boolean mGaeSuccess = false;

    public GcmIntentService() {
        super(GcmIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final Bundle extras = intent.getExtras();
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        final String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types
             * not recognized.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                Util.log("GCM send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                Util.log("GCM deleted messages on server: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                if (intent.hasExtra(MSG_KEY)) {
                    final String message = intent.getStringExtra(MSG_KEY);
                    Util.log("Received " + message + " message from GCM");

                    if (MSG_KEY_SYNC.equals(message)) {
                        handleMsgSync();
                    } else {
                        Util.log("Message type unknown. Ignoring...");
                    }
                }
            }
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void handleMsgSync() {
        long backoff = BACKOFF_MILLI_SECONDS;

        /* First update schedule */
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            Util.log("Attempt #" + i + " to sync schedule from PTW server");
            GAE.getInstance(getApplicationContext()).getPage(scheduleListener, "schedule");
            try {
                sem.acquire();
            } catch (final InterruptedException e) {}

            if (mGaeSuccess || (i == MAX_ATTEMPTS))
                break;
            try {
                Util.log("Sleeping for " + backoff + " ms before retry");
                Thread.sleep(backoff);
            } catch (final InterruptedException e) {}

            // increase backoff exponentially
            backoff *= 2;
        }

        /* Reset variables */
        backoff = BACKOFF_MILLI_SECONDS;
        mGaeSuccess = false;

        /* Next update standings */
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            if (GAE.isAccountSetupNeeded(this)) {
                Util.log("Skipping Standings sync since account not setup");
                break;
            }

            Util.log("Attempt #" + i + " to sync standings from PTW server");
            GAE.getInstance(getApplicationContext()).getPage(standingsListener, "standings");
            try {
                sem.acquire();
            } catch (final InterruptedException e) {}

            if (mGaeSuccess || (i == MAX_ATTEMPTS))
                break;
            try {
                Util.log("Sleeping for " + backoff + " ms before retry");
                Thread.sleep(backoff);
            } catch (final InterruptedException e) {}

            // increase backoff exponentially
            backoff *= 2;
        }
    }

    private final GcmGaeListener scheduleListener = new GcmGaeListener() {
        @Override
        public void onGet(final Context context, final String json) {
            Util.log("scheduleListener: onGet");

            Races.update(context, json);
            RaceAlarm.reset(context);
            sendBroadcast(new Intent(PTW.INTENT_ACTION_SCHEDULE));

            super.onGet(context, json);
        }
    };

    private final GcmGaeListener standingsListener = new GcmGaeListener() {
        @Override
        public void onGet(final Context context, final String json) {
            Util.log("standingsListener: onGet");

            final PlayerAdapter pAdapter = new PlayerAdapter(getApplicationContext());
            final int beforeUpdate = pAdapter.getRaceAfterNum();

            Standings.update(context, json);
            sendBroadcast(new Intent(PTW.INTENT_ACTION_STANDINGS));

            pAdapter.notifyDataSetChanged();
            final int afterUpdate = pAdapter.getRaceAfterNum();

            if ((beforeUpdate != afterUpdate) && (afterUpdate > 0)) {
                Util.log("Notifying user of standings update");
                showResultsNotification(context, pAdapter.getRaceAfterName());
            }

            super.onGet(context, json);
        }
    };

    private class GcmGaeListener implements GaeListener {
        @Override
        public void onGet(final Context context, final String json) {
            mGaeSuccess = true;
            sem.release();
        }

        @Override
        public void onFailedConnect(final Context context) {
            Util.log("GcmGaeListener: onFailedConnect");
            mGaeSuccess = false;
            sem.release();
        }

        @Override
        public void onLaunchIntent(final Intent launch) {
            // Should never happen, but release semaphore to prevent stuck wakelock
            mGaeSuccess = false;
            sem.release();
        }

        @Override
        public void onConnectSuccess(final Context context, final String json) {
            // Should never happen, but release semaphore to prevent stuck wakelock
            mGaeSuccess = false;
            sem.release();
        }
    }

    private static void showResultsNotification(final Context context, final String race) {
        // Only show notification if user wants results notifications
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_RESULTS, true)) {
            final Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.putExtra(MainActivity.INTENT_TAB, 2);
            final PendingIntent pi = PendingIntent.getActivity(context, PI_REQ_CODE,
                    notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            int defaults = 0;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_VIBRATE, true))
                defaults |= Notification.DEFAULT_VIBRATE;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_LED, true))
                defaults |= Notification.DEFAULT_LIGHTS;

            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_stat_steering_wheel)
            .setTicker(context.getString(R.string.remind_results_notify, race))
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.remind_results_notify, race))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setDefaults(defaults)
            .setSound(Uri.parse(prefs.getString(EditPreferences.KEY_NOTIFY_RINGTONE,
                    "content://settings/system/notification_sound")));

            getNM(context).notify(R.string.remind_results_notify, builder.build());
        }
    }

    public static void clearNotification(final Context context) {
        getNM(context).cancel(R.string.remind_results_notify);
    }

    private static NotificationManager getNM(final Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
