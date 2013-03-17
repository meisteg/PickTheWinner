/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

/**
 * Displays an EULA ("End User License Agreement") that the user has to accept before
 * using the application. Your application should call {@link Eula#show(android.app.Activity)}
 * in the onCreate() method of the first activity. If the user accepts the EULA, it will never
 * be shown again. If the user refuses, {@link android.app.Activity#finish()} is invoked
 * on your activity.
 */
class Eula {
    private static final String ASSET_EULA = "EULA";
    private static final String PREFERENCE_EULA_ACCEPTED = "eula.accepted";
    private static final String PREFERENCES_EULA = "eula";

    private static AlertDialog mDialog = null;

    /**
     * callback to let the activity know when the user has accepted the EULA.
     */
    static interface OnEulaAgreedTo {

        /**
         * Called when the user has accepted the eula and the dialog closes.
         */
        void onEulaAgreedTo();
    }

    private static SharedPreferences getEulaPrefs(final Context context) {
        return context.getSharedPreferences(PREFERENCES_EULA, Activity.MODE_PRIVATE);
    }

    static boolean hasAccepted(final Context context) {
        return getEulaPrefs(context).getBoolean(PREFERENCE_EULA_ACCEPTED, false);
    }

    /**
     * Displays the EULA if necessary. This method should be called from the onCreate()
     * method of your main Activity.
     *
     * @param activity The Activity to finish if the user rejects the EULA.
     * @return Whether the user has agreed already.
     */
    static boolean show(final Activity activity) {
        if (!hasAccepted(activity)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.eula_title);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.eula_accept, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    accept(getEulaPrefs(activity));
                    if (activity instanceof OnEulaAgreedTo) {
                        ((OnEulaAgreedTo) activity).onEulaAgreedTo();
                    }
                }
            });
            builder.setNegativeButton(R.string.eula_refuse, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    refuse(activity);
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(final DialogInterface dialog) {
                    refuse(activity);
                }
            });
            builder.setMessage(readEula(activity));
            mDialog = builder.create();
            mDialog.show();
            return false;
        }
        return true;
    }

    /**
     * Hides the EULA if necessary. This method should be called from the onPause()
     * method of your main Activity to prevent the activity from leaking the window.
     *
     * @return Whether the dialog was actually dismissed.
     */
    static boolean hide() {
        if ((mDialog != null) && (mDialog.isShowing())) {
            mDialog.dismiss();
            return true;
        }
        return false;
    }

    private static void accept(final SharedPreferences preferences) {
        preferences.edit().putBoolean(PREFERENCE_EULA_ACCEPTED, true).apply();
    }

    private static void refuse(final Activity activity) {
        activity.finish();
    }

    private static CharSequence readEula(final Activity activity) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(activity.getAssets().open(ASSET_EULA)));
            String line;
            final StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null) buffer.append(line).append('\n');
            return buffer;
        } catch (final IOException e) {
            return "";
        } finally {
            closeStream(in);
        }
    }

    /**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
    private static void closeStream(final Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (final IOException e) {
                // Ignore
            }
        }
    }
}
