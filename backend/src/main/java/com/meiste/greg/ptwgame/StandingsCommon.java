/*
 * Copyright (C) 2014 Gregory S. Meiste  <http://gregmeiste.com>
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

package com.meiste.greg.ptwgame;

import com.meiste.greg.ptwgame.entities.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StandingsCommon {
    public static final int NUM_PLAYERS_CHASE_ELIGIBLE = 30;

    public static final int NUM_PLAYERS_IN_CHASE   = 16;
    public static final int NUM_PLAYERS_IN_ROUND_2 = 12;
    public static final int NUM_PLAYERS_IN_ROUND_3 =  8;
    public static final int NUM_PLAYERS_IN_ROUND_4 =  4;

    // The 27th points race is the start of the Chase, but when counting
    // exhibition races, it is the 31st race of season.
    public static final int RACE_ID_CHASE_START = 31;
    public static final int RACE_ID_ROUND_2_START = RACE_ID_CHASE_START + 3;
    public static final int RACE_ID_ROUND_3_START = RACE_ID_CHASE_START + 6;
    public static final int RACE_ID_ROUND_4_START = RACE_ID_CHASE_START + 9;

    public static final int RACE_ID_ROUND_1_END = RACE_ID_ROUND_2_START - 1;
    public static final int RACE_ID_ROUND_2_END = RACE_ID_ROUND_3_START - 1;
    public static final int RACE_ID_ROUND_3_END = RACE_ID_ROUND_4_START - 1;
    public static final int RACE_ID_ROUND_4_END = RACE_ID_ROUND_4_START;

    public static final int CHASE_POINTS_BASE      = 5000;
    public static final int CHASE_POINTS_PER_ROUND = 1000;

    public static List<Player> getChasePlayers(final List<Player> standings) {
        final List<Player> chasePlayers = new ArrayList<>(standings);

        // Sanity check input for size to small or too large
        if (chasePlayers.size() <= 0) {
            return chasePlayers;
        }
        while (chasePlayers.size() > NUM_PLAYERS_CHASE_ELIGIBLE) {
            chasePlayers.remove(chasePlayers.size() - 1);
        }

        // Save current points leader
        final Player leader = chasePlayers.get(0);

        Collections.sort(chasePlayers, new Comparator<Player>() {
            @Override
            public int compare(final Player p1, final Player p2) {
                // Sort by wins descending
                int ret = p2.wins - p1.wins;
                if (ret != 0) {
                    return ret;
                }

                // Sort by points descending
                ret = p2.points - p1.points;
                if (ret != 0) {
                    return ret;
                }

                // Sort by races descending
                ret = p2.races - p1.races;
                if (ret != 0) {
                    return ret;
                }

                // Sort by rank ascending
                return p1.rank - p2.rank;
            }
        });

        if (chasePlayers.indexOf(leader) < NUM_PLAYERS_IN_CHASE) {
            while (chasePlayers.size() > NUM_PLAYERS_IN_CHASE) {
                chasePlayers.remove(chasePlayers.size() - 1);
            }
        } else {
            for (int i = NUM_PLAYERS_IN_CHASE - 1; i < chasePlayers.size(); i++) {
                if (!chasePlayers.get(i).equals(leader)) {
                    chasePlayers.remove(i);
                }
            }
        }

        return chasePlayers;
    }
}
