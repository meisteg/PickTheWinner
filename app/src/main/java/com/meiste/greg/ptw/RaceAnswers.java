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

import com.google.gson.Gson;

public final class RaceAnswers {
    public Integer a1;
    public Integer a2;
    public Integer a3;
    public Integer a4;
    public Integer a5;

    public static RaceAnswers fromJson(final String json) {
        return new Gson().fromJson(json, RaceAnswers.class);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
