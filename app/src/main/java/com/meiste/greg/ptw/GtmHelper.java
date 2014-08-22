/*
 * Copyright (C) 2014 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.res.Configuration;
import android.text.format.DateUtils;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.Container.FunctionCallMacroCallback;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;

public class GtmHelper implements ResultCallback<ContainerHolder>, ContainerHolder.ContainerAvailableListener,
FunctionCallMacroCallback {

    public final static String KEY_AD_ID = "adId";
    public final static String KEY_AD_SIZE = "adSize";
    public final static String KEY_ALLOW_REMOVE_ADS = "allowRemoveAds";
    public final static String KEY_GAME_ENABLED = "gameEnabled";
    public final static String KEY_ORIENTATION = "orientation";
    public final static String KEY_SCREEN_LAYOUT = "screenLayout";

    private final static String CONTAINER_ID = "GTM-PHC3RK";
    private final static long RESULT_TIMEOUT = 2 * DateUtils.SECOND_IN_MILLIS;

    private static GtmHelper sInstance;

    private final Context mContext;
    private final TagManager mTagManager;
    private final List<OnContainerAvailableListener> mListeners = new ArrayList<OnContainerAvailableListener>();
    private ContainerHolder mContainerHolder;

    public interface OnContainerAvailableListener {
        public void onContainerAvailable(Context context, Container container);
    }

    public static synchronized GtmHelper getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new GtmHelper(context);
        }
        return sInstance;
    }

    private GtmHelper(final Context context) {
        mContext = context.getApplicationContext();
        mTagManager = TagManager.getInstance(mContext);
        if (BuildConfig.DEBUG) {
            mTagManager.setVerboseLoggingEnabled(true);
        }
        loadContainer();
    }

    private void loadContainer() {
        final PendingResult<ContainerHolder> pending =
                mTagManager.loadContainerPreferNonDefault(CONTAINER_ID,
                        R.raw.gtm_default_container);
        pending.setResultCallback(this, RESULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void onResult(final ContainerHolder containerHolder) {
        if (!containerHolder.getStatus().isSuccess()) {
            Util.log("Failure loading container. Trying again.");
            loadContainer();
            return;
        }

        Util.log("Container loaded successfully");
        mContainerHolder = containerHolder;

        mTagManager.getDataLayer().push(KEY_SCREEN_LAYOUT,
                mContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);
        setCallbackFunctions();

        for (final OnContainerAvailableListener l : mListeners) {
            l.onContainerAvailable(mContext, mContainerHolder.getContainer());
        }
        // Avoid ConcurrentModificationException by clearing all listeners after the loop
        mListeners.clear();

        containerHolder.setContainerAvailableListener(this);
    }

    @Override
    public void onContainerAvailable(final ContainerHolder containerHolder, final String containerVersion) {
        // Components will get the new container on next call to getContainer()
        Util.log("New container available: version=" + containerVersion);
        setCallbackFunctions();
    }

    private void setCallbackFunctions() {
        mContainerHolder.getContainer().registerFunctionCallMacroCallback(KEY_ORIENTATION, this);
    }

    @Override
    public Object getValue(final String name, final Map<String, Object> parameters) {
        if (name.equals(KEY_ORIENTATION)) {
            return mContext.getResources().getConfiguration().orientation;
        }
        return null;
    }

    public synchronized void getContainer(final OnContainerAvailableListener l) {
        if (mContainerHolder != null) {
            l.onContainerAvailable(mContext, mContainerHolder.getContainer());
        } else {
            mListeners.add(l);
        }
    }
}
