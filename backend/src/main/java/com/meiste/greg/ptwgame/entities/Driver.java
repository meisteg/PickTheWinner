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
package com.meiste.greg.ptwgame.entities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.List;

import static com.meiste.greg.ptwgame.OfyService.ofy;

@Entity
@Cache
public final class Driver {
    @Id
    public Long id;

    @Index
    @Expose
    @SerializedName("mNumber")
    public int number;

    @Index
    @Expose
    @SerializedName("mFirstName")
    public String firstName;

    @Index
    @Expose
    @SerializedName("mLastName")
    public String lastName;

    public static Ref<Driver> getRef(final long id) {
        return Ref.create(Key.create(Driver.class, id));
    }

    public static List<Driver> getAll() {
        return ofy().load().type(Driver.class).order("lastName").order("firstName").list();
    }

    public static boolean put(final Driver driver) {
        if (driver.id == null) {
            // When adding a new driver, verify the driver number is unique
            final Driver existing = ofy().load().type(Driver.class)
                    .filter("number", driver.number).first().now();
            if (existing != null) {
                return false;
            }
        }
        ofy().save().entity(driver).now();
        return true;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    @Override
    public boolean equals(final Object o) {
        if (o != null && o instanceof Driver) {
            final Driver d = (Driver) o;
            return (number == d.number) &&
                    firstName.equals(d.firstName) &&
                    lastName.equals(d.lastName);
        }
        return false;
    }
}
