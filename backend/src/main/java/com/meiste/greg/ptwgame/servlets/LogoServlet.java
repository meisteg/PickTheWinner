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
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.meiste.greg.ptwgame.entities.Race;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LogoServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(LogoServlet.class.getName());

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        String path = req.getPathInfo();
        if (path == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Strip leading slash, then check for empty path or extra directories and length
        path = path.substring(1);
        if (path.isEmpty() || (path.indexOf('/') >= 0) ||
                (path.length() > 6) || (path.length() < 5) || !path.endsWith(".png")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            final int raceId = Integer.parseInt(path.substring(0, path.indexOf(".png")));
            log.fine("Race ID = " + raceId);

            final Race race = Race.get(raceId);
            if ((race != null) && (race.logoBlobKey != null) && !race.logoBlobKey.isEmpty()) {
                final BlobKey blobKey = new BlobKey(race.logoBlobKey);
                resp.setHeader("Cache-Control", "max-age=" + 604800); // 7 days
                BlobstoreServiceFactory.getBlobstoreService().serve(blobKey, resp);
            } else {
                resp.sendRedirect("/img/default_logo.png");
            }
        } catch (final NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
