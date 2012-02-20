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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class EditPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_REMIND_QUESTIONS = "remind.questions";
	public static final String KEY_REMIND_RACE = "remind.race";
	public static final String KEY_REMIND_VIBRATE = "remind.vibrate";
	public static final String KEY_REMIND_LED = "remind.led";
	public static final String KEY_REMIND_RINGTONE = "remind.ringtone";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Util.log("EditPreferences.onResume");
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        reminderCheck(prefs);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Util.log("EditPreferences.onPause");
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (!key.equals(KEY_REMIND_RINGTONE))
			Util.log(key + "=" + prefs.getBoolean(key, true));
		
		if (key.equals(KEY_REMIND_QUESTIONS)) {
			if (prefs.getBoolean(key, true)) {
				// TODO: Implement when question reminder code ready
			}
			reminderCheck(prefs);
		} else if (key.equals(KEY_REMIND_RACE)) {
			if (prefs.getBoolean(key, true)) {
				RaceAlarm.set(this);
			}
			reminderCheck(prefs);
		}
	}
	
	private void reminderCheck(SharedPreferences prefs) {
		if (prefs.getBoolean(KEY_REMIND_QUESTIONS, true) ||
			prefs.getBoolean(KEY_REMIND_RACE, true)) {
			findPreference(KEY_REMIND_VIBRATE).setEnabled(true);
			findPreference(KEY_REMIND_LED).setEnabled(true);
			findPreference(KEY_REMIND_RINGTONE).setEnabled(true);
		} else {
			findPreference(KEY_REMIND_VIBRATE).setEnabled(false);
			findPreference(KEY_REMIND_LED).setEnabled(false);
			findPreference(KEY_REMIND_RINGTONE).setEnabled(false);
		}
	}
}
