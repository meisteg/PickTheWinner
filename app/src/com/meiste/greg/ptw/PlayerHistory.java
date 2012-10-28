/*
 * Copyright (C) 2012 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.gson.Gson;

public final class PlayerHistory {
    private List<Integer> ids;
    private List<RaceQuestions> questions;
    private List<RaceAnswers> answers;

    public static PlayerHistory fromJson(String json) {
        return new Gson().fromJson(json, PlayerHistory.class);
    }

    public void commit(Context context) {
        if ((ids == null) || (questions == null) || (answers == null))
            return;
        if ((ids.size() != questions.size()) && (ids.size() != answers.size()))
            return;
        if (ids.size() <= 0)
            return;

        Util.log("PlayerHistory: Restoring " + ids.size() + " races for player.");

        SharedPreferences qcache = context.getSharedPreferences(Questions.QCACHE, Activity.MODE_PRIVATE);
        SharedPreferences acache = context.getSharedPreferences(Questions.ACACHE, Activity.MODE_PRIVATE);

        Editor qeditor = qcache.edit();
        Editor aeditor = acache.edit();

        for (int i = 0; i < ids.size(); i++) {
            qeditor.putString(Questions.CACHE_PREFIX + ids.get(i), questions.get(i).toJson());
            aeditor.putString(Questions.CACHE_PREFIX + ids.get(i), answers.get(i).toJson());
        }

        qeditor.commit();
        aeditor.commit();
    }
}
