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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class EditPreferences extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {
    public static final String KEY_ACCOUNT_EMAIL = "account.email";
    public static final String KEY_ACCOUNT_COOKIE = "account.cookie";

    public static final String KEY_NOTIFY_QUESTIONS = "remind.questions";
    public static final String KEY_NOTIFY_RACE = "remind.race";
    public static final String KEY_NOTIFY_RESULTS = "remind.results";
    public static final String KEY_NOTIFY_VIBRATE = "remind.vibrate";
    public static final String KEY_NOTIFY_LED = "remind.led";
    public static final String KEY_NOTIFY_RINGTONE = "remind.ringtone";

    private static final String KEY_ACCOUNT_SCREEN = "account_screen";
    private static final String KEY_REMINDER_SETTINGS = "reminder_settings_category";
    private static final String KEY_BUILD = "build";

    private static final int TAPS_TO_ENABLE_LOGGING = 7;

    private int mLogHitCountdown;
    private Preference mVibrate;

    @TargetApi(11)
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mVibrate = findPreference(KEY_NOTIFY_VIBRATE);
        final boolean methodAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (methodAvailable && (vibrator == null || !vibrator.hasVibrator())) {
            Util.log("Remove vibrator option since vibrator not present");
            final PreferenceCategory pc = (PreferenceCategory)findPreference(KEY_REMINDER_SETTINGS);
            pc.removePreference(mVibrate);
            mVibrate = null;
        }

        try {
            final PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            final Preference build = findPreference(KEY_BUILD);
            if (BuildConfig.DEBUG) {
                build.setSummary(pInfo.versionName + " " + getString(R.string.debug_build));
            } else {
                build.setSummary(pInfo.versionName);
            }
        } catch (final NameNotFoundException e) {}
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        Util.log("EditPreferences.onResume");

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        reminderCheck(prefs);

        final Preference account = findPreference(KEY_ACCOUNT_SCREEN);
        account.setSummary(prefs.getString(KEY_ACCOUNT_EMAIL, getString(R.string.account_needed)));

        mLogHitCountdown = TAPS_TO_ENABLE_LOGGING;
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Util.log("EditPreferences.onPause");
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        reminderCheck(prefs);
    }

    @SuppressWarnings("deprecation")
    private void reminderCheck(final SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_NOTIFY_QUESTIONS, true) ||
                prefs.getBoolean(KEY_NOTIFY_RACE, true) ||
                prefs.getBoolean(KEY_NOTIFY_RESULTS, true)) {
            findPreference(KEY_NOTIFY_LED).setEnabled(true);
            findPreference(KEY_NOTIFY_RINGTONE).setEnabled(true);
            if (mVibrate != null)
                mVibrate.setEnabled(true);
        } else {
            findPreference(KEY_NOTIFY_LED).setEnabled(false);
            findPreference(KEY_NOTIFY_RINGTONE).setEnabled(false);
            if (mVibrate != null)
                mVibrate.setEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            final Intent homeIntent = new Intent(this, MainActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference) {
        if (preference.getKey().equals(KEY_BUILD) && (mLogHitCountdown > 0)) {
            mLogHitCountdown--;
            if ((mLogHitCountdown == 0) && !Util.LOGGING_ENABLED) {
                Util.LOGGING_ENABLED = true;
                Util.log("Debug logging is temporarily enabled");
            }
        } else if (preference instanceof CheckBoxPreference) {
            final String setting = ((CheckBoxPreference)preference).isChecked() ? "enable" : "disable";
            EasyTracker.getInstance(this).send(
                    MapBuilder.createEvent("Preferences", preference.getKey(), setting, null).build());
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
