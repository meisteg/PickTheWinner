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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Calendar;

import static com.meiste.greg.ptwgame.OfyService.ofy;

@Entity
@Index
@Cache
public class RaceCorrectAnswers {
    @SuppressWarnings("unused")
    @Id
    private Long id;

    public int mYear = Calendar.getInstance().get(Calendar.YEAR);
    public int mRaceId = -1;

    @Expose
    public Integer a1;

    @Expose
    public Integer a2;

    @Expose
    public Integer a3;

    @Expose
    public Integer a4;

    @Expose
    public Integer a5;

    public static RaceCorrectAnswers get() {
        return ofy().load().type(RaceCorrectAnswers.class)
                .filter("mYear", Calendar.getInstance().get(Calendar.YEAR))
                .order("-mRaceId")
                .limit(1).first().now();
    }

    public static RaceCorrectAnswers get(final Race race) {
        return get(race.raceId);
    }

    public static RaceCorrectAnswers get(final int raceId) {
        return get(Calendar.getInstance().get(Calendar.YEAR), raceId);
    }

    public static RaceCorrectAnswers get(final int year, final int raceId) {
        return ofy().load().type(RaceCorrectAnswers.class)
                .filter("mYear", year)
                .filter("mRaceId", raceId)
                .first().now();
    }

    public static void put(final RaceCorrectAnswers answers) {
        ofy().save().entity(answers).now();
    }

    public RaceCorrectAnswers() {
    }

    public RaceCorrectAnswers(final Race race) {
        mRaceId = race.raceId;
    }

    public String toJson() {
        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }
}
