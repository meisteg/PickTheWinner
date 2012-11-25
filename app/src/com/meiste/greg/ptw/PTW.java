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

import android.app.Application;

public class PTW extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Util.log("Application onCreate");

        /* HACK: Instantiate GAE class here so it can be used by activities and
         * services. For some reason, passing getApplicationContext() to GAE
         * from a service doesn't work */
        GAE.getInstance(this);
    }
}
