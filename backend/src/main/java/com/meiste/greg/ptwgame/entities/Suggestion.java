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

package com.meiste.greg.ptwgame.entities;

import com.google.appengine.api.users.User;
import com.google.gson.Gson;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;

import java.util.Calendar;

import static com.meiste.greg.ptwgame.OfyService.ofy;

@Entity
@Index
@Cache
public class Suggestion {
    @SuppressWarnings("unused")
    @Id
    private Long id;

    public int mYear = Calendar.getInstance().get(Calendar.YEAR);
    public int mRaceId = -1;
    public String mUserEmail;

    @Unindex
    public String mSuggestion;

    public static void put(final Suggestion suggestion) {
        ofy().save().entity(suggestion).now();
    }

    @SuppressWarnings("unused")
    public Suggestion() {
        // Needed by objectify
    }

    public Suggestion(final Race race, final User user, final String json) {
        mRaceId = race.raceId;
        mUserEmail = (user != null) ? user.getEmail() : null;

        // Limit the input to 200 characters. Done here instead of the app
        // because someone can and will try to submit without the app.
        final String temp = new Gson().fromJson(json, String.class);
        mSuggestion = temp.substring(0, Math.min(200, temp.length()));
    }
}
