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

import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.meiste.greg.ptw.Driver;
import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.RaceAnswers;
import com.meiste.greg.ptw.RaceQuestions;

import timber.log.Timber;

public class QuestionsAnswers extends Fragment {

    private static String RACE_QUESTIONS = "qjson";
    private static String RACE_ANSWERS = "ajson";
    private static String RACE_CORRECT_ANSWERS = "cajson";

    public static QuestionsAnswers getInstance(final FragmentManager fm,
            final String qjson, final String ajson, final String cajson) {
        QuestionsAnswers f = (QuestionsAnswers) fm.findFragmentByTag(getTag(qjson, ajson, cajson));
        if (f == null) {
            f = new QuestionsAnswers();

            final Bundle args = new Bundle();
            args.putString(RACE_QUESTIONS, qjson);
            args.putString(RACE_ANSWERS, ajson);
            args.putString(RACE_CORRECT_ANSWERS, cajson);
            f.setArguments(args);
        }

        return f;
    }

    public static String getTag(final String qjson, final String ajson, final String cajson) {
        if (cajson != null) {
            return qjson + ajson + cajson;
        }
        return qjson + ajson;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        Timber.v("onCreateView");

        final String qjson = getArguments().getString(RACE_QUESTIONS);
        if (qjson == null) {
            return null;
        }
        final String ajson = getArguments().getString(RACE_ANSWERS);
        if (ajson == null) {
            return null;
        }
        final RaceQuestions rq = RaceQuestions.fromJson(qjson);
        final RaceAnswers ra = RaceAnswers.fromJson(ajson);
        final Resources res = getActivity().getResources();

        final View v = inflater.inflate(R.layout.questions_answered, container, false);

        final TextView q2 = (TextView) v.findViewById(R.id.question2);
        q2.setText(getActivity().getString(R.string.questions_2, rq.q2));

        final TextView q3 = (TextView) v.findViewById(R.id.question3);
        q3.setText(getActivity().getString(R.string.questions_3, rq.q3));

        final TextView a1 = (TextView) v.findViewById(R.id.answer1);
        a1.setText(Driver.find(rq.drivers, res, ra.a1).getName());

        final TextView a2 = (TextView) v.findViewById(R.id.answer2);
        a2.setText(rq.a2[ra.a2]);

        final TextView a3 = (TextView) v.findViewById(R.id.answer3);
        a3.setText(rq.a3[ra.a3]);

        final TextView a4 = (TextView) v.findViewById(R.id.answer4);
        a4.setText(Driver.find(rq.drivers, res, ra.a4).getName());

        final TextView a5 = (TextView) v.findViewById(R.id.answer5);
        a5.setText(res.getStringArray(R.array.num_leaders)[ra.a5]);

        final String cajson = getArguments().getString(RACE_CORRECT_ANSWERS);
        if (cajson != null) {
            Timber.d("Correct answers available");
            final RaceAnswers rca = RaceAnswers.fromJson(cajson);

            // Have to check for null in case there is no correct answer
            if ((rca.a1 != null) && (rca.a1 >= 0)) {
                if (rca.a1.equals(ra.a1)) {
                    a1.setTextColor(res.getColor(R.color.answer_right));
                } else {
                    a1.setPaintFlags(a1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    a1.setTextColor(res.getColor(R.color.answer_wrong));
                    final TextView c1 = (TextView) v.findViewById(R.id.correct1);
                    c1.setText(Driver.find(rq.drivers, res, rca.a1).getName());
                    c1.setVisibility(View.VISIBLE);
                }
            }
            if ((rca.a2 != null) && (rca.a2 >= 0)) {
                if (rca.a2.equals(ra.a2)) {
                    a2.setTextColor(res.getColor(R.color.answer_right));
                } else {
                    a2.setPaintFlags(a2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    a2.setTextColor(res.getColor(R.color.answer_wrong));
                    final TextView c2 = (TextView) v.findViewById(R.id.correct2);
                    c2.setText(rq.a2[rca.a2]);
                    c2.setVisibility(View.VISIBLE);
                }
            }
            if ((rca.a3 != null) && (rca.a3 >= 0)) {
                if (rca.a3.equals(ra.a3)) {
                    a3.setTextColor(res.getColor(R.color.answer_right));
                } else {
                    a3.setPaintFlags(a3.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    a3.setTextColor(res.getColor(R.color.answer_wrong));
                    final TextView c3 = (TextView) v.findViewById(R.id.correct3);
                    c3.setText(rq.a3[rca.a3]);
                    c3.setVisibility(View.VISIBLE);
                }
            }
            if ((rca.a4 != null) && (rca.a4 >= 0)) {
                if (rca.a4.equals(ra.a4)) {
                    a4.setTextColor(res.getColor(R.color.answer_right));
                } else {
                    a4.setPaintFlags(a4.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    a4.setTextColor(res.getColor(R.color.answer_wrong));
                    final TextView c4 = (TextView) v.findViewById(R.id.correct4);
                    c4.setText(Driver.find(rq.drivers, res, rca.a4).getName());
                    c4.setVisibility(View.VISIBLE);
                }
            }
            if ((rca.a5 != null) && (rca.a5 >= 0)) {
                if (rca.a5.equals(ra.a5)) {
                    a5.setTextColor(res.getColor(R.color.answer_right));
                } else {
                    a5.setPaintFlags(a5.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    a5.setTextColor(res.getColor(R.color.answer_wrong));
                    final TextView c5 = (TextView) v.findViewById(R.id.correct5);
                    c5.setText(res.getStringArray(R.array.num_leaders)[rca.a5]);
                    c5.setVisibility(View.VISIBLE);
                }
            }
        }

        return v;
    }
}
