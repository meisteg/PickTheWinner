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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import com.google.android.gms.tagmanager.Container;
import com.meiste.greg.ptw.GtmHelper.OnContainerAvailableListener;
import com.meiste.greg.ptw.tab.Questions;
import com.squareup.picasso.Picasso;

public class WidgetProvider extends AppWidgetProvider implements OnContainerAvailableListener {

    private static final long UPDATE_INTERVAL = DateUtils.MINUTE_IN_MILLIS;
    private static final long UPDATE_FUDGE = 100; /* milliseconds */
    private static final long UPDATE_WARNING = (2 * DateUtils.HOUR_IN_MILLIS);

    private static final int PI_REQ_CODE = 810647;

    private static final String WIDGET_STATE = "widget.enabled";

    private static Race sRace;

    @Override
    public void onReceive (@NonNull final Context context, @NonNull final Intent intent) {
        final AppWidgetManager appWM = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = getInstalledWidgets(context, appWM);

        if (intent.hasExtra(Intent.EXTRA_ALARM_COUNT)) {
            onUpdate(context, appWM, appWidgetIds);
        } else if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)) {
            Util.log("WidgetProvider.onReceive: Time change");
            setAlarm(context);
        } else if (intent.getAction().equals(PTW.INTENT_ACTION_SCHEDULE)) {
            Util.log("WidgetProvider.onReceive: Schedule Updated");
            if (appWidgetIds.length > 0) {
                /* Force full widget update */
                sRace = null;
                onUpdate(context, appWM, appWidgetIds);
            }
        } else if (intent.getAction().equals(PTW.INTENT_ACTION_ANSWERS)) {
            Util.log("WidgetProvider.onReceive: Answers submitted");
            if (appWidgetIds.length > 0) {
                onUpdate(context, appWM, appWidgetIds);
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onEnabled(final Context context) {
        final boolean prevEnabled = Util.getState(context).getBoolean(WIDGET_STATE, false);
        Util.log("WidgetProvider.onEnabled: prevEnabled=" + prevEnabled);

        /* onEnabled gets called on device power up, so prevent extra enables
         * from being tracked. */
        if (!prevEnabled) {
            Util.getState(context).edit().putBoolean(WIDGET_STATE, true).apply();
            Analytics.trackEvent(context, "Widget", "state", "enabled");
        }
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWM, final int[] appWidgetIds) {
        Util.log("WidgetProvider.onUpdate: num=" + appWidgetIds.length);
        GtmHelper.getInstance(context).getContainer(this);

        /* Set alarm to update widget when device is awake. */
        setAlarm(context);
    }

    @Override
    public void onDisabled(final Context context) {
        Util.log("WidgetProvider.onDisabled");

        final AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getAlarmIntent(context));

        Util.getState(context).edit().putBoolean(WIDGET_STATE, false).apply();
        Analytics.trackEvent(context, "Widget", "state", "disabled");
    }

    @Override
    public void onContainerAvailable(final Context context, final Container container) {
        final AppWidgetManager appWM = AppWidgetManager.getInstance(context);
        if (container.getBoolean(GtmHelper.KEY_GAME_ENABLED)) {
            if ((sRace == null) || (!sRace.isFuture() && !sRace.isRecent())) {
                sRace = Race.getNext(context, true, true);
            }

            if (sRace != null) {
                setRaceView(context, appWM);
            } else {
                setEndOfSeasonView(context, appWM);
            }
        } else {
            setDisabledView(context, appWM);
        }
    }

    private void setRaceView(final Context context, final AppWidgetManager appWM) {
        final RemoteViews rViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        final int str_id = sRace.isRecent() ? R.string.widget_current_race : R.string.widget_next_race;
        final String nextRace = context.getString(str_id,
                sRace.getStartRelative(context, sRace.isRecent() ? 0 : UPDATE_FUDGE));

        rViews.setTextViewText(R.id.when, nextRace);

        if (sRace.isExhibition()) {
            rViews.setInt(R.id.status, "setText", R.string.widget_exhibition);
            rViews.setInt(R.id.widget_text_layout, "setBackgroundResource", R.drawable.widget_normal);
        } else if (sRace.isRecent()) {
            rViews.setInt(R.id.status, "setText", R.string.widget_no_results);
            rViews.setInt(R.id.widget_text_layout, "setBackgroundResource", R.drawable.widget_normal);
        } else if (!sRace.inProgress()) {
            rViews.setInt(R.id.status, "setText", R.string.widget_no_questions);
            rViews.setInt(R.id.widget_text_layout, "setBackgroundResource", R.drawable.widget_normal);
        } else {
            final SharedPreferences acache =
                    context.getSharedPreferences(Questions.ACACHE, Activity.MODE_PRIVATE);
            if (acache.contains(Questions.cachePrefix() + sRace.getId())) {
                rViews.setInt(R.id.status, "setText", R.string.widget_submitted);
                rViews.setInt(R.id.widget_text_layout, "setBackgroundResource", R.drawable.widget_good);
            } else if ((sRace.getStartTimestamp() - System.currentTimeMillis()) <= UPDATE_WARNING) {
                rViews.setInt(R.id.status, "setText", R.string.widget_no_answers);
                rViews.setInt(R.id.widget_text_layout, "setBackgroundResource", R.drawable.widget_warning);
            } else {
                rViews.setInt(R.id.status, "setText", R.string.widget_please_submit);
                rViews.setInt(R.id.widget_text_layout, "setBackgroundResource", R.drawable.widget_normal);
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(PTW.INTENT_EXTRA_TAB, 1);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, PI_REQ_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        rViews.setOnClickPendingIntent(R.id.widget_text_layout, pi);

        intent = new Intent(context, RaceActivity.class);
        intent.putExtra(RaceActivity.INTENT_ID, sRace.getId());
        intent.putExtra(RaceActivity.INTENT_ALARM, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pi = PendingIntent.getActivity(context, PI_REQ_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        rViews.setOnClickPendingIntent(R.id.race_logo, pi);

        Picasso.with(context).load(GAE.PROD_URL + "/img/race/" + sRace.getId() + ".png")
                .error(R.drawable.logo)
                .into(rViews, R.id.race_logo, getInstalledWidgets(context, appWM));
    }

    private void setEndOfSeasonView(final Context context, final AppWidgetManager appWM) {
        final RemoteViews rViews = new RemoteViews(context.getPackageName(), R.layout.widget_no_race);

        final Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent pi = PendingIntent.getActivity(context, PI_REQ_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        rViews.setOnClickPendingIntent(R.id.widget_full_layout, pi);
        appWM.updateAppWidget(getInstalledWidgets(context, appWM), rViews);
    }

    private void setDisabledView(final Context context, final AppWidgetManager appWM) {
        final RemoteViews rViews = new RemoteViews(context.getPackageName(), R.layout.widget_disabled);
        appWM.updateAppWidget(getInstalledWidgets(context, appWM), rViews);
    }

    private void setAlarm(final Context context) {
        /* No point setting alarm if no widgets */
        if (getInstalledWidgets(context).length == 0)
            return;

        final long now = System.currentTimeMillis();
        final long next = UPDATE_INTERVAL - (now % UPDATE_INTERVAL);

        final AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC, now + next, getAlarmIntent(context));
        } else {
            am.set(AlarmManager.RTC, now + next, getAlarmIntent(context));
        }
    }

    private static PendingIntent getAlarmIntent(final Context context) {
        final Intent intent = new Intent(context, WidgetProvider.class);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static int[] getInstalledWidgets(final Context context) {
        return getInstalledWidgets(context, AppWidgetManager.getInstance(context));
    }

    private static int[] getInstalledWidgets(final Context context, final AppWidgetManager appWM) {
        return appWM.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
    }
}
