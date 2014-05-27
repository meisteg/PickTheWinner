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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootSetup extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
            Util.log("Checking package replaced");
            if (!intent.getData().getSchemeSpecificPart().equals(context.getPackageName())) {
                // Not Pick The Winner, so ignore
                return;
            }
        }

        if (Eula.hasAccepted(context)) {
            Util.log("Running boot setup");
            RaceAlarm.set(context);
            QuestionAlarm.set(context);
        } else
            Util.log("Skipping boot setup since EULA not accepted");
    }

}