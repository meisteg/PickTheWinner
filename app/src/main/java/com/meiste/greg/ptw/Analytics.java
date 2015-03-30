/*
 * Copyright (C) 2014-2015 Gregory S. Meiste  <http://gregmeiste.com>
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

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class Analytics {

    private static Tracker sTracker;

    /**
     * Get the global {@link com.google.android.gms.analytics.Tracker} instance.
     */
    private static synchronized Tracker getTracker(final Context context) {
        if (sTracker == null) {
            sTracker = GoogleAnalytics.getInstance(context.getApplicationContext())
                    .newTracker(R.xml.analytics);
        }
        return sTracker;
    }

    public static void init(final Context context) {
        GoogleAnalytics.getInstance(context).setLocalDispatchPeriod(60);
        GoogleAnalytics.getInstance(context).setDryRun(BuildConfig.DEBUG);

        getTracker(context);
    }

    public static void trackEvent(final Context context, final String category,
            final String action, final String label) {
        getTracker(context).send(new HitBuilders.EventBuilder()
        .setCategory(category).setAction(action).setLabel(label).build());
    }
}
