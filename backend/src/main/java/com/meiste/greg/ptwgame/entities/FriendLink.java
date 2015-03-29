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

package com.meiste.greg.ptwgame.entities;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

import java.util.List;

import static com.meiste.greg.ptwgame.OfyService.ofy;

@Entity
@Index
@Cache
public class FriendLink {
    private static class LoadPlayer {}
    private static class LoadFriend {}

    @SuppressWarnings("unused")
    @Id
    private Long id;

    @Load(LoadPlayer.class)
    public Ref<Player> playerRef;

    @Load(LoadFriend.class)
    public Ref<Player> friendRef;

    public static FriendLink get(final Player p, final Player f) {
        return ofy().load().type(FriendLink.class)
                .filter("playerRef", p.getRef())
                .filter("friendRef", f.getRef())
                .first().now();
    }

    public static List<FriendLink> getByPlayer(final Player p) {
        return ofy().load().group(LoadFriend.class).type(FriendLink.class)
                .filter("playerRef", p.getRef()).list();
    }

    public static List<FriendLink> getByFriend(final Player f) {
        return ofy().load().group(LoadPlayer.class).type(FriendLink.class)
                .filter("friendRef", f.getRef()).list();
    }

    public static void put(final FriendLink flink) {
        ofy().save().entity(flink).now();
    }

    public static void del(final FriendLink flink) {
        ofy().delete().entity(flink).now();
    }

    @SuppressWarnings("unused")
    public FriendLink() {
        // Needed by objectify
    }

    public FriendLink(final Player p, final Player f) {
        playerRef = p.getRef();
        friendRef = f.getRef();
    }

    public Player getPlayer() {
        return playerRef.get();
    }

    public Player getFriend() {
        return friendRef.get();
    }
}
