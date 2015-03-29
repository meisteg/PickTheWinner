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

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.meiste.greg.ptwgame.entities.Race;
import com.meiste.greg.ptwgame.entities.Track;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ScheduleServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(ScheduleServlet.class.getName());
    private static final int NUM_RACES_IN_SEASON = 41;

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // Limit is here so an admin could start entering races for the next season without
        // them showing up on player devices.
        final List<Race> races = Race.getList(NUM_RACES_IN_SEASON);
        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        resp.setContentType("text/plain");
        resp.getWriter().print(gson.toJson(races));
    }

    @Override
    public void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final UserService userService = UserServiceFactory.getUserService();
        if (userService.isUserAdmin()) {
            switch (req.getParameter("op")) {
                case "add_race":
                    addRace(req);
                    break;
                case "edit_race":
                    editRace(req);
                    break;
                case "add_track":
                    addTrack(req);
                    break;
                case "upload_logo":
                    uploadLogo(req);
                    break;
            }
            resp.sendRedirect("/schedule.jsp");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private void addRace(final HttpServletRequest req) {
        final Race race = new Race();
        if (postRace(req, race)) {
            log.info("Added: " + race.name);
        }
    }

    private void editRace(final HttpServletRequest req) {
        final Race race = Race.get(Long.parseLong(req.getParameter("entityId")));
        if (race == null) {
            log.severe("Failed to edit race: Race not found!");
            return;
        }

        if (postRace(req, race)) {
            log.info("Edited: " + race.name);
        }
    }

    private boolean postRace(final HttpServletRequest req, final Race race) {
        final String errMsg = "Failed to " + (race.id != null ? "edit" : "add") + " race: ";
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        final Track track = Track.get(Long.parseLong(req.getParameter("track")));
        if (track == null) {
            log.severe(errMsg + "Track not found!");
            return false;
        }

        race.setTrack(track);
        race.raceId = Integer.parseInt(req.getParameter("raceId"));
        race.raceNum = Integer.parseInt(req.getParameter("raceNum"));
        race.name = req.getParameter("name");
        race.tv = req.getParameter("tv");
        try {
            race.startTime = sdf.parse(req.getParameter("startTime")).getTime();
            race.questionTime = sdf.parse(req.getParameter("questionTime")).getTime();
            Race.put(race);
        } catch (final ParseException e) {
            log.severe(errMsg + e);
            return false;
        }

        return true;
    }

    private void addTrack(final HttpServletRequest req) {
        final Track track = new Track();
        track.longName  = req.getParameter("longName");
        track.shortName = req.getParameter("shortName");
        track.length    = req.getParameter("length");
        track.layout    = req.getParameter("layout");
        track.city      = req.getParameter("city");
        track.state     = req.getParameter("state");
        Track.put(track);

        log.info(track.longName + " added");
    }

    private void uploadLogo(final HttpServletRequest req) {
        final BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
        final Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(req);
        final List<BlobKey> blobKeys = blobs.get("logo");

        String blobKey = null;
        if (blobKeys == null || blobKeys.isEmpty()) {
            log.warning("Blob missing");
        } else {
            blobKey = blobKeys.get(0).getKeyString();
        }

        final Race race = Race.get(Long.parseLong(req.getParameter("race_id")));
        if (race == null) {
            log.severe("Race not found!");

            // Cleanup orphans
            if (blobKey != null) {
                blobstoreService.delete(new BlobKey(blobKey));
            }
            return;
        }

        if ((race.logoBlobKey != null) && !race.logoBlobKey.isEmpty()) {
            log.warning("Removing existing logo");
            blobstoreService.delete(new BlobKey(race.logoBlobKey));
        }

        race.logoBlobKey = blobKey;
        Race.put(race);
    }
}
