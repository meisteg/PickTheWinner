/*
 * Copyright 2012-2013 Google Inc.
 * Copyright (C) 2013 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.greg.ptw.gcm;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.format.DateUtils;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.meiste.greg.ptw.GAE;
import com.meiste.greg.ptw.Util;

/**
 * Utilities for device registration.
 * <p>
 * <strong>Note:</strong> this class uses a private {@link SharedPreferences}
 * object to keep track of the registration token.
 */
public final class Gcm {

    private static final long ON_SERVER_LIFESPAN_MS = 7 * DateUtils.DAY_IN_MILLIS;

    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;

    private final static String SENDER_ID = "540948604089";

    private static final String GCM_PREFS = "com.google.android.gcm";
    private static final String PROPERTY_REG_ID = "regId";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER = "onServer";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTime";

    /**
     * Checks whether the application was successfully registered on GCM
     * service.
     */
    public static boolean isRegistered(final Context context) {
        return getRegistrationId(context).length() > 0;
    }

    /**
     * Checks whether the device was successfully registered in the server side.
     */
    public static boolean isRegisteredOnServer(final Context context) {
        final SharedPreferences prefs = getGcmPrefs(context);
        final boolean isRegistered = prefs.getBoolean(PROPERTY_ON_SERVER, false);
        Util.log("Is registered on server: " + isRegistered);
        if (isRegistered) {
            // checks if the information is not stale
            final long expirationTime =
                    prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
            if (System.currentTimeMillis() > expirationTime) {
                Util.log("flag expired on: " + new Timestamp(expirationTime));
                return false;
            }
        }
        return isRegistered;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public static String getRegistrationId(final Context context) {
        final SharedPreferences prefs = getGcmPrefs(context);
        final String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Util.log("Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        final int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        final int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Util.log("App version changed from " + registeredVersion + " to " + currentVersion);
            return "";
        }
        return registrationId;
    }

    /**
     * Checks if the application needs to be registered, and registers if needed.
     */
    public static void registerIfNeeded(final Context context) {
        if (!isRegistered(context) || !isRegisteredOnServer(context)) {
            Util.log("Registering with GCM");
            register(context);
        } else {
            Util.log("Already registered with GCM: " + getRegistrationId(context));
        }
    }

    /**
     * Registers the device with GCM service.
     */
    public static void register(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
                    final String regId = gcm.register(SENDER_ID);
                    if (sendRegistrationIdToBackend(context, regId)) {
                        storeRegistrationId(context, regId);
                    }
                } catch (final IOException e) {
                    Util.log("GCM registration failed: " + e);
                }
            }
        }).start();
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private static SharedPreferences getGcmPrefs(final Context context) {
        return context.getSharedPreferences(GCM_PREFS, Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(final Context context) {
        try {
            final PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (final NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private static void storeRegistrationId(final Context context, final String regId) {
        final SharedPreferences prefs = getGcmPrefs(context);
        final int appVersion = getAppVersion(context);
        Util.log("Saving regId on app version " + appVersion);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    /**
     * Sets whether the device was successfully registered in the server side.
     */
    private static void setRegisteredOnServer(final Context context, final boolean flag) {
        final SharedPreferences prefs = getGcmPrefs(context);
        final Editor editor = prefs.edit();
        editor.putBoolean(PROPERTY_ON_SERVER, flag);
        // set the flag's expiration date
        final long expirationTime = System.currentTimeMillis() + ON_SERVER_LIFESPAN_MS;
        Util.log("Setting registeredOnServer status as " + flag + " until " +
                new Timestamp(expirationTime));
        editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
        editor.apply();
    }

    /**
     * Sends the registration ID to app server.
     */
    private static boolean sendRegistrationIdToBackend(final Context context, final String regId) {
        Util.log("Device registered with GCM: regId = " + regId);

        final String serverUrl = GAE.PROD_URL + "/register";
        final Map<String, String> params = new HashMap<String, String>();
        params.put("regId", regId);
        long backoff = BACKOFF_MILLI_SECONDS;

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            Util.log("Attempt #" + i + " to register device on PTW server");
            try {
                post(serverUrl, params);
                setRegisteredOnServer(context, true);
                Util.log("Device successfully registered on PTW server");
                return true;
            } catch (final IOException e) {
                Util.log("Failed to register on attempt " + i + " with " + e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Util.log("Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (final InterruptedException e1) {
                    Util.log("Thread interrupted: abort remaining retries!");
                    break;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
        return false;
    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params request parameters.
     *
     * @throws IOException propagated from POST.
     */
    private static void post(final String endpoint, final Map<String, String> params)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        final StringBuilder bodyBuilder = new StringBuilder();
        final Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            final Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
            .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        final String body = bodyBuilder.toString();
        Util.log("Posting '" + body + "' to " + url);
        final byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            final OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            final int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private Gcm() {
        throw new UnsupportedOperationException();
    }
}
