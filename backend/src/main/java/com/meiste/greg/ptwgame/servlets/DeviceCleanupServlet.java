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

import com.meiste.greg.ptwgame.entities.Device;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DeviceCleanupServlet extends HttpServlet {

    private static final Logger logger =
            Logger.getLogger(DeviceCleanupServlet.class.getName());

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final List<String> devices = Device.cleanOld();
        final StringBuilder sb = new StringBuilder();
        for (final String device : devices) {
            sb.append(device).append("\n\n");
        }

        final Properties props = new Properties();
        final Session session = Session.getDefaultInstance(props, null);
        final String msgSubject = "Cleaned up " + devices.size() + " registrations";
        final String msgBody = sb.toString();
        final Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress("greg.meiste@gmail.com", "Pick The Winner"));
            msg.addRecipient(Message.RecipientType.TO,
                    new InternetAddress("greg.meiste@gmail.com", "Greg Meiste"));
            msg.setSubject(msgSubject);
            msg.setText(msgBody);
            Transport.send(msg);
        } catch (final MessagingException e) {
            logger.severe("MessagingException when trying to send email");
        }

        resp.setContentType("text/plain");
        resp.getWriter().print(msgSubject);
        resp.getWriter().print("\n\n");
        resp.getWriter().print(msgBody);
    }
}
