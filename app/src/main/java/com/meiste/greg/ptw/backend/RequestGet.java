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

import com.squareup.okhttp.Response;

import java.io.IOException;

import timber.log.Timber;

public class RequestGet extends Request<ResultGet> {

    public RequestGet(final String page) {
        super(page);
    }

    @Override
    public ResultGet execute() {
        Timber.v("Getting %s", getPage());

        final com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                .url(getUrl())
                .build();
        try {
            final Response response = getOkClient().newCall(request).execute();
            if (response.isSuccessful()) {
                return new ResultGet(Result.SUCCESS, response.body().string());
            }
            // TODO: Handle expired cookie use case
        } catch (final IOException e) {
            Timber.e(e, "Failed to get %s", getPage());
        }

        return new ResultGet(Result.ERROR, null);
    }
}
