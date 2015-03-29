/*
 * Copyright (C) 2012-2015 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.meiste.greg.ptwgame.RefUtils;

import static com.meiste.greg.ptwgame.OfyService.ofy;

@Entity
@Cache
public class RaceQuestions {
    private static class LoadRace {}

    @SuppressWarnings("unused")
    @Id
    private Long id;

    @Index
    @Load(LoadRace.class)
    public Ref<Race> raceRef;

    @Load
    public List<Ref<Driver>> driverRefs;

    @Ignore
    @Expose
    public List<Driver> drivers;

    @Expose
    public String q2;

    @Expose
    public String[] a2;

    @Expose
    public String q3;

    @Expose
    public String[] a3;

    public static RaceQuestions get(final Race race) {
        return ofy().load().type(RaceQuestions.class)
                .filter("raceRef", race.getRef())
                .first().now();
    }

    public static List<RaceQuestions> getAll() {
        return ofy().load().group(LoadRace.class).type(RaceQuestions.class).list();
    }

    public static void put(final RaceQuestions rq) {
        ofy().save().entity(rq).now();
    }

    @SuppressWarnings("unused")
    public RaceQuestions() {
        // Needed by objectify
    }

    public RaceQuestions(final Race race) {
        raceRef = race.getRef();
    }

    @SuppressWarnings("unused")
    @OnLoad
    private void setDrivers() {
        drivers = RefUtils.deref(driverRefs);
    }

    @SuppressWarnings("unused")
    @OnSave
    private void setDriverRefs() {
        if ((driverRefs == null) && (drivers != null)) {
            driverRefs = RefUtils.ref(drivers);
        }
    }

    public void setQ2(final String q) {
        q2 = fixUp(q);
    }

    public void setA2(final String[] a) {
        a2 = a;
    }

    public void setQ3(final String q) {
        q3 = fixUp(q);
    }

    public void setA3(final String[] a) {
        a3 = a;
    }

    public Race getRace() {
        return raceRef.get();
    }

    public void setDefaults() {
        drivers = Driver.getAll();
        q2 = "Which manufacturer will have more cars finish in the top 10?";
        a2 = new String[] {"Chevrolet", "Ford", "Toyota"};
        q3 = "Will there be a new points leader after the race?";
        a3 = new String[] {"Yes", "No"};
    }

    public String toJson() {
        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }

    private String fixUp(final String s) {
        return s.replace("’", "'");
    }
}
