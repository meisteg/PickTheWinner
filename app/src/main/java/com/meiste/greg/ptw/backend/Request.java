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

import android.os.Handler;
import android.os.Looper;

import com.meiste.greg.ptw.GAE;
import com.squareup.okhttp.OkHttpClient;

abstract class Request<R extends Result> implements PendingResult<R> {
    private final String mPage;
    private final OkHttpClient mOkClient;
    private ResultCallback<R> mCallback;
    private R mResult;

    public Request(final String page) {
        mPage = page;

        mOkClient = new OkHttpClient();
        mOkClient.setFollowRedirects(false);
    }

    @Override
    public synchronized R await() {
        while (mResult != null) {
            try {
                wait();
            } catch (final InterruptedException e) {
                // Ignore
            }
        }

        return mResult;
    }

    @Override
    public synchronized void setResultCallback(final ResultCallback<R> callback) {
        mCallback = callback;

        // Check if already have result and if so, immediately call back
        if (mResult != null) {
            invokeCallback();
        }
    }

    private synchronized void setResult(final R result) {
        mResult = result;
        notifyAll();

        if (mCallback != null) {
            invokeCallback();
        }
    }

    /** Calls back requesting component on main thread */
    private void invokeCallback() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                mCallback.onResult(mResult);
            }
        });
    }

    protected String getPage() {
        return mPage;
    }

    protected String getUrl() {
        // TODO: Move PROD_URL to neutral class
        return GAE.PROD_URL + "/" + mPage;
    }

    protected OkHttpClient getOkClient() {
        return mOkClient;
    }

    public void run() {
        setResult(execute());
    }

    abstract public R execute();
}
