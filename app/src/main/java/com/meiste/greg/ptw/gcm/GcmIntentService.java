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
package com.meiste.greg.ptw.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.tagmanager.Container;
import com.meiste.greg.ptw.EditPreferences;
import com.meiste.greg.ptw.GAE;
import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.GtmHelper;
import com.meiste.greg.ptw.GtmHelper.OnContainerAvailableListener;
import com.meiste.greg.ptw.MainActivity;
import com.meiste.greg.ptw.PTW;
import com.meiste.greg.ptw.PlayerAdapter;
import com.meiste.greg.ptw.PlayerHistory;
import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.Races;
import com.meiste.greg.ptw.Util;
import com.meiste.greg.ptw.sync.AccountUtils;
import com.meiste.greg.ptw.tab.RuleBook;
import com.meiste.greg.ptw.tab.Standings;

public class GcmIntentService extends IntentService implements OnContainerAvailableListener {

    private static final int MAX_ATTEMPTS = 5;
    private static final int PI_REQ_CODE = 426801;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    private static final long WAIT_TIMEOUT = DateUtils.MINUTE_IN_MILLIS;

    private static final String MSG_KEY = "collapse_key";
    private static final String MSG_KEY_SYNC = "ptw_sync";
    private static final String MSG_KEY_HISTORY = "ptw_history";
    private static final String MSG_KEY_RULES = "ptw_rules";

    private final Object mSync = new Object();
    private boolean mGaeSuccess = false;
    private Container mContainer;

    public GcmIntentService() {
        super(GcmIntentService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        GtmHelper.getInstance(getApplicationContext()).getContainer(this);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final Bundle extras = intent.getExtras();
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        final String messageType = gcm.getMessageType(intent);

        synchronized (mSync) {
            if (mContainer == null) {
                try {
                    mSync.wait();
                } catch (final InterruptedException e) {
                    // Continue anyway
                }
            }
        }

        if ((extras != null) && !extras.isEmpty()) {
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types
             * not recognized.
             */
            switch (messageType) {
                case GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR:
                    Util.log("GCM send error: " + extras.toString());
                    break;
                case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
                    Util.log("GCM deleted messages on server: " + extras.toString());
                    break;
                case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE:
                    if (intent.hasExtra(MSG_KEY)) {
                        final String message = intent.getStringExtra(MSG_KEY);
                        Util.log("Received " + message + " message from GCM");

                        switch (message) {
                            case MSG_KEY_SYNC:
                                getFromServer("schedule", scheduleListener, false);
                                getFromServer("standings", standingsListener, true);
                                break;
                            case MSG_KEY_HISTORY:
                                getFromServer("history", historyListener, true);
                                break;
                            case MSG_KEY_RULES:
                                getFromServer("rule_book", rulesListener, false);
                                break;
                            default:
                                Util.log("Message type unknown. Ignoring...");
                                break;
                        }
                    }
                    break;
            }
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    @Override
    public void onContainerAvailable(final Context context, final Container container) {
        synchronized (mSync) {
            mContainer = container;
            mSync.notify();
        }
    }

    private void getFromServer(final String page, final GcmGaeListener l, final boolean accountRequired) {
        long backoff = BACKOFF_MILLI_SECONDS;
        mGaeSuccess = false;

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            if (accountRequired && AccountUtils.isAccountSetupNeeded(this)) {
                Util.log("Skipping " + page + " sync since account not setup");
                break;
            }

            Util.log("Attempt #" + i + " to get " + page + " from PTW server");
            synchronized (mSync) {
                GAE.getInstance(getApplicationContext()).getPage(l, page);
                try {
                    mSync.wait(WAIT_TIMEOUT);
                } catch (final InterruptedException e) {
                    // Continue anyway
                }
            }

            if (mGaeSuccess || (i == MAX_ATTEMPTS))
                break;
            try {
                Util.log("Sleeping for " + backoff + " ms before retry");
                Thread.sleep(backoff);
            } catch (final InterruptedException e) {
                // Continue anyway
            }

            // increase backoff exponentially
            backoff *= 2;
        }
    }

    private final GcmGaeListener scheduleListener = new GcmGaeListener() {
        @Override
        public void onGet(final Context context, final String json) {
            Util.log("scheduleListener: onGet");

            Races.update(context, json);
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

    private final GcmGaeListener historyListener = new GcmGaeListener() {
        @Override
        public void onGet(final Context context, final String json) {
            Util.log("historyListener: onGet");

            PlayerHistory.fromJson(json).commit(context);
            sendBroadcast(new Intent(PTW.INTENT_ACTION_HISTORY));

            super.onGet(context, json);
        }
    };

    private final GcmGaeListener rulesListener = new GcmGaeListener() {
        @Override
        public void onGet(final Context context, final String json) {
            Util.log("rulesListener: onGet");

            RuleBook.update(context, json);
            super.onGet(context, json);
        }
    };

    private class GcmGaeListener implements GaeListener {
        @Override
        public void onGet(final Context context, final String json) {
            synchronized (mSync) {
                mGaeSuccess = true;
                mSync.notify();
            }
        }

        @Override
        public void onFailedConnect(final Context context) {
            Util.log("GcmGaeListener: onFailedConnect");
            synchronized (mSync) {
                mGaeSuccess = false;
                mSync.notify();
            }
        }

        @Override
        public void onLaunchIntent(final Intent launch) {
            // Should never happen
            synchronized (mSync) {
                mGaeSuccess = false;
                mSync.notify();
            }
        }

        @Override
        public void onConnectSuccess(final Context context, final String json) {
            // Should never happen
            synchronized (mSync) {
                mGaeSuccess = false;
                mSync.notify();
            }
        }
    }

    private void showResultsNotification(final Context context, final String race) {
        // Only show notification if user wants results notifications
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_RESULTS, true) &&
                mContainer.getBoolean(GtmHelper.KEY_GAME_ENABLED)) {
            final Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.putExtra(PTW.INTENT_EXTRA_TAB, 2);
            final PendingIntent pi = PendingIntent.getActivity(context, PI_REQ_CODE,
                    notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            int defaults = 0;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_VIBRATE, true))
                defaults |= Notification.DEFAULT_VIBRATE;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_LED, true))
                defaults |= Notification.DEFAULT_LIGHTS;

            final String contextText = context.getString(R.string.remind_results_notify, race);
            final NotificationCompat.Builder b = new NotificationCompat.Builder(context);
            b.setSmallIcon(R.drawable.ic_stat_steering_wheel);
            b.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
            b.setTicker(context.getString(R.string.remind_results_notify, race));
            b.setContentTitle(context.getString(R.string.app_name));
            b.setContentText(contextText);
            b.setStyle(new NotificationCompat.BigTextStyle().bigText(contextText));
            b.setContentIntent(pi);
            b.setAutoCancel(true);
            b.setDefaults(defaults);
            b.setSound(Uri.parse(prefs.getString(EditPreferences.KEY_NOTIFY_RINGTONE,
                    PTW.DEFAULT_NOTIFY_SND)));

            getNM(context).notify(R.string.remind_results_notify, b.build());
        }
    }

    public static void clearNotification(final Context context) {
        getNM(context).cancel(R.string.remind_results_notify);
    }

    private static NotificationManager getNM(final Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
