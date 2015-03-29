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

package com.meiste.greg.ptwgame.servlets;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.meiste.greg.ptwgame.StandingsCommon;
import com.meiste.greg.ptwgame.entities.Device;
import com.meiste.greg.ptwgame.entities.FriendLink;
import com.meiste.greg.ptwgame.entities.Multicast;
import com.meiste.greg.ptwgame.entities.Player;
import com.meiste.greg.ptwgame.entities.RaceCorrectAnswers;

public class StandingsServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(StandingsServlet.class.getName());

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final UserService userService = UserServiceFactory.getUserService();
        final User user = userService.getCurrentUser();

        if (user != null) {
            resp.setContentType("text/plain");
            resp.getWriter().print(new Standings(user).toJson());
        } else {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
        }
    }

    @Override
    public void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final UserService userService = UserServiceFactory.getUserService();
        final User user = userService.getCurrentUser();

        if (user != null) {
            final Player self = Player.getByUser(user);
            final String json = req.getReader().readLine();
            String newName = null;

            if (json != null)
                newName = new Gson().fromJson(json, String.class);

            // Limit the input to 5-20 characters. Done here in addition to the
            // app because someone can and will try to submit without the app.
            if (newName != null)
                newName = newName.substring(0, Math.min(20, newName.length()));
            if ((newName != null) && (newName.length() < 5))
                newName = self.name;

            if (newName != null) {
                // No special characters allowed
                for (int i = 0; i < newName.length(); i++) {
                    if (!Character.isLetterOrDigit(newName.charAt(i))) {
                        newName = self.name;
                        break;
                    }
                }
            }

            if (newName != null) {
                // Verify name not taken by someone else
                final Player other = Player.getByName(newName);
                if ((other != null) && !other.mUserId.equals(self.mUserId)) {
                    newName = self.name;
                }
            }

            self.name = newName;
            Player.put(self, true);
            log.info(user.getEmail() + " changed player name to " + newName);
            sendGcm(self);

            doGet(req, resp);
        } else {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
        }
    }

    private void sendGcm(final Player player) {
        final List<String> deviceList = Device.getRegIdsByPlayer(player);
        if (deviceList.size() == 1) {
            // If just one device, don't need to ping it. The device already is aware
            // of the situation. Only need to ping when where are multiple devices.
            deviceList.clear();
        }

        // Also send sync GCM to players who are friends with this player
        final List<FriendLink> fLinks = FriendLink.getByFriend(player);
        for (final FriendLink fLink : fLinks) {
            deviceList.addAll(Device.getRegIdsByPlayer(fLink.getPlayer()));
        }

        if (!deviceList.isEmpty()) {
            log.info("Sending sync GCM to " + deviceList.size() + " devices");

            final String multicastKey = Multicast.create(deviceList);
            final TaskOptions taskOptions = TaskOptions.Builder
                    .withUrl("/tasks/send")
                    .param(SendMessageServlet.PARAMETER_MULTICAST, multicastKey)
                    .method(Method.POST);
            QueueFactory.getDefaultQueue().add(taskOptions);
        }
    }

    private class Standings {
        private static final int STANDINGS_NUM_TO_SHOW_BEFORE_CHASE =
                StandingsCommon.NUM_PLAYERS_CHASE_ELIGIBLE + 5;
        private static final int STANDINGS_NUM_TO_SHOW_DURING_CHASE = 25;

        @Expose
        private int race_id = 0;
        @Expose
        private final List<Player> standings;
        @Expose
        private Player self;

        public Standings(final User user) {
            final RaceCorrectAnswers a = RaceCorrectAnswers.get();
            if (a != null) {
                race_id = a.mRaceId;
            }

            self = Player.getByUser(user);

            int numToShow;
            if (race_id >= StandingsCommon.RACE_ID_CHASE_START) {
                numToShow = STANDINGS_NUM_TO_SHOW_DURING_CHASE;
                standings = Player.getList(numToShow);

                int numPlayersInChase = StandingsCommon.NUM_PLAYERS_IN_CHASE;
                if (race_id >= StandingsCommon.RACE_ID_ROUND_1_END)
                    numPlayersInChase = StandingsCommon.NUM_PLAYERS_IN_ROUND_2;
                if (race_id >= StandingsCommon.RACE_ID_ROUND_2_END)
                    numPlayersInChase = StandingsCommon.NUM_PLAYERS_IN_ROUND_3;
                if (race_id >= StandingsCommon.RACE_ID_ROUND_3_END)
                    numPlayersInChase = StandingsCommon.NUM_PLAYERS_IN_ROUND_4;
                if (race_id >= StandingsCommon.RACE_ID_ROUND_4_END)
                    numPlayersInChase = 1; /* Champion */

                for (final Player p : standings) {
                    if (p.rank <= numPlayersInChase) {
                        if (p.rank.equals(self.rank)) {
                            self.inChase = true;
                        }
                        p.inChase = true;
                    } else {
                        break;
                    }
                }
            } else {
                numToShow = STANDINGS_NUM_TO_SHOW_BEFORE_CHASE;
                standings = Player.getList(numToShow);

                final List<Player> chasePlayers = StandingsCommon.getChasePlayers(standings);
                for (final Player p : chasePlayers) {
                    if (p.rank.equals(self.rank)) {
                        self.inChase = true;
                    }
                    p.inChase = true;
                }
            }

            final List<FriendLink> fLinks = FriendLink.getByPlayer(self);
            for (final FriendLink fLink : fLinks) {
                final Player friend = fLink.getFriend();
                if (friend == null) {
                    FriendLink.del(fLink);
                    continue;
                }

                if (friend.rank != null) {
                    if (friend.rank <= numToShow) {
                        standings.get(friend.rank - 1).friend = true;
                    } else {
                        friend.friend = true;
                        int i;
                        for (i = numToShow; i < standings.size(); ++i) {
                            if ((standings.get(i).rank == null) || (standings.get(i).rank > friend.rank)) {
                                standings.add(i, friend);
                                break;
                            }
                        }
                        if (i >= standings.size()) {
                            standings.add(friend);
                        }
                    }
                } else {
                    friend.friend = true;
                    standings.add(friend);
                }
            }
        }

        public String toJson() {
            // Need to exclude fields without Expose for Player class
            final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
            return gson.toJson(this);
        }
    }
}
