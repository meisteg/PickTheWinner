/*
 * Copyright (C) 2012-2015 Gregory S. Meiste  <http://gregmeiste.com>
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.tagmanager.Container;
import com.meiste.greg.ptw.GtmHelper.OnContainerAvailableListener;
import com.meiste.greg.ptw.tab.Questions;

public final class RaceAlarm extends IntentService implements OnContainerAvailableListener {

    private static final String RACE_ID = "race_id";

    private static final int PI_REQ_CODE = 693033;
    private static final int PI_ACTION_REQ_CODE = 693034;

    private static boolean alarm_set = false;

    private final Object mSync = new Object();
    private Container mContainer;

    public RaceAlarm() {
        super(RaceAlarm.class.getSimpleName());
        setIntentRedelivery(true);
    }

    @SuppressLint("NewApi")
    public static void set(final Context context) {
        final Race race = Race.getNext(context, true, true);

        if (!alarm_set && (race != null)) {
            Util.log("Setting race alarm for race " + race.getId());

            final AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            final Intent intent = new Intent(context, RaceAlarm.class);
            intent.putExtra(RACE_ID, race.getId());
            final PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, race.getStartTimestamp(), pendingIntent);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, race.getStartTimestamp(), pendingIntent);
            }

            alarm_set = true;
        } else {
            Util.log("Not setting race alarm: alarm_set=" + alarm_set);
        }
    }

    public static void reset(final Context context) {
        alarm_set = false;
        set(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        GtmHelper.getInstance(getApplicationContext()).getContainer(this);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        alarm_set = false;
        final Race race = Race.getInstance(this, intent.getIntExtra(RACE_ID, 0));
        Util.log("Received race alarm for race " + race.getId());

        synchronized (mSync) {
            if (mContainer == null) {
                try {
                    mSync.wait();
                } catch (final InterruptedException e) {
                    // Continue anyway
                }
            }
        }

        // Only show notification if user wants race reminders
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_RACE, true) &&
                mContainer.getBoolean(GtmHelper.KEY_GAME_ENABLED)) {
            final Intent notificationIntent = new Intent(this, RaceActivity.class);
            notificationIntent.putExtra(RaceActivity.INTENT_ID, race.getId());
            notificationIntent.putExtra(RaceActivity.INTENT_ALARM, true);
            final PendingIntent pi = PendingIntent.getActivity(this, PI_REQ_CODE,
                    notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            int defaults = 0;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_VIBRATE, true))
                defaults |= Notification.DEFAULT_VIBRATE;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_LED, true))
                defaults |= Notification.DEFAULT_LIGHTS;

            final NotificationCompat.Builder b = new NotificationCompat.Builder(this);
            b.setSmallIcon(R.drawable.ic_stat_steering_wheel);
            b.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
            b.setTicker(getString(R.string.remind_race_ticker, race.getName()));
            b.setContentTitle(getString(R.string.remind_race_notify));
            b.setContentText(race.getName());
            b.setStyle(new NotificationCompat.BigTextStyle().bigText(race.getName()));
            b.setContentIntent(pi);
            b.setAutoCancel(true);
            b.setDefaults(defaults);
            b.setSound(Uri.parse(prefs.getString(EditPreferences.KEY_NOTIFY_RINGTONE,
                    PTW.DEFAULT_NOTIFY_SND)));
            b.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            final SharedPreferences cache = getSharedPreferences(Questions.ACACHE, Activity.MODE_PRIVATE);
            if (cache.contains(Questions.cachePrefix() + race.getId())) {
                final Intent actionIntent = new Intent(this, MainActivity.class);
                actionIntent.putExtra(PTW.INTENT_EXTRA_TAB, 1);
                final PendingIntent pi_action = PendingIntent.getActivity(this, PI_ACTION_REQ_CODE,
                        actionIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                b.addAction(R.drawable.ic_stat_steering_wheel,
                        getString(R.string.remind_race_action), pi_action);
            }

            getNM(this).notify(R.string.remind_race_ticker, b.build());
        } else {
            Util.log("Ignoring race alarm since option is disabled");
        }

        // Reset alarm for the next race
        set(this);
        sendBroadcast(new Intent(PTW.INTENT_ACTION_RACE_ALARM));
    }

    @Override
    public void onContainerAvailable(final Context context, final Container container) {
        synchronized (mSync) {
            mContainer = container;
            mSync.notify();
        }
    }

    public static void clearNotification(final Context context) {
        getNM(context).cancel(R.string.remind_race_ticker);
    }

    private static NotificationManager getNM(final Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
