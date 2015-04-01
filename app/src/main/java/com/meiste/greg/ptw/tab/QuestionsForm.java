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

package com.meiste.greg.ptw.tab;

import android.app.ActivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.meiste.greg.ptw.Driver;
import com.meiste.greg.ptw.DriverAdapter;
import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.RaceQuestions;

import timber.log.Timber;

public class QuestionsForm extends Fragment implements View.OnClickListener {

    private static String RACE_QUESTIONS = "qjson";

    @Expose
    @SerializedName("a1")
    private int mWinner;

    @Expose
    @SerializedName("a2")
    private int mA2;

    @Expose
    @SerializedName("a3")
    private int mA3;

    @Expose
    @SerializedName("a4")
    private int mMostLaps;

    @Expose
    @SerializedName("a5")
    private int mNumLeaders;

    public static QuestionsForm getInstance(final FragmentManager fm, final String json) {
        QuestionsForm f = (QuestionsForm) fm.findFragmentByTag(json);
        if (f == null) {
            f = new QuestionsForm();

            final Bundle args = new Bundle();
            args.putString(RACE_QUESTIONS, json);
            f.setArguments(args);
        }

        return f;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        Timber.v("onCreateView");

        final String json = getArguments().getString(RACE_QUESTIONS);
        if (json == null) {
            return null;
        }
        final RaceQuestions rq = RaceQuestions.fromJson(json);
        final View v = inflater.inflate(R.layout.questions_form, container, false);

        final Spinner winner = (Spinner) v.findViewById(R.id.winner);
        winner.setAdapter(new DriverAdapter(getActivity(), rq.drivers));
        winner.setOnItemSelectedListener(new WinnerSelectedListener());

        final TextView q2 = (TextView) v.findViewById(R.id.question2);
        q2.setText(getActivity().getString(R.string.questions_2, rq.q2));

        final Spinner a2 = (Spinner) v.findViewById(R.id.question2a);
        final ArrayAdapter<CharSequence> a2_adapter = new ArrayAdapter<CharSequence>(
                getActivity(), android.R.layout.simple_spinner_item, rq.a2);
        a2_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        a2.setAdapter(a2_adapter);
        a2.setOnItemSelectedListener(new A2SelectedListener());

        final TextView q3 = (TextView) v.findViewById(R.id.question3);
        q3.setText(getActivity().getString(R.string.questions_3, rq.q3));

        final Spinner a3 = (Spinner) v.findViewById(R.id.question3a);
        final ArrayAdapter<CharSequence> a3_adapter = new ArrayAdapter<CharSequence>(
                getActivity(), android.R.layout.simple_spinner_item, rq.a3);
        a3_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        a3.setAdapter(a3_adapter);
        a3.setOnItemSelectedListener(new A3SelectedListener());

        final Spinner mostlaps = (Spinner) v.findViewById(R.id.mostlaps);
        mostlaps.setAdapter(new DriverAdapter(getActivity(), rq.drivers));
        mostlaps.setOnItemSelectedListener(new MostLapsSelectedListener());

        final Spinner numleaders = (Spinner) v.findViewById(R.id.numleaders);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.num_leaders, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numleaders.setAdapter(adapter);
        numleaders.setOnItemSelectedListener(new NumLeadersSelectedListener());

        final Button send = (Button) v.findViewById(R.id.send);
        send.setOnClickListener(this);

        return v;
    }

    private class WinnerSelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(final AdapterView<?> parent, final View v, final int pos, final long id) {
            final Driver driver = (Driver) parent.getItemAtPosition(pos);
            mWinner = driver.getNumber();
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {}
    }

    private class A2SelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(final AdapterView<?> parent, final View v, final int pos, final long id) {
            mA2 = pos;
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {}
    }

    private class A3SelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(final AdapterView<?> parent, final View v, final int pos, final long id) {
            mA3 = pos;
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {}
    }

    private class MostLapsSelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(final AdapterView<?> parent, final View v, final int pos, final long id) {
            final Driver driver = (Driver) parent.getItemAtPosition(pos);
            mMostLaps = driver.getNumber();
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {}
    }

    private class NumLeadersSelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(final AdapterView<?> parent, final View v, final int pos, final long id) {
            mNumLeaders = pos;
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {}
    }

    @Override
    public void onClick(final View v) {
        if (ActivityManager.isUserAMonkey()) {
            Timber.v("onClick: User is a monkey!");
            return;
        }
        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        ((Questions) getParentFragment()).onSubmitAnswers(gson.toJson(this));
    }
}
