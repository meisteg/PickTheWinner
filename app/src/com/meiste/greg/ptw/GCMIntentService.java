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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.Intent;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;

    public GCMIntentService() {
        super(Util.GCM_SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String regId) {
        Util.log("Device registered with GCM: regId = " + regId);

        String serverUrl = GAE.PROD_URL + "/register";
        Map<String, String> params = new HashMap<String, String>();
        params.put("regId", regId);
        long backoff = BACKOFF_MILLI_SECONDS;

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            Util.log("Attempt #" + i + " to register device on PTW server");
            try {
                post(serverUrl, params);
                GCMRegistrar.setRegisteredOnServer(context, true);
                Util.log("Device successfully registered on PTW server");
                return;
            } catch (IOException e) {
                Util.log("Failed to register on attempt " + i + " with " + e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Util.log("Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    Util.log("Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    break;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }

        Util.log("PTW server register failed. Unregister from GCM");
        GCMRegistrar.unregister(context);
    }

    @Override
    protected void onUnregistered(Context context, String regId) {
        Util.log("Device unregistered from GCM");

        if (!GCMRegistrar.isRegisteredOnServer(context))
            return;

        String serverUrl = GAE.PROD_URL + "/unregister";
        Map<String, String> params = new HashMap<String, String>();
        params.put("regId", regId);
        try {
            post(serverUrl, params);
            GCMRegistrar.setRegisteredOnServer(context, false);
            Util.log("Device unregistered from PTW server");
        } catch (IOException e) {
            // At this point the device is unregistered from GCM, but still
            // registered in the server. It is not necessary to try to
            // unregister again: if the server tries to send a message to the
            // device, it will get a "NotRegistered" error message and should
            // unregister the device.
            Util.log("Failed to unregister device from PTW server");
        }
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Util.log("Received message from GCM");
        /* TODO: Update standings and notify user */
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
        Util.log("Received deleted messages notification from GCM");
        /* TODO: Update standings and notify user */
    }

    @Override
    public void onError(Context context, String errorId) {
        Util.log("Received error from GCM: " + errorId);
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        Util.log("Received recoverable error from GCM: " + errorId);
        return super.onRecoverableError(context, errorId);
    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params request parameters.
     *
     * @throws IOException propagated from POST.
     */
    private static void post(String endpoint, Map<String, String> params)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
            .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        Util.log("Posting '" + body + "' to " + url);
        byte[] bytes = body.getBytes();
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
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
