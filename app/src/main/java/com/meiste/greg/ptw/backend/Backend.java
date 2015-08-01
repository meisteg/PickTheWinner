/*
 * Copyright (C) 2015 Gregory S. Meiste  <http://gregmeiste.com>
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

package com.meiste.greg.ptw.backend;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.meiste.greg.ptw.EditPreferences;
import com.meiste.greg.ptw.R;

import java.util.LinkedList;
import java.util.Queue;

import timber.log.Timber;

public class Backend implements Handler.Callback {

    private static final Object sInstanceSync = new Object();
    private static Backend sInstance;

    private final Context mContext;
    private final Handler mHandler;
    private final Queue<Message> mRequestQueue;
    private Message mCurrentRequest;

    public static Backend getInstance(@NonNull final Context context) {
        synchronized (sInstanceSync) {
            if (sInstance == null) {
                Timber.v("Instantiate Backend object");
                sInstance = new Backend(context.getApplicationContext());
            }
        }
        return sInstance;
    }

    private Backend(@NonNull final Context context) {
        mContext = context;
        mRequestQueue = new LinkedList<>();

        final HandlerThread handlerThread = new HandlerThread(Backend.class.getName());
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), this);
    }

    private void expireCookieIfTesting() {
        if (mContext.getResources().getBoolean(R.bool.gae_force_cookie_expired)) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.edit().putString(EditPreferences.KEY_ACCOUNT_COOKIE, "ExpiredCookie").apply();
        }
    }

    public PendingResult<ResultGet> getPage(@NonNull final String page) {
        expireCookieIfTesting();
        return newRequest(new RequestGet(page)); // TODO: also create PutRequestPut, RequestConnect
    }

    private synchronized <T extends Result> PendingResult<T> newRequest(final Request<T> request) {
        final Message msg = mHandler.obtainMessage(0 /* what */, request);

        if (mCurrentRequest == null) {
            sendRequest(msg);
        } else {
            mRequestQueue.add(msg);
        }

        return request;
    }

    private void sendRequest(final Message msg) {
        mCurrentRequest = msg;
        mHandler.sendMessage(msg);
    }

    private synchronized void sendNextQueuedRequest() {
        Timber.v("sendNextQueuedRequest: size=%d", mRequestQueue.size());

        final Message msg = mRequestQueue.poll();
        if (msg != null) {
            sendRequest(msg);
        } else {
            mCurrentRequest = null;
        }
    }

    @Override
    public boolean handleMessage(final Message msg) {
        final Request request = (Request) msg.obj;
        request.run();
        sendNextQueuedRequest();

        // Indicate to handler that message was handled
        return true;
    }
}
