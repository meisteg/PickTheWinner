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

package com.meiste.greg.ptwgame.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.meiste.greg.ptwgame.entities.Player;
import com.meiste.greg.ptwgame.entities.RaceAnswers;
import com.meiste.greg.ptwgame.entities.RaceQuestions;

public class HistoryServlet extends HttpServlet {

    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final UserService userService = UserServiceFactory.getUserService();
        final User user = userService.getCurrentUser();

        if (user != null) {
            resp.setContentType("text/plain");
            resp.getWriter().print(new PlayerHistory(user).toJson());
        } else {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
        }
    }

    private class PlayerHistory {
        @Expose
        private List<Integer> ids = new ArrayList<>();
        @Expose
        private List<RaceQuestions> questions = new ArrayList<>();
        @Expose
        private List<RaceAnswers> answers;

        PlayerHistory(final User user) {
            final Player player = Player.getByUser(user);
            answers = RaceAnswers.getAllForUser(player);
            if ((answers != null) && (answers.size() > 0)) {
                for (final RaceAnswers a : answers) {
                    ids.add(a.getRace().raceId);
                    questions.add(RaceQuestions.get(a.getRace()));
                }
            }
        }

        public String toJson() {
            // Need to exclude fields without Expose for sub-classes
            final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
            return gson.toJson(this);
        }
    }
}
