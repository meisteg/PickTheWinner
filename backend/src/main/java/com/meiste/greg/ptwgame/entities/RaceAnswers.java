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
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

import java.util.List;

import static com.meiste.greg.ptwgame.OfyService.ofy;

@Entity
@Index
@Cache
public class RaceAnswers {
    private static class LoadRace {}
    private static class LoadPlayer {}

    @SuppressWarnings("unused")
    @Id
    private Long id;

    public long timestamp = System.currentTimeMillis();

    @Load(LoadRace.class)
    public Ref<Race> raceRef;

    @Load(LoadPlayer.class)
    public Ref<Player> playerRef;

    @SuppressWarnings("unused")
    @Expose
    public Integer a1;

    @SuppressWarnings("unused")
    @Expose
    public Integer a2;

    @SuppressWarnings("unused")
    @Expose
    public Integer a3;

    @SuppressWarnings("unused")
    @Expose
    public Integer a4;

    @SuppressWarnings("unused")
    @Expose
    public Integer a5;

    public static RaceAnswers get(final Race race, final Player player) {
        return ofy().load().type(RaceAnswers.class)
                .filter("raceRef", race.getRef())
                .filter("playerRef", player.getRef())
                .first().now();
    }

    public static List<RaceAnswers> getAllForUser(final Player player) {
        return ofy().load().group(LoadRace.class).type(RaceAnswers.class)
                .filter("playerRef", player.getRef())
                .list();
    }

    public static List<RaceAnswers> getAllForRace(final Race race) {
        return ofy().load().group(LoadPlayer.class).type(RaceAnswers.class)
                .filter("raceRef", race.getRef())
                .list();
    }

    public static void put(final RaceAnswers answers) {
        ofy().save().entity(answers).now();
    }

    private RaceAnswers() {
        // Needed by objectify and gson
    }

    public void setPlayer(final Player player) {
        playerRef = player.getRef();
    }

    public Player getPlayer() {
        return playerRef.get();
    }

    public void setRace(final Race race) {
        raceRef = race.getRef();
    }

    public Race getRace() {
        return raceRef.get();
    }

    public static RaceAnswers fromJson(final String json) {
        return new Gson().fromJson(json, RaceAnswers.class);
    }

    public String toJson() {
        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }
}
