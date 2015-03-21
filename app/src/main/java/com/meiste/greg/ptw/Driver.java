/*
 * Copyright (C) 2012-2013, 2015 Gregory S. Meiste  <http://gregmeiste.com>
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

import android.content.res.Resources;
import android.text.TextUtils;

import com.google.gson.Gson;

public final class Driver {
    private int mNumber;
    private String mFirstName;
    private String mLastName;

    public static Driver fromJson(final String json) {
        return new Gson().fromJson(json, Driver.class);
    }

    public static Driver find(final Driver[] drivers, final Resources res, final int num) {
        if (drivers != null) {
            for (final Driver driver : drivers) {
                if (driver.getNumber() == num) {
                    return driver;
                }
            }
        }

        // This should never happen
        final Driver driver = new Driver();
        driver.mNumber = num;
        driver.mFirstName = "";
        driver.mLastName = res.getString(R.string.not_available);
        return driver;
    }

    public int getNumber() {
        return mNumber;
    }

    public String getName() {
        if (TextUtils.isEmpty(mFirstName)) {
            return mLastName;
        }
        return mFirstName + " " + mLastName;
    }

    @Override
    public String toString() {
        return getName();
    }
}
