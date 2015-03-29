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

package com.meiste.greg.ptwgame.entities;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static com.meiste.greg.ptwgame.OfyService.ofy;

@Entity
@Cache
public class Multicast {

    private static final Logger log = Logger.getLogger(Multicast.class.getName());

    @Id
    public Long id;

    public List<String> regIds;

    /**
     * Creates a persistent record with the devices to be notified using a
     * multicast message.
     *
     * @param devices registration ids of the devices.
     * @return ID of the persistent record.
     */
    public static String create(final List<String> devices) {
        log.info("Storing multicast for " + devices.size() + " devices");

        final Multicast multicast = new Multicast(devices);
        ofy().save().entity(multicast).now();

        return Long.toString(multicast.id);
    }

    /**
     * Gets a persistent record with the devices to be notified using a
     * multicast message.
     *
     * @param id ID of the persistent record.
     */
    public static List<String> getRegIds(final String id) {
        final Multicast multicast = ofy().load().type(Multicast.class).id(Long.parseLong(id)).now();
        if (multicast != null) {
            return multicast.regIds;
        }

        log.severe("No multicast for id " + id);
        return Collections.emptyList();
    }

    /**
     * Updates a persistent record with the devices to be notified using a
     * multicast message.
     *
     * @param id ID of the persistent record.
     * @param devices new list of registration ids of the devices.
     */
    public static void update(final String id, final List<String> devices) {
        final Multicast multicast = ofy().load().type(Multicast.class).id(Long.parseLong(id)).now();
        if (multicast != null) {
            multicast.regIds = devices;
            ofy().save().entity(multicast).now();
        } else {
            log.severe("No multicast for id " + id);
        }
    }

    /**
     * Deletes a persistent record with the devices to be notified using a
     * multicast message.
     *
     * @param id ID of the persistent record.
     */
    public static void delete(final String id) {
        ofy().delete().type(Multicast.class).id(Long.parseLong(id));
    }

    @SuppressWarnings("unused")
    private Multicast() {
        // Needed by Objectify
    }

    private Multicast(final List<String> devices) {
        regIds = devices;
    }
}
