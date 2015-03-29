/*
 * Copyright 2012 Google Inc.
 * Copyright 2012-2015 Gregory S. Meiste  <http://gregmeiste.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meiste.greg.ptwgame.servlets;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.meiste.greg.ptwgame.ApiKeyInitializer;
import com.meiste.greg.ptwgame.entities.Device;
import com.meiste.greg.ptwgame.entities.Multicast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that sends a message to a device.
 * <p>
 * This servlet is invoked by AppEngine's Push Queue mechanism.
 */
public class SendMessageServlet extends GCMBaseServlet {

    private static final String HEADER_QUEUE_COUNT = "X-AppEngine-TaskRetryCount";
    private static final String HEADER_QUEUE_NAME = "X-AppEngine-QueueName";
    private static final int MAX_RETRY = 3;

    public static final String PARAMETER_MULTICAST = "multicastKey";
    public static final String PARAMETER_MSG_TYPE = "collapseKey";

    public static final String MSG_TYPE_SYNC = "ptw_sync";
    public static final String MSG_TYPE_HISTORY = "ptw_history";
    public static final String MSG_TYPE_RULES = "ptw_rules";

    private Sender sender;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        sender = newSender(config);
    }

    /**
     * Creates the {@link Sender} based on the servlet settings.
     */
    protected Sender newSender(final ServletConfig config) {
        final String key = (String) config.getServletContext()
                .getAttribute(ApiKeyInitializer.ATTRIBUTE_ACCESS_KEY);
        return new Sender(key);
    }

    /**
     * Indicates to App Engine that this task should be retried.
     */
    private void retryTask(final HttpServletResponse resp) {
        resp.setStatus(500);
    }

    /**
     * Indicates to App Engine that this task is done.
     */
    private void taskDone(final HttpServletResponse resp) {
        resp.setStatus(200);
    }

    /**
     * Processes the request to add a new message.
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        if (req.getHeader(HEADER_QUEUE_NAME) == null) {
            throw new IOException("Missing header " + HEADER_QUEUE_NAME);
        }

        final String retryCountHeader = req.getHeader(HEADER_QUEUE_COUNT);
        logger.fine("retry count: " + retryCountHeader);
        if (retryCountHeader != null) {
            final int retryCount = Integer.parseInt(retryCountHeader);
            if (retryCount > MAX_RETRY) {
                logger.severe("Too many retries, dropping task");
                taskDone(resp);
                return;
            }
        }

        final String multicastKey = req.getParameter(PARAMETER_MULTICAST);
        if (multicastKey != null) {
            final String collapseKey = getParameter(req, PARAMETER_MSG_TYPE, MSG_TYPE_SYNC);
            sendMulticastMessage(multicastKey, collapseKey, resp);
            return;
        }

        logger.severe("Invalid request!");
        taskDone(resp);
    }

    private void sendMulticastMessage(final String multicastKey, final String collapseKey,
            final HttpServletResponse resp) {
        // Recover registration ids from datastore
        final List<String> regIds = Multicast.getRegIds(multicastKey);
        final Message message = new Message.Builder().collapseKey(collapseKey).build();
        MulticastResult multicastResult;
        try {
            multicastResult = sender.sendNoRetry(message, regIds);
        } catch (final IOException e) {
            logger.log(Level.SEVERE, "Exception posting " + message, e);
            multicastDone(resp, multicastKey);
            return;
        }
        boolean allDone = true;
        // check if any registration id must be updated
        if (multicastResult.getCanonicalIds() != 0) {
            final List<Result> results = multicastResult.getResults();
            for (int i = 0; i < results.size(); i++) {
                final String canonicalRegId = results.get(i).getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    final String regId = regIds.get(i);
                    Device.updateRegistration(regId, canonicalRegId);
                }
            }
        }
        if (multicastResult.getFailure() != 0) {
            // there were failures, check if any could be retried
            final List<Result> results = multicastResult.getResults();
            final List<String> retriableRegIds = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                final String error = results.get(i).getErrorCodeName();
                if (error != null) {
                    final String regId = regIds.get(i);
                    logger.warning("Got error (" + error + ") for regId " + regId);
                    if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                        // application has been removed from device - unregister it
                        Device.unregister(regId);
                    }
                    if (error.equals(Constants.ERROR_UNAVAILABLE)) {
                        retriableRegIds.add(regId);
                    }
                }
            }
            if (!retriableRegIds.isEmpty()) {
                // update task
                Multicast.update(multicastKey, retriableRegIds);
                allDone = false;
                retryTask(resp);
            }
        }

        if (allDone) {
            multicastDone(resp, multicastKey);
        } else {
            retryTask(resp);
        }
    }

    private void multicastDone(final HttpServletResponse resp, final String multicastKey) {
        Multicast.delete(multicastKey);
        taskDone(resp);
    }
}
