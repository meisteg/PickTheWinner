/*
 * Copyright (C) 2013-2015 Gregory S. Meiste  <http://gregmeiste.com>
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
import com.meiste.greg.ptwgame.FriendRequest;
import com.meiste.greg.ptwgame.entities.Device;
import com.meiste.greg.ptwgame.entities.FriendLink;
import com.meiste.greg.ptwgame.entities.Multicast;
import com.meiste.greg.ptwgame.entities.Player;

public class FriendServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(FriendServlet.class.getName());

    @Override
    public void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final UserService userService = UserServiceFactory.getUserService();
        final User user = userService.getCurrentUser();
        if (user == null) {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
            return;
        }

        final Player self = Player.getByUser(user);
        final String json = req.getReader().readLine();
        if ((self == null) || (json == null)) {
            resp.sendError(405);
            return;
        }

        final FriendRequest fReq = new Gson().fromJson(json, FriendRequest.class);
        if (!fReq.player.isIdentifiable()) {
            resp.sendError(405);
            return;
        }

        final Player other;
        if (fReq.player.rank != null)
            other = Player.getByRank(fReq.player.rank);
        else
            other = Player.getByName(fReq.player.name);

        if ((other == null) || other.mUserId.equals(user.getUserId())) {
            resp.sendError(405);
            return;
        }

        FriendLink fLinkDB = FriendLink.get(self, other);
        final List<String> deviceList = new ArrayList<>();
        if (fReq.player.friend) {
            if (fLinkDB == null) {
                FriendLink.put(new FriendLink(self, other));
                deviceList.addAll(getUserDevices(self));
            }
            // If GCM registration ID present, it indicates friend request
            // is the result of NFC. Need to friend in reverse as well, then
            // notify other player.
            if ((fReq.gcmRegId != null) && (Device.getByRegId(fReq.gcmRegId) != null)) {
                fLinkDB = FriendLink.get(other, self);
                if (fLinkDB == null) {
                    FriendLink.put(new FriendLink(other, self));
                    deviceList.addAll(getFriendDevices(other, fReq.gcmRegId));
                }
            }
        } else if (fLinkDB != null) {
            FriendLink.del(fLinkDB);
            deviceList.addAll(getUserDevices(self));
        }

        if (!deviceList.isEmpty()) {
            sendGcm(deviceList);
        }
    }

    private static List<String> getUserDevices(final Player player) {
        final List<String> deviceList = Device.getRegIdsByPlayer(player);
        if (deviceList.size() == 1) {
            // If just one device, don't need to ping it. The device already is aware
            // of the situation. Only need to ping when where are multiple devices.
            deviceList.clear();
        }
        return deviceList;
    }

    private static List<String> getFriendDevices(final Player player, final String regId) {
        final List<String> deviceList = Device.getRegIdsByPlayer(player);
        if (!deviceList.contains(regId)) {
            // This should not happen anymore, but leaving just in case.
            deviceList.add(regId);
        }
        return deviceList;
    }

    private static void sendGcm(final List<String> deviceList) {
        logger.info("Sending sync GCM to " + deviceList.size() + " devices");

        final String multicastKey = Multicast.create(deviceList);
        final TaskOptions taskOptions = TaskOptions.Builder
                .withUrl("/tasks/send")
                .param(SendMessageServlet.PARAMETER_MULTICAST, multicastKey)
                .method(Method.POST);
        QueueFactory.getDefaultQueue().add(taskOptions);
    }
}
