/*
 * Copyright (C) 2012-2014 Gregory S. Meiste  <http://gregmeiste.com>
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

public final class Player {
    public String name;
    public Integer rank;
    public int points;
    public int races;
    public int wins;
    public boolean friend;
    public boolean inChase;

    public String getName() {
        return name;
    }

    public String getRank() {
        return (rank == null) ? "-" : rank.toString();
    }

    public String getPoints() {
        return Integer.toString(points);
    }

    public String getRaces() {
        return Integer.toString(races);
    }

    public String getWins() {
        return Integer.toString(wins);
    }

    public boolean isInChase() {
        return inChase;
    }

    public boolean isIdentifiable() {
        return (name != null) || (rank != null);
    }

    public boolean isFriend() {
        return friend;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Player fromJson(final String json) {
        return new Gson().fromJson(json, Player.class);
    }
}
