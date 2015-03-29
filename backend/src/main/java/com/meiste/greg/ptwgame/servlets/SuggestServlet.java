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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.meiste.greg.ptwgame.entities.Race;
import com.meiste.greg.ptwgame.entities.Suggestion;

public class SuggestServlet extends HttpServlet {

    public void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final Race race = Race.getNext(false, false);

        if (race != null) {
            final UserService userService = UserServiceFactory.getUserService();
            final User user = userService.getCurrentUser();
            final String json = req.getReader().readLine();

            Suggestion.put(new Suggestion(race, user, json));
            resp.setContentType("text/plain");
            resp.getWriter().print(json);
        } else {
            resp.sendError(405);
        }
    }
}
