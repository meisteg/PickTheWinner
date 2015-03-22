/*
 * Copyright (C) 2012, 2014-2015 Gregory S. Meiste  <http://gregmeiste.com>
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;

public final class Races {

    private static final String FILENAME = "schedule";
    private static final Object sRacesSync = new Object();
    private static Race[] sRaces;

    public static Race[] get(final Context context) {
        synchronized (sRacesSync) {
            if (sRaces == null) {
                Util.log("Populating race array");
                sRaces = getDownloaded(context);
            }
        }
        return sRaces;
    }

    public static boolean update(final Context context, final String json) {
        synchronized (sRacesSync) {
            try {
                sRaces = new Gson().fromJson(json, Race[].class);
            } catch (final Exception e) {
                Util.log("Failed to parse downloaded schedule: " + e);
                return false;
            }

            try {
                final FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                fos.write(json.getBytes());
                fos.close();
            } catch (final Exception e) {
                Util.log("Failed to save update to schedule: " + e);
                return false;
            }
        }

        RaceAlarm.reset(context);
        context.sendBroadcast(new Intent(PTW.INTENT_ACTION_SCHEDULE));
        return true;
    }

    private static Race[] getDownloaded(final Context context) {
        try {
            final InputStream is = context.openFileInput(FILENAME);
            final BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            final StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null)
                buffer.append(line).append('\n');
            in.close();
            return new Gson().fromJson(buffer.toString(), Race[].class);
        } catch (final Exception e) {
            Util.log("Failed to open/parse schedule: " + e);
        }
        return new Race[0];
    }
}
