/*
 * Copyright 2012 Google Inc.
 * Copyright 2012, 2014-2015 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.meiste.greg.ptwgame.entities.Device;
import com.meiste.greg.ptwgame.entities.Multicast;

/**
 * Servlet that adds a new message to all registered devices.
 * <p>
 * This servlet is used just by the browser (i.e., not device).
 */
public class PushServlet extends GCMBaseServlet {

    public static final String PARAMETER_MSG_TYPE = "msgType";

    private static final int MULTICAST_SIZE = 1000;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException, ServletException {
        final String msgType = getParameter(req, PARAMETER_MSG_TYPE,
                SendMessageServlet.MSG_TYPE_SYNC);
        final List<Device> devices = Device.getDevices();
        String status;

        if (devices.isEmpty()) {
            status = "Message " + msgType + " ignored as there are no devices registered!";
        } else {
            final Queue queue = QueueFactory.getDefaultQueue();

            // must split in chunks of MULTICAST_SIZE devices (GCM limit)
            final int total = devices.size();
            final List<String> partialDevices = new ArrayList<>(total);
            int counter = 0;
            int tasks = 0;
            for (final Device device : devices) {
                counter++;
                partialDevices.add(device.regId);
                final int partialSize = partialDevices.size();
                if (partialSize == MULTICAST_SIZE || counter == total) {
                    final String multicastKey = Multicast.create(partialDevices);
                    logger.fine("Queuing " + partialSize + " devices on multicast " +
                            multicastKey);
                    final TaskOptions taskOptions = TaskOptions.Builder
                            .withUrl("/tasks/send")
                            .param(SendMessageServlet.PARAMETER_MULTICAST, multicastKey)
                            .param(SendMessageServlet.PARAMETER_MSG_TYPE, msgType)
                            .method(Method.POST);
                    queue.add(taskOptions);
                    partialDevices.clear();
                    tasks++;
                }
            }
            status = "Sending " + tasks + " multicast messages of type " +
                    msgType + " to " + total + " devices";
        }

        resp.setContentType("text/plain");
        resp.getWriter().print(status);
    }
}
