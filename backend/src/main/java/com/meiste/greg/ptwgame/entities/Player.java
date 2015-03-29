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

import com.google.appengine.api.users.User;
import com.google.gson.annotations.Expose;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;

import java.util.List;
import java.util.logging.Logger;

import static com.meiste.greg.ptwgame.OfyService.ofy;

@Entity
@Index
@Cache
public class Player {
    private static final Logger log = Logger.getLogger(Player.class.getName());

    @SuppressWarnings("unused")
    @Id
    private Long id;

    public String mUserId;
    public String userEmail;

    @Expose
    public String name;

    @Expose
    public Integer rank;

    @Expose
    public Integer points;

    @Expose
    public Integer races;

    @Expose
    public Integer wins;

    @Expose
    @Ignore
    public boolean friend;

    @Expose
    @Ignore
    public boolean inChase;

    public static synchronized Player getByUser(final User user) {
        Player player = getByProperty("mUserId", user.getUserId());
        if (player == null) {
            log.info("User " + user + " not found. Creating player...");
            player = new Player(user);
            Player.put(player, true);
        }
        return player;
    }

    public static Player getByRank(final int rank) {
        return getByProperty("rank", rank);
    }

    public static Player getByName(final String name) {
        return getByProperty("name", name);
    }

    public static List<Player> getList(final int limit) {
        return ofy().load().type(Player.class)
                .filter("rank !=", null)
                .order("rank")
                .limit(limit)
                .list();
    }

    public static List<Player> getForRanking() {
        return ofy().load().type(Player.class)
                .order("-points").order("-wins").order("-races")
                .list();
    }

    private static Player getByProperty(final String propName, final Object propValue) {
        return ofy().load().type(Player.class)
                .filter(propName, propValue)
                .first().now();
    }

    public static void put(final Player player, final boolean now) {
        final Result<Key<Player>> result = ofy().save().entity(player);
        if (now) {
            result.now();
        }
    }

    public Player() {
    }

    public Player(final User user) {
        mUserId = user.getUserId();
        userEmail = user.getEmail();

        // Do not set rank. A null rank indicates "not ranked".
        points = races = wins = 0;
    }

    public boolean isIdentifiable() {
        return (name != null) || (rank != null);
    }

    public Ref<Player> getRef() {
        return Ref.create(this);
    }

    public void reset() {
        rank = null;
        points = races = wins = 0;
        Player.put(this, false);
    }
}
