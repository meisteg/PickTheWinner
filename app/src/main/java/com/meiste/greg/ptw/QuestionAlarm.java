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

public final class QuestionAlarm extends IntentService implements OnContainerAvailableListener {

    private static final String LAST_REMIND = "question_last_remind";
    private static final String RACE_ID = "question_race_id";
    private static final int PI_REQ_CODE = 146066;
    private static boolean alarm_set = false;

    private final Object mSync = new Object();
    private Container mContainer;

    public QuestionAlarm() {
        super(QuestionAlarm.class.getSimpleName());
        setIntentRedelivery(true);
    }

    public static void set(final Context context) {
        // Get next points race: allow in progress
        Race race = Race.getNext(context, false, true);
        if (race == null)
            return;

        // Check if user was already reminded of in progress race
        if (Util.getState(context).getInt(LAST_REMIND, -1) >= race.getId()) {
            // Get next points race: do not allow in progress
            race = Race.getNext(context, false, false);
            if (race == null)
                return;
        }

        if (!alarm_set) {
            Util.log("Setting question alarm for race " + race.getId());

            final AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            final Intent intent = new Intent(context, QuestionAlarm.class);
            intent.putExtra(RACE_ID, race.getId());
            final PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, race.getQuestionTimestamp(), pendingIntent);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, race.getQuestionTimestamp(), pendingIntent);
            }

            alarm_set = true;
        } else {
            Util.log("Not setting question alarm: alarm_set=true");
        }
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
        Util.log("Received question alarm for race " + race.getId());

        synchronized (mSync) {
            if (mContainer == null) {
                try {
                    mSync.wait();
                } catch (final InterruptedException e) {
                    // Continue anyway
                }
            }
        }

        // Only show notification if user wants question reminders
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_QUESTIONS, true) &&
                mContainer.getBoolean(GtmHelper.KEY_GAME_ENABLED)) {
            final Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.putExtra(PTW.INTENT_EXTRA_TAB, 1);
            final PendingIntent pi = PendingIntent.getActivity(this, PI_REQ_CODE,
                    notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            int defaults = 0;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_VIBRATE, true))
                defaults |= Notification.DEFAULT_VIBRATE;
            if (prefs.getBoolean(EditPreferences.KEY_NOTIFY_LED, true))
                defaults |= Notification.DEFAULT_LIGHTS;

            final NotificationCompat.Builder b = new NotificationCompat.Builder(this);
            b.setSmallIcon(R.drawable.ic_stat_steering_wheel);
            b.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
            b.setTicker(getString(R.string.remind_questions_ticker, race.getName()));
            b.setContentTitle(getString(R.string.app_name));
            b.setContentText(race.getName());
            b.setStyle(new NotificationCompat.BigTextStyle().bigText(race.getName()));
            b.setContentIntent(pi);
            b.setAutoCancel(true);
            b.setDefaults(defaults);
            b.setSound(Uri.parse(prefs.getString(EditPreferences.KEY_NOTIFY_RINGTONE,
                    PTW.DEFAULT_NOTIFY_SND)));

            getNM(this).notify(R.string.remind_questions_ticker, b.build());
        } else {
            Util.log("Ignoring question alarm since option is disabled");
        }

        // Remember that user was reminded of this race
        Util.getState(this).edit().putInt(LAST_REMIND, race.getId()).apply();

        // Reset alarm for the next race
        set(this);
        sendBroadcast(new Intent(PTW.INTENT_ACTION_RACE_ALARM));
    }

    @Override
    public void onContainerAvailable(final Container container) {
        synchronized (mSync) {
            mContainer = container;
            mSync.notify();
        }
    }

    public static void clearNotification(final Context context) {
        getNM(context).cancel(R.string.remind_questions_ticker);
    }

    private static NotificationManager getNM(final Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
