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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.meiste.greg.ptw.tab.Questions;
import com.meiste.greg.ptw.tab.Standings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

public final class GAE {

    public static final String PROD_URL = "https://ptwgame.appspot.com";

    private static final String AUTH_COOKIE_NAME = "SACSID";
    // Timeout in milliseconds until a connection is established.
    private static final int TIMEOUT_CONNECTION = 10000;
    // Timeout in milliseconds to wait for data.
    private static final int TIMEOUT_SOCKET = 15000;

    private static final Object sInstanceSync = new Object();
    private static GAE sInstance;

    private final Context mContext;
    private final Handler mHandler;
    private final Object mListenerSync = new Object();
    private GaeListener mListener;
    private String mAccountName;
    private boolean mNeedInvalidate = true;
    private int mRemainingRetries = 3;
    private String mGetPage;
    private String mJson;

    public static interface GaeListener {
        void onFailedConnect(Context context);
        void onLaunchIntent(Intent launch);
        void onConnectSuccess(Context context, String json);
        void onGet(Context context, String json);
    }

    public static GAE getInstance(final Context context) {
        synchronized (sInstanceSync) {
            if (sInstance == null) {
                Util.log("Instantiate GAE object");
                sInstance = new GAE(context);
            }
        }
        return sInstance;
    }

    private GAE(final Context context) {
        mContext = context;
        mHandler = new Handler();
    }

    public void connect(final GaeListener listener, final String account) {
        if ((listener == null) || (account == null))
            throw new IllegalArgumentException("No null arguments allowed");

        final Runnable r = new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSync) {
                    if (mListener == null) {
                        Util.log("Connect using " + account);
                        mListener = listener;
                        mAccountName = account;
                        doConnect();
                    } else
                        mHandler.postDelayed(this, 100);
                }
            }
        };
        mHandler.post(r);
    }

    public void getPage(final GaeListener listener, final String page) {
        if ((listener == null) || (page == null))
            throw new IllegalArgumentException("No null arguments allowed");

        if (mContext.getResources().getBoolean(R.bool.gae_force_cookie_expired)) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.edit().putString(EditPreferences.KEY_ACCOUNT_COOKIE, "ExpiredCookie").apply();
        }

        final Runnable r = new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSync) {
                    if (mListener == null) {
                        Util.log("Getting page " + page);
                        mListener = listener;
                        new GetPageTask().execute(page);
                    } else
                        mHandler.postDelayed(this, 100);
                }
            }
        };
        mHandler.post(r);
    }

    public void postPage(final GaeListener listener, final String page, final String json) {
        if ((listener == null) || (page == null) || (json == null))
            throw new IllegalArgumentException("No null arguments allowed");

        if (mContext.getResources().getBoolean(R.bool.gae_force_cookie_expired)) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.edit().putString(EditPreferences.KEY_ACCOUNT_COOKIE, "ExpiredCookie").apply();
        }

        final Runnable r = new Runnable() {
            @Override
            public void run() {
                synchronized (mListenerSync) {
                    if (mListener == null) {
                        Util.log("Posting to " + page + " page: " + json);
                        mListener = listener;
                        new PostPageTask().execute(page, json);
                    } else
                        mHandler.postDelayed(this, 100);
                }
            }
        };
        mHandler.post(r);
    }

    private void doConnect() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(EditPreferences.KEY_ACCOUNT_EMAIL, null);
        editor.putString(EditPreferences.KEY_ACCOUNT_COOKIE, null);
        editor.apply();

        // HACK: The GAE class should not need to know what to clear on a user
        // change. Ideally, the components would register for this event and
        // handle this themselves. However, the fragments are not guaranteed to
        // be instantiated, and therefore not registered. Handle here for now.
        mContext.getSharedPreferences(Questions.ACACHE, Activity.MODE_PRIVATE).edit().clear().apply();
        mContext.deleteFile(Standings.FILENAME);

        reconnect();
    }

    private boolean reconnect() {
        final AccountManager mgr = AccountManager.get(mContext);
        final Account[] accts = mgr.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        for (final Account acct : accts) {
            if (acct.name.equals(mAccountName)) {
                mgr.getAuthToken(acct, "ah", null, false, new AuthTokenCallback(), null);
                return true;
            }
        }
        Util.log("Account " + mAccountName + " not found!");
        return false;
    }

    private class AuthTokenCallback implements AccountManagerCallback<Bundle> {
        @Override
        public void run(final AccountManagerFuture<Bundle> future) {
            try {
                final Bundle result = future.getResult();

                final Intent launch = (Intent) result.get(AccountManager.KEY_INTENT);
                if (launch != null) {
                    Util.log("Need to launch activity before getting authToken");
                    // How can we get the result of the activity if it is a new task!?
                    launch.setFlags(launch.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
                    cbLaunchIntent(launch);
                    return;
                }

                final String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                if (mNeedInvalidate) {
                    Util.log("Invalidating token and starting over");
                    mNeedInvalidate = false;

                    final AccountManager mgr = AccountManager.get(mContext);
                    mgr.invalidateAuthToken(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE, authToken);
                    reconnect();
                } else {
                    Util.log("authToken=" + authToken);
                    mNeedInvalidate = true;

                    // Phase 2: get authCookie from PTW server
                    new GetCookieTask().execute(authToken);
                }
            } catch (final IOException e) {
                final boolean isOnline = Util.isNetworkConnected(mContext);
                Util.log("Get auth token failed with IOException: online=" + isOnline);

                if (isOnline && (mRemainingRetries > 0)) {
                    mRemainingRetries--;
                    reconnect();
                } else
                    cbFailedConnect();
            } catch (final Exception e) {
                Util.log("Get auth token failed with exception " + e);
                cbFailedConnect();
            }
        }
    }

    private class GetCookieTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(final String... tokens) {
            String authCookie = null;

            try {
                final DefaultHttpClient client = new DefaultHttpClient();
                final URI uri = new URI(PROD_URL + "/_ah/login?continue="
                        + URLEncoder.encode(PROD_URL, "UTF-8") + "&auth=" + tokens[0]);
                final HttpGet method = new HttpGet(uri);
                final HttpParams getParams = new BasicHttpParams();
                HttpClientParams.setRedirecting(getParams, false);
                HttpConnectionParams.setConnectionTimeout(getParams, TIMEOUT_CONNECTION);
                HttpConnectionParams.setSoTimeout(getParams, TIMEOUT_SOCKET);
                method.setParams(getParams);

                final HttpResponse res = client.execute(method);
                final Header[] headers = res.getHeaders("Set-Cookie");
                final int statusCode = res.getStatusLine().getStatusCode();
                if (statusCode != 302 || headers.length == 0) {
                    Util.log("Get auth cookie failed: statusCode=" + statusCode);
                    return false;
                }

                for (final Cookie cookie : client.getCookieStore().getCookies()) {
                    if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                        authCookie = AUTH_COOKIE_NAME + "=" + cookie.getValue();
                        Util.log(authCookie);
                    }
                }
            } catch (final Exception e) {
                Util.log("Get auth cookie failed with exception " + e);
                return false;
            }

            final SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(mContext);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString(EditPreferences.KEY_ACCOUNT_EMAIL, mAccountName);
            editor.putString(EditPreferences.KEY_ACCOUNT_COOKIE, authCookie);
            editor.apply();

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                if (mJson != null)
                    new PostPageTask().execute(mGetPage, mJson);
                else if (mGetPage != null)
                    new GetPageTask().execute(mGetPage);
                else
                    cbConnectSuccess(null);
            } else
                cbFailedConnect();
        }
    }

    private class GetPageTask extends AsyncTask<String, Integer, Boolean> {
        final StringBuilder mBuilder = new StringBuilder();

        @Override
        protected Boolean doInBackground(final String... pages) {
            mGetPage = null;
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final DefaultHttpClient client = new DefaultHttpClient();
            final HttpGet method = new HttpGet(PROD_URL + "/" + pages[0]);

            final HttpParams getParams = new BasicHttpParams();
            HttpClientParams.setRedirecting(getParams, false);
            HttpConnectionParams.setConnectionTimeout(getParams, TIMEOUT_CONNECTION);
            HttpConnectionParams.setSoTimeout(getParams, TIMEOUT_SOCKET);
            method.setParams(getParams);
            method.setHeader("Cookie", prefs.getString(EditPreferences.KEY_ACCOUNT_COOKIE, null));

            try {
                final HttpResponse resp = client.execute(method);

                switch(resp.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    final BufferedReader reader = new BufferedReader(
                            new InputStreamReader(resp.getEntity().getContent()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        mBuilder.append(line).append('\n');
                    }
                    break;
                case HttpStatus.SC_MOVED_TEMPORARILY:
                    if (mAccountName != null) {
                        Util.log("Get page failed (status 302)");
                        return false;
                    }
                    Util.log("Cookie expired? Attempting reconnect");
                    mGetPage = pages[0];
                    mAccountName = prefs.getString(EditPreferences.KEY_ACCOUNT_EMAIL, null);
                    return reconnect();
                default:
                    Util.log("Get page failed (invalid status code)");
                    return false;
                }
            } catch (final Exception e) {
                Util.log("Get page failed with exception " + e);
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                if (mGetPage == null)
                    cbGet(mBuilder.toString());
            } else
                cbFailedConnect();
        }
    }

    private class PostPageTask extends AsyncTask<String, Integer, Boolean> {
        String mJsonReturned;

        @Override
        protected Boolean doInBackground(final String... args) {
            mJson = null;
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final DefaultHttpClient client = new DefaultHttpClient();
            final HttpPost method = new HttpPost(PROD_URL + "/" + args[0]);

            final HttpParams postParams = new BasicHttpParams();
            HttpClientParams.setRedirecting(postParams, false);
            HttpConnectionParams.setConnectionTimeout(postParams, TIMEOUT_CONNECTION);
            HttpConnectionParams.setSoTimeout(postParams, TIMEOUT_SOCKET);
            method.setParams(postParams);
            method.setHeader("Cookie", prefs.getString(EditPreferences.KEY_ACCOUNT_COOKIE, null));

            try {
                final StringEntity se = new StringEntity(args[1]);
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                method.setEntity(se);

                final HttpResponse resp = client.execute(method);

                switch(resp.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    final BufferedReader reader = new BufferedReader(
                            new InputStreamReader(resp.getEntity().getContent()));
                    mJsonReturned = reader.readLine();
                    break;
                case HttpStatus.SC_MOVED_TEMPORARILY:
                    if (mAccountName != null) {
                        Util.log("Post page failed (status 302)");
                        return false;
                    }
                    Util.log("Cookie expired? Attempting reconnect");
                    mGetPage = args[0];
                    mJson = args[1];
                    mAccountName = prefs.getString(EditPreferences.KEY_ACCOUNT_EMAIL, null);
                    return reconnect();
                default:
                    Util.log("Post page failed (invalid status code)");
                    return false;
                }
            } catch (final Exception e) {
                Util.log("Post page failed with exception " + e);
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                if (mJson == null)
                    cbConnectSuccess(mJsonReturned);
            } else
                cbFailedConnect();
        }
    }

    private void cbFailedConnect() {
        mListener.onFailedConnect(mContext);
        synchronized (mListenerSync) {
            mGetPage = mJson = mAccountName = null;
            mListener = null;
        }
    }

    private void cbLaunchIntent(final Intent launch) {
        mListener.onLaunchIntent(launch);
        synchronized (mListenerSync) {
            mGetPage = mJson = mAccountName = null;
            mListener = null;
        }
    }

    private void cbConnectSuccess(final String json) {
        mListener.onConnectSuccess(mContext, json);
        synchronized (mListenerSync) {
            mGetPage = mJson = mAccountName = null;
            mListener = null;
        }
    }

    private void cbGet(final String json) {
        mListener.onGet(mContext, json);
        synchronized (mListenerSync) {
            mGetPage = mJson = mAccountName = null;
            mListener = null;
        }
    }
}
