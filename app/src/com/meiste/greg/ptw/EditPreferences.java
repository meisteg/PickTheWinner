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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

public class EditPreferences extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {
    public static final String KEY_ACCOUNT_EMAIL = "account.email";
    public static final String KEY_ACCOUNT_COOKIE = "account.cookie";

    public static final String KEY_REMIND_QUESTIONS = "remind.questions";
    public static final String KEY_REMIND_RACE = "remind.race";
    public static final String KEY_REMIND_VIBRATE = "remind.vibrate";
    public static final String KEY_REMIND_LED = "remind.led";
    public static final String KEY_REMIND_RINGTONE = "remind.ringtone";

    private static final String KEY_ACCOUNT_SCREEN = "account_screen";
    private static final String KEY_REMINDER_SETTINGS = "reminder_settings_category";

    private Preference mVibrate;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mVibrate = findPreference(KEY_REMIND_VIBRATE);
        boolean methodAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (methodAvailable && (vibrator == null || !vibrator.hasVibrator())) {
            Util.log("Remove vibrator option since vibrator not present");
            PreferenceCategory pc = (PreferenceCategory)findPreference(KEY_REMINDER_SETTINGS);
            pc.removePreference(mVibrate);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        Util.log("EditPreferences.onResume");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        reminderCheck(prefs);

        Preference account = findPreference(KEY_ACCOUNT_SCREEN);
        account.setSummary(prefs.getString(KEY_ACCOUNT_EMAIL, getString(R.string.account_needed)));
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
                QuestionAlarm.set(this);
            }
            reminderCheck(prefs);
        } else if (key.equals(KEY_REMIND_RACE)) {
            if (prefs.getBoolean(key, true)) {
                RaceAlarm.set(this);
            }
            reminderCheck(prefs);
        }
    }

    @SuppressWarnings("deprecation")
    private void reminderCheck(SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_REMIND_QUESTIONS, true) ||
                prefs.getBoolean(KEY_REMIND_RACE, true)) {
            findPreference(KEY_REMIND_LED).setEnabled(true);
            findPreference(KEY_REMIND_RINGTONE).setEnabled(true);
            if (mVibrate != null)
                mVibrate.setEnabled(true);
        } else {
            findPreference(KEY_REMIND_LED).setEnabled(false);
            findPreference(KEY_REMIND_RINGTONE).setEnabled(false);
            if (mVibrate != null)
                mVibrate.setEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent homeIntent = new Intent(this, MainActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
