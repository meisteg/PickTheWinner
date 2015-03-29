/*
 * Copyright (C) 2012-2013, 2015 Gregory S. Meiste  <http://gregmeiste.com>
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
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;

import java.util.List;

import static com.meiste.greg.ptwgame.OfyService.ofy;

@Entity
@Cache
public final class Race {

    @Id
    public Long id;

    @Index
    @Expose
    @SerializedName("mId")
    public int raceId;

    @Index
    @Expose
    @SerializedName("mRaceNum")
    public int raceNum;

    @Expose
    @SerializedName("mName")
    public String name;

    @SuppressWarnings("unused")
    @Expose
    @SerializedName("mTv")
    public String tv;

    @Index
    @Expose
    @SerializedName("mStart")
    public long startTime;

    @Index
    @Expose
    @SerializedName("mQuestion")
    public long questionTime;

    @Load
    public Ref<Track> trackRef;

    public String logoBlobKey;

    @Ignore
    @Expose
    @SerializedName("mTrackLong")
    public String trackNameLong;

    @Ignore
    @Expose
    @SerializedName("mTrackShort")
    public String trackNameShort;

    @Ignore
    @Expose
    @SerializedName("mSize")
    public String trackLength;

    @Ignore
    @Expose
    @SerializedName("mLayout")
    public String trackLayout;

    @Ignore
    @Expose
    @SerializedName("mCityState")
    public String trackCityState;

    public Race() {
        // Do nothing
    }

    @SuppressWarnings("unused")
    @OnLoad
    private void onLoad() {
        final Track track = getTrack();
        trackNameLong = track.longName;
        trackNameShort = track.shortName;
        trackLength = track.length;
        trackLayout = track.layout;
        trackCityState = track.city + ", " + track.state;
    }

    public void setTrack(final Track track) {
        trackRef = Ref.create(track);
    }

    public Track getTrack() {
        return trackRef.get();
    }

    public boolean isFuture() {
        return System.currentTimeMillis() < startTime;
    }

    public boolean inProgress() {
        return (questionTime < System.currentTimeMillis()) && isFuture();
    }

    public boolean isExhibition() {
        return raceNum <= 0;
    }

    public Ref<Race> getRef() {
        return Ref.create(this);
    }

    public static Race getNext(final boolean allowExhibition, final boolean allowInProgress) {
        // Only one inequality filter per query is supported on the datastore. :(
        // Because of this, looping through the results to find match is required.
        // Limit is used to keep datastore accesses to a minimum. Currently, the most exhibition
        // races in a row in a season is 3, so use limit of 4 to cover case where allowExhibition
        // is false.
        final List<Race> races = ofy().load().type(Race.class)
                .filter("startTime >", System.currentTimeMillis())
                .order("startTime").limit(4).list();
        for (final Race race : races) {
            if (!allowExhibition && race.isExhibition()) {
                continue;
            } else if (!allowInProgress && race.inProgress()) {
                continue;
            }
            return race;
        }
        return null;
    }

    public static Race get(final long entityId) {
        return ofy().load().type(Race.class).id(entityId).now();
    }

    /**
     * Get a race by its race ID.
     *
     * This method should be avoided if possible. It is a legacy function to support old
     * implementations. Race IDs are not unique, so this function will return the earliest
     * race with the requested ID. Please use {@link #get(long)} instead.
     *
     * @param raceId ID fo race to fetch
     * @return Earliest Race with requested ID, or null if none found.
     */
    public static Race get(final int raceId) {
        return ofy().load().type(Race.class).filter("raceId", raceId).order("startTime").first().now();
    }

    // Used by admin.jsp
    @SuppressWarnings("unused")
    public static Race getPrev(final Race race) {
        // See comment in getNext() above for why this looping logic is required.
        final List<Race> races = ofy().load().type(Race.class)
                .filter("startTime <", race.startTime)
                .order("-startTime").limit(3).list();
        for (final Race r : races) {
            // Unlike getNext() above, this function always ignores exhibition races.
            if (r.isExhibition()) {
                continue;
            }
            return r;
        }
        return null;
    }

    public static List<Race> getList(final int limit) {
        return ofy().load().type(Race.class).order("startTime").limit(limit).list();
    }

    public static void put(final Race race) {
        ofy().save().entity(race).now();
    }
}
