/*
 * Copyright (C) 2013 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.greg.ptw;

import com.google.gson.Gson;

public final class FriendRequest {
    protected final Player player;
    protected final String gcmRegId;

    public FriendRequest(final Player p, final String id) {
        player = p;
        gcmRegId = id;
    }

    public FriendRequest(final Player p) {
        this(p, null);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static FriendRequest fromJson(final String json) {
        return new Gson().fromJson(json, FriendRequest.class);
    }
}
