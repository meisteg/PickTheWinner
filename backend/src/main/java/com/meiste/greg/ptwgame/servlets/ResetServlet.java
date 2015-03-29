/*
 * Copyright (C) 2015 Gregory S. Meiste  <http://gregmeiste.com>
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

import com.meiste.greg.ptwgame.entities.Player;
import com.meiste.greg.ptwgame.entities.RaceQuestions;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResetServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(ResetServlet.class.getName());

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // Check if reset is allowed. Do not allow reset in middle of the season!
        if (RaceQuestions.getAll().size() > 0) {
            log.severe("Cannot reset game during the season!");
            return;
        }

        final List<Player> players = Player.getForRanking();
        for (final Player player : players) {
            player.reset();
        }
    }
}
