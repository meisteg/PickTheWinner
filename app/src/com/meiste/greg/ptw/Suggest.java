/*
 * Copyright (C) 2012-2013 Gregory S. Meiste  <http://gregmeiste.com>
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

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.gson.Gson;
import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.ObservableScrollView.ScrollViewListener;

public final class Suggest extends TabFragment implements View.OnClickListener, ScrollViewListener, GaeListener {
    private EditText mQuestion;
    private int mScroll = 0;

    public static Suggest newInstance(final Context context) {
        final Suggest fragment = new Suggest();
        fragment.setTitle(context.getString(R.string.tab_suggest));

        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final Race race = Race.getNext(getActivity(), false, false);

        if (race == null) {
            return inflater.inflate(R.layout.suggest_no_race, container, false);
        }

        final View v = inflater.inflate(R.layout.suggest, container, false);

        final TextView track = (TextView) v.findViewById(R.id.racetrack);
        track.setText(race.getTrack(Race.NAME_LONG));

        final Button send = (Button) v.findViewById(R.id.send);
        send.setOnClickListener(this);
        send.setEnabled(false);

        mQuestion = (EditText) v.findViewById(R.id.question);
        mQuestion.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable s) {
                if (s.toString().trim().length() >= 20)
                    send.setEnabled(true);
                else
                    send.setEnabled(false);
            }

            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {}
        });
        mQuestion.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (send.isEnabled())
                        send.performClick();
                    return true;
                }
                return false;
            }
        });

        final ObservableScrollView sv = (ObservableScrollView) v.findViewById(R.id.scroll_suggest);
        sv.postScrollTo(0, mScroll);
        sv.setScrollViewListener(this);

        return v;
    }

    @Override
    public void onClick(final View v) {
        if (ActivityManager.isUserAMonkey()) {
            Util.log("Suggest: onClick: User is a monkey!");
        } else {
            final String json = new Gson().toJson(mQuestion.getText().toString().trim());
            GAE.getInstance(getActivity()).postPage(this, "suggest", json);
        }

        Toast.makeText(getActivity(), R.string.suggest_success, Toast.LENGTH_SHORT).show();
        mQuestion.setText("");

        EasyTracker.getTracker().sendEvent("Suggest", "button", "send", (long) 0);
    }

    @Override
    public void onScrollChanged(final ObservableScrollView sv, final int x, final int y,
            final int oldx, final int oldy) {
        mScroll = y;
    }

    @Override
    public void onFailedConnect(final Context context) {
        Util.log("Suggest: onFailedConnect");
    }

    @Override
    public void onConnectSuccess(final Context context, final String json) {
        Util.log("Suggest: onConnectSuccess");
    }

    @Override
    public void onLaunchIntent(final Intent launch) {}

    @Override
    public void onGet(final Context context, final String json) {}
}
