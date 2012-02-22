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

public final class QuestionAlarm extends BroadcastReceiver {
	
	private static final String LAST_REMIND = "question_last_remind";
	private static final String RACE_ID = "question_race_id";
	private static boolean alarm_set = false;

	public static void set(Context context) {
		// Verify user wants Question reminders
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.getBoolean(EditPreferences.KEY_REMIND_QUESTIONS, true))
			return;
		
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
		
			AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(context, QuestionAlarm.class);
			intent.putExtra(RACE_ID, race.getId());
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			am.set(AlarmManager.RTC_WAKEUP, race.getQuestionTimestamp(), pendingIntent);
			
			alarm_set = true;
		} else {
			Util.log("Not setting question alarm: alarm_set=" + alarm_set);
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		alarm_set = false;
		
		// Verify user didn't turn off question reminders after alarm was set
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean(EditPreferences.KEY_REMIND_QUESTIONS, true)) {
			Race race = new Race(context, intent.getIntExtra(RACE_ID, 0));
			Util.log("Received question alarm for race " + race.getId());
			
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager nm = (NotificationManager) context.getSystemService(ns);
			
			// TODO: Replace with custom icon
			int icon = android.R.drawable.stat_sys_warning;
			CharSequence tickerText = context.getString(R.string.remind_questions_ticker, race.getName());
			long when = System.currentTimeMillis();
			Notification notification = new Notification(icon, tickerText, when);
			
			CharSequence contentTitle = context.getString(R.string.app_name);
			CharSequence contentText = race.getName();
			Intent notificationIntent = new Intent(context, MainActivity.class);
			notificationIntent.putExtra(MainActivity.INTENT_TAB, 1);
			PendingIntent pi = PendingIntent.getActivity(context, 0, notificationIntent,
					PendingIntent.FLAG_CANCEL_CURRENT);

			notification.sound = Uri.parse(prefs.getString(EditPreferences.KEY_REMIND_RINGTONE,
					"content://settings/system/notification_sound"));
			if (prefs.getBoolean(EditPreferences.KEY_REMIND_VIBRATE, true))
				notification.defaults |= Notification.DEFAULT_VIBRATE;
			if (prefs.getBoolean(EditPreferences.KEY_REMIND_LED, true))
				notification.defaults |= Notification.DEFAULT_LIGHTS;
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(context, contentTitle, contentText, pi);
			
			nm.notify(R.string.remind_questions_ticker, notification);
			
			// Remember that user was reminded of this race
			Util.getState(context).edit().putInt(LAST_REMIND, race.getId()).commit();
			
			// Reset alarm for the next race
			set(context);
		} else {
			Util.log("Ignoring question alarm since option now disabled");
		}
	}

}
