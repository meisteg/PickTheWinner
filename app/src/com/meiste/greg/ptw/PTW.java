/*
 * Copyright (C) 2012-2013 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Application;
import android.content.Context;

import com.google.analytics.tracking.android.EasyTracker;

public class PTW extends Application {

    /* Base64-encoded RSA public key generated by Google Play */
    public final static String PUB_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAixirisYfpA7rr8w2rNhFCSbXnq+cH0za4JUgmd5Zk3E4/wOWM6ErtZfuNX+nnq9IoJUc9kWteA/ACxNzSaQiGMGFbK346YzVLsXMgRamkgTHAPDEDOiPPbMYAVpp+fDKTj8DxrR5ZnNugbFlqA+EJuzJGE8GgQi0D/7G48hRPDrRY6gKEIvJElEiSK3IgWLaYnDVr2oG2voZmBqcTIQeVZnxd7sinN3YescAw9u27Nobm6RJ10V43Wfkxm9uaTb8hqOdnoN1+z7iFnid4FlI0KFY8kOIQ8aRQuf9lPYR4kaj1qDpamBtoDscxxYJvWBiSvLaDaajV3K6Q64hra9tLQIDAQAB";

    public final static String TAG = "PickTheWinner";

    public final static String INTENT_ACTION_ANSWERS = "com.meiste.greg.ptw.action.ANSWERS";
    public final static String INTENT_ACTION_RACE_ALARM = "com.meiste.greg.ptw.action.RACE_ALARM";
    public final static String INTENT_ACTION_SCHEDULE = "com.meiste.greg.ptw.action.SCHEDULE";
    public final static String INTENT_ACTION_STANDINGS = "com.meiste.greg.ptw.action.STANDINGS";

    private BitmapLruCache mCache;

    @Override
    public void onCreate() {
        super.onCreate();
        Util.log("Application onCreate");

        EasyTracker.getInstance().setContext(this);

        /* HACK: Instantiate GAE class here so it can be used by activities and
         * services. For some reason, passing getApplicationContext() to GAE
         * from a service doesn't work */
        GAE.getInstance(this);

        final File cacheLocation = new File(getCacheDir() + File.separator + "bitmaps");
        cacheLocation.mkdirs();

        final BitmapLruCache.Builder builder = new BitmapLruCache.Builder(this);
        builder.setMemoryCacheEnabled(true).setMemoryCacheMaxSizeUsingHeapSize();
        builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheLocation);

        mCache = builder.build();
    }

    public BitmapLruCache getBitmapCache() {
        return mCache;
    }

    public static PTW getApplication(final Context context) {
        return (PTW) context.getApplicationContext();
    }
}
