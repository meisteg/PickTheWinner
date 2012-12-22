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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public final class RaceAlarm extends BroadcastReceiver {

    private static final String RACE_ID = "race_id";
    private static boolean alarm_set = false;

    public static void set(final Context context) {
        final Race race = Race.getNext(context, true, true);

        if (!alarm_set && (race != null)) {
            Util.log("Setting race alarm for race " + race.getId());

            final AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            final Intent intent = new Intent(context, RaceAlarm.class);
            intent.putExtra(RACE_ID, race.getId());
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC_WAKEUP, race.getStartTimestamp(), pendingIntent);

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
    public void onReceive(final Context context, final Intent intent) {
        alarm_set = false;
        final Race race = Race.getInstance(context, intent.getIntExtra(RACE_ID, 0));
        Util.log("Received race alarm for race " + race.getId());

        // Only show notification if user wants race reminders
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_RACE, true)) {
            final Intent notificationIntent = new Intent(context, RaceActivity.class);
            notificationIntent.putExtra(RaceActivity.INTENT_ID, race.getId());
            notificationIntent.putExtra(RaceActivity.INTENT_ALARM, true);
            final PendingIntent pi = PendingIntent.getActivity(context, 0, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            int defaults = 0;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_VIBRATE, true))
                defaults |= Notification.DEFAULT_VIBRATE;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_LED, true))
                defaults |= Notification.DEFAULT_LIGHTS;

            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_stat_steering_wheel)
            .setTicker(context.getString(R.string.remind_race_ticker, race.getName()))
            .setContentTitle(context.getString(R.string.remind_race_notify))
            .setContentText(race.getName())
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setDefaults(defaults)
            .setSound(Uri.parse(prefs.getString(EditPreferences.KEY_NOTIFY_RINGTONE,
                    "content://settings/system/notification_sound")));

            final String ns = Context.NOTIFICATION_SERVICE;
            final NotificationManager nm = (NotificationManager) context.getSystemService(ns);
            nm.notify(R.string.remind_race_ticker, builder.getNotification());
        } else {
            Util.log("Ignoring race alarm since option is disabled");
        }

        // Reset alarm for the next race
        set(context);
        BusProvider.getInstance().post(new RaceAlarmEvent());
    }
}
