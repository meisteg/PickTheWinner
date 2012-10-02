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

import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.ObservableScrollView.ScrollViewListener;
import com.squareup.otto.Subscribe;

public final class Questions extends TabFragment implements View.OnClickListener, ScrollViewListener, GaeListener {

    public final static String QCACHE = "question_cache";
    public final static String ACACHE = "answer_cache";
    public final static String CACHE_PREFIX = Calendar.getInstance().get(Calendar.YEAR) + "_race";

    @SuppressWarnings("unused")
    @Expose
    @SerializedName("a1")
    private int mWinner;

    @SuppressWarnings("unused")
    @Expose
    @SerializedName("a2")
    private int mA2;

    @SuppressWarnings("unused")
    @Expose
    @SerializedName("a3")
    private int mA3;

    @SuppressWarnings("unused")
    @Expose
    @SerializedName("a4")
    private int mMostLaps;

    @SuppressWarnings("unused")
    @Expose
    @SerializedName("a5")
    private int mNumLeaders;

    private int mScroll = 0;
    private boolean mSetupNeeded;
    private boolean mChanged = false;
    private Race mRaceNext;
    private Race mRaceSelected = null;
    private boolean mFailedConnect = false;
    private boolean mSending = false;
    private long mOnCreateViewTime = 0;
    private RaceAnswers mRa;

    public static Questions newInstance(Context context) {
        Questions fragment = new Questions();
        fragment.setTitle(context.getString(R.string.tab_questions));

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v;
        mRaceNext = Race.getNext(getActivity(), false, true);
        mSetupNeeded = GAE.isAccountSetupNeeded(getActivity());
        mChanged = false;
        mRa = null;
        QuestionsRaceAdapter raceAdapter = new QuestionsRaceAdapter(getActivity());
        mOnCreateViewTime = System.currentTimeMillis();
        setRetainInstance(true);

        if (mRaceSelected == null) {
            if (mRaceNext != null) {
                mRaceSelected = mRaceNext;
            } else if (raceAdapter.getCount() > 0) {
                mRaceSelected = raceAdapter.getItem(raceAdapter.getCount()-1);
            }
        }

        if (mRaceSelected == null) {
            return inflater.inflate(R.layout.questions_no_race, container, false);
        } else if (mSetupNeeded) {
            return inflater.inflate(R.layout.no_account, container, false);
        } else if (mSending) {
            return inflater.inflate(R.layout.connecting, container, false);
        } else if (mRaceSelected.inProgress() || !mRaceSelected.isFuture()) {
            if (mFailedConnect) {
                mFailedConnect = false;
                v = inflater.inflate(R.layout.no_connection, container, false);

                Button retry = (Button) v.findViewById(R.id.retry);
                retry.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mChanged = true;
                        notifyChanged();
                    }
                });

                return v;
            }

            SharedPreferences cache = getActivity().getSharedPreferences(QCACHE, Activity.MODE_PRIVATE);
            String json = cache.getString(CACHE_PREFIX + mRaceSelected.getId(), null);
            if (json == null) {
                GAE.getInstance(getActivity()).getPage(this, "questions");
                return inflater.inflate(R.layout.connecting, container, false);
            }

            RaceQuestions rq = RaceQuestions.fromJson(json);

            cache = getActivity().getSharedPreferences(ACACHE, Activity.MODE_PRIVATE);
            json = cache.getString(CACHE_PREFIX + mRaceSelected.getId(), null);
            if (json == null) {
                Util.log("Questions: Showing form");

                v = inflater.inflate(R.layout.questions, container, false);

                Spinner winner = (Spinner) v.findViewById(R.id.winner);
                winner.setAdapter(new DriverAdapter(getActivity()));
                winner.setOnItemSelectedListener(new WinnerSelectedListener());

                Spinner a2 = (Spinner) v.findViewById(R.id.question2a);
                ArrayAdapter<CharSequence> a2_adapter = new ArrayAdapter<CharSequence>(
                        getActivity(), android.R.layout.simple_spinner_item, rq.a2);
                a2_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                a2.setAdapter(a2_adapter);
                a2.setOnItemSelectedListener(new A2SelectedListener());

                Spinner a3 = (Spinner) v.findViewById(R.id.question3a);
                ArrayAdapter<CharSequence> a3_adapter = new ArrayAdapter<CharSequence>(
                        getActivity(), android.R.layout.simple_spinner_item, rq.a3);
                a3_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                a3.setAdapter(a3_adapter);
                a3.setOnItemSelectedListener(new A3SelectedListener());

                Spinner mostlaps = (Spinner) v.findViewById(R.id.mostlaps);
                mostlaps.setAdapter(new DriverAdapter(getActivity()));
                mostlaps.setOnItemSelectedListener(new MostLapsSelectedListener());

                Spinner numleaders = (Spinner) v.findViewById(R.id.numleaders);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                        getActivity(), R.array.num_leaders, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                numleaders.setAdapter(adapter);
                numleaders.setOnItemSelectedListener(new NumLeadersSelectedListener());

                Button send = (Button) v.findViewById(R.id.send);
                send.setOnClickListener(this);
            } else {
                Util.log("Questions: Showing submitted answers");

                v = inflater.inflate(R.layout.questions_answered, container, false);
                Resources res = getActivity().getResources();
                mRa = RaceAnswers.fromJson(json);

                TextView a1 = (TextView) v.findViewById(R.id.answer1);
                a1.setText(Driver.newInstance(res, mRa.a1).getName());

                TextView a2 = (TextView) v.findViewById(R.id.answer2);
                a2.setText(rq.a2[mRa.a2]);

                TextView a3 = (TextView) v.findViewById(R.id.answer3);
                a3.setText(rq.a3[mRa.a3]);

                TextView a4 = (TextView) v.findViewById(R.id.answer4);
                a4.setText(Driver.newInstance(res, mRa.a4).getName());

                TextView a5 = (TextView) v.findViewById(R.id.answer5);
                a5.setText(res.getStringArray(R.array.num_leaders)[mRa.a5]);
            }

            TextView q2 = (TextView) v.findViewById(R.id.question2);
            q2.setText(getActivity().getString(R.string.questions_2, rq.q2));

            TextView q3 = (TextView) v.findViewById(R.id.question3);
            q3.setText(getActivity().getString(R.string.questions_3, rq.q3));

            ObservableScrollView sv = (ObservableScrollView) v.findViewById(R.id.scroll_questions);
            sv.postScrollTo(0, mScroll);
            sv.setScrollViewListener(this);
        } else {
            v = inflater.inflate(R.layout.questions_not_yet, container, false);

            TextView time = (TextView) v.findViewById(R.id.questiontime);
            time.setText(mRaceSelected.getQuestionDateTime(getActivity()));
        }

        Spinner raceSpinner = (Spinner) v.findViewById(R.id.race_spinner);
        if (raceSpinner != null) {
            raceSpinner.setAdapter(raceAdapter);
            raceSpinner.setSelection(raceAdapter.getPosition(mRaceSelected));
            raceSpinner.setOnItemSelectedListener(new RaceSelectedListener());
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
        SharedPreferences cache = getActivity().getSharedPreferences(ACACHE, Activity.MODE_PRIVATE);

        // Check if user changed their account status
        mChanged = mSetupNeeded != GAE.isAccountSetupNeeded(getActivity());
        if (mRaceSelected != null) {
            // See if race answers have been cleared by a new account connect
            mChanged |= ((mRa != null) && !cache.contains(CACHE_PREFIX + mRaceSelected.getId()));
        }
        if (mRaceNext != null) {
            // See if race questions are now available but weren't previously
            mChanged |= (mOnCreateViewTime < mRaceNext.getQuestionTimestamp()) && mRaceNext.inProgress();
            // Check if questions form needs to disappear because race started
            mChanged |= !mRaceNext.isFuture();
        }

        if (mChanged) {
            Util.log("Questions: onResume: notifyChanged");
            mRaceSelected = null;
            notifyChanged();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public boolean isChanged() {
        return mChanged;
    }

    @Subscribe
    public void onScheduleUpdate(ScheduleUpdateEvent event) {
        Util.log("Questions: onScheduleUpdate");
        mChanged = true;
        mRaceSelected = null;
        notifyChanged();
    }

    @Subscribe
    public void onRaceAlarm(RaceAlarmEvent event) {
        Util.log("Questions: onRaceAlarm");
        mChanged = true;
        mRaceSelected = null;
        notifyChanged();
    }

    private static class RaceQuestions {
        public String q2;
        public String[] a2;
        public String q3;
        public String[] a3;

        public static RaceQuestions fromJson(String json) {
            return new Gson().fromJson(json, RaceQuestions.class);
        }
    }

    private static class RaceAnswers {
        public int a1;
        public int a2;
        public int a3;
        public int a4;
        public int a5;

        public static RaceAnswers fromJson(String json) {
            return new Gson().fromJson(json, RaceAnswers.class);
        }
    }

    private class RaceSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
            Race race = (Race) parent.getItemAtPosition(pos);
            if (mRaceSelected.getId() != race.getId()) {
                mRaceSelected = race;
                Util.log("Questions: Selected race = " + mRaceSelected.getId());
                mChanged = true;
                notifyChanged();
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {}
    }

    private class WinnerSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
            Driver driver = (Driver) parent.getItemAtPosition(pos);
            mWinner = driver.getNumber();
        }

        public void onNothingSelected(AdapterView<?> parent) {}
    }

    private class A2SelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
            mA2 = pos;
        }

        public void onNothingSelected(AdapterView<?> parent) {}
    }

    private class A3SelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
            mA3 = pos;
        }

        public void onNothingSelected(AdapterView<?> parent) {}
    }

    private class MostLapsSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
            Driver driver = (Driver) parent.getItemAtPosition(pos);
            mMostLaps = driver.getNumber();
        }

        public void onNothingSelected(AdapterView<?> parent) {}
    }

    private class NumLeadersSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
            mNumLeaders = pos;
        }

        public void onNothingSelected(AdapterView<?> parent) {}
    }

    @Override
    public void onClick(View v) {
        mScroll = 0;
        mChanged = mSending = true;
        notifyChanged();

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        GAE.getInstance(getActivity()).postPage(this, "questions", gson.toJson(this));
    }

    @Override
    public void onScrollChanged(ObservableScrollView sv, int x, int y, int oldx, int oldy) {
        mScroll = y;
    }

    @Override
    public void onFailedConnect(Context context) {
        Util.log("Questions: onFailedConnect");

        // Verify application wasn't closed before callback returned
        if (getActivity() != null) {
            mSending = false;
            mFailedConnect = mChanged = true;
            notifyChanged();
        }
    }

    @Override
    public void onGet(Context context, String json) {
        Util.log("Questions: onGet: " + json);

        SharedPreferences cache = context.getSharedPreferences(QCACHE, Activity.MODE_PRIVATE);
        cache.edit().putString(CACHE_PREFIX + mRaceNext.getId(), json).commit();

        // Verify application wasn't closed before callback returned
        if (getActivity() != null) {
            mChanged = true;
            notifyChanged();
        }
    }

    @Override
    public void onConnectSuccess(Context context, String json) {
        Util.log("Questions: onConnectSuccess: " + json);
        Toast.makeText(context, R.string.questions_success, Toast.LENGTH_SHORT).show();

        SharedPreferences cache = context.getSharedPreferences(ACACHE, Activity.MODE_PRIVATE);
        cache.edit().putString(CACHE_PREFIX + mRaceNext.getId(), json).commit();

        // Verify application wasn't closed before callback returned
        if (getActivity() != null) {
            mSending = false;
            mChanged = true;
            notifyChanged();
        }
    }

    @Override
    public void onLaunchIntent(Intent launch) {}
}
