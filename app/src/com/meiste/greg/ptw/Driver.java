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

import android.content.Context;
import android.content.res.Resources;

import com.google.gson.Gson;

public final class Driver {
    private int mNumber;
    private String mName;

    public static Driver newInstance(final Context context, final int id) {
        final String json = context.getResources().getStringArray(R.array.drivers)[id];
        return new Gson().fromJson(json, Driver.class);
    }

    public static Driver newInstance(final Resources res, final int num) {
        String[] drivers = res.getStringArray(R.array.drivers);
        final Gson gson = new Gson();

        for (final String json : drivers) {
            final Driver driver = gson.fromJson(json, Driver.class);
            if (driver.getNumber() == num)
                return driver;
        }

        // Now check the inactive drivers array
        drivers = res.getStringArray(R.array.drivers_inactive);
        for (final String json : drivers) {
            final Driver driver = gson.fromJson(json, Driver.class);
            if (driver.getNumber() == num)
                return driver;
        }

        final Driver driver = new Driver();
        driver.mNumber = num;
        driver.mName = res.getString(R.string.not_available);
        return driver;
    }

    public static int getNumDrivers(final Context context) {
        return context.getResources().getStringArray(R.array.drivers).length;
    }

    public int getNumber() {
        return mNumber;
    }

    public String getName() {
        return mName;
    }

    @Override
    public String toString() {
        return getName();
    }
}
