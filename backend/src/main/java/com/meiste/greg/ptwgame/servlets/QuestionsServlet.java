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
import com.meiste.greg.ptwgame.entities.Device;
import com.meiste.greg.ptwgame.entities.Multicast;
import com.meiste.greg.ptwgame.entities.Player;
import com.meiste.greg.ptwgame.entities.Race;
import com.meiste.greg.ptwgame.entities.RaceAnswers;
import com.meiste.greg.ptwgame.entities.RaceQuestions;

public class QuestionsServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(QuestionsServlet.class.getName());

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final UserService userService = UserServiceFactory.getUserService();
        final User user = userService.getCurrentUser();

        if (user != null) {
            final Race race = Race.getNext(false, true);
            if ((race != null) && race.inProgress()) {
                RaceQuestions q = RaceQuestions.get(race);
                if (q == null) {
                    log.warning("No questions found for race! Using default questions.");
                    q = new RaceQuestions(race);
                    q.setDefaults();
                    RaceQuestions.put(q);
                }

                resp.setContentType("text/plain");
                resp.getWriter().print(q.toJson());
            } else {
                resp.sendError(405);
            }
        } else {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
        }
    }

    @Override
    public synchronized void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final UserService userService = UserServiceFactory.getUserService();
        final User user = userService.getCurrentUser();

        if (user != null) {
            final Player player = Player.getByUser(user);
            final Race race = Race.getNext(false, true);
            if ((player != null) && (race != null) && race.inProgress()) {
                RaceAnswers a = RaceAnswers.get(race, player);
                if (a == null) {
                    a = RaceAnswers.fromJson(req.getReader().readLine());
                    a.setPlayer(player);
                    a.setRace(race);
                    RaceAnswers.put(a);
                    sendGcm(player);
                }

                resp.setContentType("text/plain");
                resp.getWriter().print(a.toJson());
            } else {
                resp.sendError(405);
            }
        } else {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
        }
    }

    private void sendGcm(final Player player) {
        final List<String> deviceList = Device.getRegIdsByPlayer(player);

        // If just one device, don't need to ping it. The device already is aware
        // of the situation. Only need to ping when where are multiple devices.
        if (deviceList.size() > 1) {
            log.info("Sending history GCM to " + deviceList.size() + " devices");

            final String multicastKey = Multicast.create(deviceList);
            final TaskOptions taskOptions = TaskOptions.Builder
                    .withUrl("/tasks/send")
                    .param(SendMessageServlet.PARAMETER_MULTICAST, multicastKey)
                    .param(SendMessageServlet.PARAMETER_MSG_TYPE, SendMessageServlet.MSG_TYPE_HISTORY)
                    .method(Method.POST);
            QueueFactory.getDefaultQueue().add(taskOptions);
        }
    }
}
