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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.meiste.greg.ptw.GAE.GaeListener;

public class GCMIntentService extends GCMBaseIntentService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    private final Semaphore sem = new Semaphore(0);
    private boolean mGaeSuccess = false;

    public GCMIntentService() {
        super(PTW.GCM_SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String regId) {
        Util.log("Device registered with GCM: regId = " + regId);

        String serverUrl = GAE.PROD_URL + "/register";
        Map<String, String> params = new HashMap<String, String>();
        params.put("regId", regId);
        long backoff = BACKOFF_MILLI_SECONDS;

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            Util.log("Attempt #" + i + " to register device on PTW server");
            try {
                post(serverUrl, params);
                GCMRegistrar.setRegisteredOnServer(context, true);
                Util.log("Device successfully registered on PTW server");
                return;
            } catch (IOException e) {
                Util.log("Failed to register on attempt " + i + " with " + e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Util.log("Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    Util.log("Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    break;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }

        Util.log("PTW server register failed. Unregister from GCM");
        GCMRegistrar.unregister(context);
    }

    @Override
    protected void onUnregistered(Context context, String regId) {
        Util.log("Device unregistered from GCM");

        if (!GCMRegistrar.isRegisteredOnServer(context))
            return;

        String serverUrl = GAE.PROD_URL + "/unregister";
        Map<String, String> params = new HashMap<String, String>();
        params.put("regId", regId);
        try {
            post(serverUrl, params);
            GCMRegistrar.setRegisteredOnServer(context, false);
            Util.log("Device unregistered from PTW server");
        } catch (IOException e) {
            // At this point the device is unregistered from GCM, but still
            // registered in the server. It is not necessary to try to
            // unregister again: if the server tries to send a message to the
            // device, it will get a "NotRegistered" error message and should
            // unregister the device.
            Util.log("Failed to unregister device from PTW server");
        }
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Util.log("Received message from GCM");
        long backoff = BACKOFF_MILLI_SECONDS;

        /* First update schedule */
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            Util.log("Attempt #" + i + " to sync schedule from PTW server");
            GAE.getInstance(getApplicationContext()).getPage(scheduleListener, "schedule");
            try {
                sem.acquire();
            } catch (InterruptedException e) {}

            if (mGaeSuccess || (i == MAX_ATTEMPTS))
                break;
            try {
                Util.log("Sleeping for " + backoff + " ms before retry");
                Thread.sleep(backoff);
            } catch (InterruptedException e) {}

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
            } catch (InterruptedException e) {}

            if (mGaeSuccess || (i == MAX_ATTEMPTS))
                break;
            try {
                Util.log("Sleeping for " + backoff + " ms before retry");
                Thread.sleep(backoff);
            } catch (InterruptedException e) {}

            // increase backoff exponentially
            backoff *= 2;
        }
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
        Util.log("Received deleted messages notification from GCM");
    }

    @Override
    public void onError(Context context, String errorId) {
        Util.log("Received error from GCM: " + errorId);
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        Util.log("Received recoverable error from GCM: " + errorId);
        return super.onRecoverableError(context, errorId);
    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params request parameters.
     *
     * @throws IOException propagated from POST.
     */
    private static void post(String endpoint, Map<String, String> params)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
            .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        Util.log("Posting '" + body + "' to " + url);
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private GcmGaeListener scheduleListener = new GcmGaeListener() {
        @Override
        public void onGet(Context context, String json) {
            Util.log("scheduleListener: onGet");

            Races.update(context, json);
            RaceAlarm.reset(context);
            BusProvider.getInstance().post(new ScheduleUpdateEvent());

            super.onGet(context, json);
        }
    };

    private GcmGaeListener standingsListener = new GcmGaeListener() {
        @Override
        public void onGet(Context context, String json) {
            Util.log("standingsListener: onGet");

            final PlayerAdapter pAdapter = new PlayerAdapter(getApplicationContext());
            int beforeUpdate = pAdapter.getRaceAfterNum();

            Standings.update(context, json);
            BusProvider.getInstance().post(new StandingsUpdateEvent());

            pAdapter.notifyDataSetChanged();
            int afterUpdate = pAdapter.getRaceAfterNum();

            if ((beforeUpdate != afterUpdate) && (afterUpdate > 0)) {
                Util.log("Notifying user of standings update");
                showResultsNotification(context, pAdapter.getRaceAfterName());
            }

            super.onGet(context, json);
        }
    };

    private class GcmGaeListener implements GaeListener {
        @Override
        public void onGet(Context context, String json) {
            mGaeSuccess = true;
            sem.release();
        }

        @Override
        public void onFailedConnect(Context context) {
            Util.log("GcmGaeListener: onFailedConnect");
            mGaeSuccess = false;
            sem.release();
        }

        @Override
        public void onLaunchIntent(Intent launch) {
            // Should never happen, but release semaphore to prevent stuck wakelock
            mGaeSuccess = false;
            sem.release();
        }

        @Override
        public void onConnectSuccess(Context context, String json) {
            // Should never happen, but release semaphore to prevent stuck wakelock
            mGaeSuccess = false;
            sem.release();
        }
    }

    private void showResultsNotification(Context context, String race) {
        // Only show notification if user wants results notifications
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_RESULTS, true)) {
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.putExtra(MainActivity.INTENT_TAB, 2);
            PendingIntent pi = PendingIntent.getActivity(context, 0, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            int defaults = 0;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_VIBRATE, true))
                defaults |= Notification.DEFAULT_VIBRATE;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_LED, true))
                defaults |= Notification.DEFAULT_LIGHTS;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_stat_steering_wheel)
            .setTicker(context.getString(R.string.remind_results_notify, race))
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.remind_results_notify, race))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setDefaults(defaults)
            .setSound(Uri.parse(prefs.getString(EditPreferences.KEY_NOTIFY_RINGTONE,
                    "content://settings/system/notification_sound")));

            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager nm = (NotificationManager) context.getSystemService(ns);
            nm.notify(R.string.remind_results_notify, builder.getNotification());
        }
    }
}
