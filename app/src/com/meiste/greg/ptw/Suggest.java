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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.ObservableScrollView.ScrollViewListener;

public final class Suggest extends TabFragment implements View.OnClickListener, ScrollViewListener, GaeListener {
    private EditText mQuestion;
    private int mScroll = 0;

    public static Suggest newInstance(Context context) {
        Suggest fragment = new Suggest();
        fragment.setTitle(context.getString(R.string.tab_suggest));

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Race race = Race.getNext(getActivity(), false, false);

        if (race == null) {
            return inflater.inflate(R.layout.suggest_no_race, container, false);
        }

        View v = inflater.inflate(R.layout.suggest, container, false);

        TextView track = (TextView) v.findViewById(R.id.racetrack);
        track.setText(race.getTrack(Race.NAME_LONG));

        final Button send = (Button) v.findViewById(R.id.send);
        send.setOnClickListener(this);
        send.setEnabled(false);

        mQuestion = (EditText) v.findViewById(R.id.question);
        mQuestion.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() >= 20)
                    send.setEnabled(true);
                else
                    send.setEnabled(false);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        ObservableScrollView sv = (ObservableScrollView) v.findViewById(R.id.scroll_suggest);
        sv.postScrollTo(0, mScroll);
        sv.setScrollViewListener(this);

        return v;
    }

    @Override
    public void onClick(View v) {
        String json = new Gson().toJson(mQuestion.getText().toString().trim());
        GAE.getInstance(getActivity()).postPage(this, "suggest", json);

        Toast.makeText(getActivity(), R.string.suggest_success, Toast.LENGTH_SHORT).show();
        mQuestion.setText("");
    }

    @Override
    public void onScrollChanged(ObservableScrollView sv, int x, int y, int oldx, int oldy) {
        mScroll = y;
    }

    @Override
    public void onFailedConnect() {
        Util.log("Suggest: onFailedConnect");
    }

    @Override
    public void onConnectSuccess(Context context, String json) {
        Util.log("Suggest: onConnectSuccess");
    }

    @Override
    public void onLaunchIntent(Intent launch) {}

    @Override
    public void onGet(Context context, String json) {}
}
