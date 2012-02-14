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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class RaceAlarm extends BroadcastReceiver {
	
	private static final String RACE_ID = "race_id";
	private static boolean alarm_set = false;

	public static void set(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Race race = Race.getNext(context, true, true);
		
		if (!alarm_set && prefs.getBoolean("remind.race", true) && (race != null)) {
			Util.log("Setting race alarm for race " + race.getId());
		
			AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(context, RaceAlarm.class);
			intent.putExtra(RACE_ID, race.getId());
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			am.set(AlarmManager.RTC_WAKEUP, race.getStartTimestamp(), pendingIntent);
			
			alarm_set = true;
		} else {
			Util.log("Not setting race alarm: alarm_set=" + alarm_set);
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		alarm_set = false;
		
		// Verify user didn't turn off race reminders after alarm was set
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean("remind.race", true)) {
			Util.log("Received race alarm for race " + intent.getIntExtra(RACE_ID, 0));
			
			// TODO: Send notification to user
			
			// Reset alarm for the next race
			set(context);
		} else {
			Util.log("Ignoring race alarm since option now disabled");
		}
	}

}
