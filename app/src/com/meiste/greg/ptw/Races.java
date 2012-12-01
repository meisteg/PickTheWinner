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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;

import com.google.gson.Gson;

public final class Races {

    private static final String FILENAME = "schedule";
    private static final Object sRacesSync = new Object();
    private static Race[] sRaces;

    public static Race[] get(Context context) {
        Race[] lRaces;
        Race[] iRaces;

        synchronized (sRacesSync) {
            if (sRaces == null) {
                Util.log("Populating race array");

                lRaces = getLatest(context);
                iRaces = getIncluded(context);
                if ((lRaces == null) || (lRaces.length <= 0)) {
                    // User doesn't have updated schedule, so use included schedule
                    sRaces = iRaces;
                } else if (lRaces[lRaces.length-1].getStartTimestamp() < iRaces[0].getStartTimestamp()) {
                    // Included schedule is newer than updated schedule
                    sRaces = iRaces;
                    Util.log("Removing downloaded schedule because included is newer");
                    context.deleteFile(FILENAME);
                } else {
                    sRaces = lRaces;
                }
            }
        }
        return sRaces;
    }

    public static void update(Context context, String json) {
        synchronized (sRacesSync) {
            try {
                FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                fos.write(json.getBytes());
                fos.close();
                sRaces = null;
            } catch (Exception e) {
                Util.log("Failed to save update to schedule");
            }
        }
    }

    private static Race[] getIncluded(Context context) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(context.getAssets().open(FILENAME)));
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null)
                buffer.append(line).append('\n');
            in.close();
            return new Gson().fromJson(buffer.toString(), Race[].class);
        } catch (IOException e) {
            Util.log("Unable to open included schedule: " + e);
        }

        return null;
    }

    private static Race[] getLatest(Context context) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(context.openFileInput(FILENAME)));
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null)
                buffer.append(line).append('\n');
            in.close();
            return new Gson().fromJson(buffer.toString(), Race[].class);
        } catch (Exception e) {
            Util.log("No updated schedule found");
        }
        return null;
    }
}
