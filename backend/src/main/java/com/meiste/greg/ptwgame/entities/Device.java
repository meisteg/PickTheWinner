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

import com.google.appengine.api.users.User;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.meiste.greg.ptwgame.OfyService.ofy;

@Entity
@Index
@Cache
public class Device {
    @Id
    public Long id;

    public String regId;
    public String userId;
    public long timestamp;
    public Ref<Player> playerRef;

    private static final Logger log = Logger.getLogger(Device.class.getName());
    private static final long DEVICE_REG_EXPIRATION = TimeUnit.DAYS.toMillis(180);

    public static Device getByRegId(final String regId) {
        return ofy().load().type(Device.class).filter("regId", regId).first().now();
    }

    public static synchronized void register(final String regId, final User user) {
        log.info("Registering " + regId);
        Device device = getByRegId(regId);
        if (device != null) {
            log.info(regId + " is already registered.");
        } else {
            device = new Device();
            device.regId = regId;
        }
        if (user != null) {
            final String userId = user.getUserId();
            if ((userId != null) && (!userId.isEmpty())) {
                device.userId = userId;
            }
            device.playerRef = Player.getByUser(user).getRef();
        }
        ofy().save().entity(device).now();
    }

    public static synchronized void unregister(final String regId) {
        log.info("Unregistering " + regId);
        final Device device = getByRegId(regId);
        if (device != null) {
            ofy().delete().entity(device).now();
        } else {
            log.warning("Device " + regId + " already unregistered");
        }
    }

    public static void updateRegistration(final String oldId, final String newId) {
        log.info("Updating " + oldId + " to " + newId);
        unregister(oldId);
        register(newId, null);
    }

    public static List<Device> getDevices() {
        return ofy().load().type(Device.class).list();
    }

    public static List<String> getRegIdsByPlayer(final Player player) {
        final List<String> regIds = new ArrayList<>();
        // TODO: Remove userId and use playerRef once all records have been updated
        final Iterable<Device> devices = ofy().load().type(Device.class)
                .filter("userId", player.mUserId).iterable();
        for (final Device device : devices) {
            regIds.add(device.regId);
        }
        return regIds;
    }

    public static List<String> cleanOld() {
        final List<String> regIds = new ArrayList<>();
        final long expiration = System.currentTimeMillis() - DEVICE_REG_EXPIRATION;

        log.info("Cleaning up registrations older than " + expiration);

        final List<Device> devices =
                ofy().load().type(Device.class).filter("timestamp <", expiration).list();
        for (final Device device : devices) {
            regIds.add(device.regId);
            ofy().delete().entity(device).now();
        }
        return regIds;
    }

    public Device() {
    }

    @SuppressWarnings("unused")
    @OnSave
    public void setTimestamp() {
        timestamp = System.currentTimeMillis();
    }
}
