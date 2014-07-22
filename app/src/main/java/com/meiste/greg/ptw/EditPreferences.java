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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.MenuItem;

import com.google.android.gms.analytics.GoogleAnalytics;

public class EditPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
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

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mVibrate = findPreference(KEY_NOTIFY_VIBRATE);
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            Util.log("Remove vibrator option since vibrator not present");
            final PreferenceCategory pc = (PreferenceCategory)findPreference(KEY_REMINDER_SETTINGS);
            pc.removePreference(mVibrate);
            mVibrate = null;
        }

        findPreference(KEY_BUILD).setSummary(BuildConfig.VERSION_NAME);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        Util.log("EditPreferences.onResume");

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        reminderCheck(prefs);
        setRingtoneSummary(prefs);

        final Preference account = findPreference(KEY_ACCOUNT_SCREEN);
        account.setSummary(prefs.getString(KEY_ACCOUNT_EMAIL, getString(R.string.account_needed)));

        mLogHitCountdown = TAPS_TO_ENABLE_LOGGING;
    }

    @Override
    public void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
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
        if (key.equals(KEY_NOTIFY_RINGTONE)) {
            setRingtoneSummary(prefs);
        }
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

    @SuppressWarnings("deprecation")
    private void setRingtoneSummary(final SharedPreferences prefs) {
        String tone = prefs.getString(KEY_NOTIFY_RINGTONE, PTW.DEFAULT_NOTIFY_SND);
        Preference preference = findPreference(KEY_NOTIFY_RINGTONE);

        if (TextUtils.isEmpty(tone)) {
            // Empty values correspond to 'silent' (no ringtone).
            preference.setSummary(R.string.ringtone_silent);
        } else {
            Ringtone ringtone = RingtoneManager.getRingtone(
                    preference.getContext(), Uri.parse(tone));

            if (ringtone == null) {
                // Set generic summary if there was a lookup error.
                preference.setSummary(R.string.ringtone_summary);
            } else {
                // Set the summary to reflect the new ringtone display name.
                preference.setSummary(ringtone.getTitle(preference.getContext()));
            }
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
            Analytics.trackEvent(this, "Preferences", preference.getKey(), setting);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
