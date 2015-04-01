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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.Race;

import timber.log.Timber;

public class QuestionsNotYet extends Fragment {

    private static String RACE_ID = "race_id";

    public static QuestionsNotYet getInstance(final FragmentManager fm, final Race race) {
        QuestionsNotYet f = (QuestionsNotYet) fm.findFragmentByTag(getTag(race));
        if (f == null) {
            f = new QuestionsNotYet();

            final Bundle args = new Bundle();
            args.putInt(RACE_ID, race.getId());
            f.setArguments(args);
        }

        return f;
    }

    public static String getTag(final Race race) {
        return QuestionsNotYet.class.getName() + race.getId();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        Timber.v("onCreateView");

        final View v = inflater.inflate(R.layout.questions_not_yet, container, false);
        final Race race = Race.getInstance(getActivity(), getArguments().getInt(RACE_ID, 0));

        final TextView time = (TextView) v.findViewById(R.id.questiontime);
        time.setText(race.getQuestionDateTime(getActivity()));

        return v;
    }
}
