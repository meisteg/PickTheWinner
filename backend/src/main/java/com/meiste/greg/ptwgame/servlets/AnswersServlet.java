/*
 * Copyright (C) 2012, 2014 Gregory S. Meiste  <http://gregmeiste.com>
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

import com.meiste.greg.ptwgame.entities.RaceCorrectAnswers;

public class AnswersServlet extends HttpServlet {

    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final String year = req.getParameter("year");
        final String race_id = req.getParameter("race_id");
        if ((year != null) && (race_id != null)) {
            final int y = Integer.parseInt(year);
            final int r = Integer.parseInt(race_id);
            final RaceCorrectAnswers a = RaceCorrectAnswers.get(y, r);
            if (a != null) {
                resp.setContentType("text/plain");
                resp.getWriter().print(a.toJson());
            } else {
                resp.sendError(405);
            }
        } else {
            resp.sendError(405);
        }
    }
}
