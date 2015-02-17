/*
 * Copyright (C) 2012, 2014-2015 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.meiste.greg.ptw.tab.Questions;

import butterknife.ButterKnife;
import butterknife.InjectView;

public final class QuestionsRaceAdapter extends BaseAdapter {
    private final List<Race> mRaces = new ArrayList<>();
    private final LayoutInflater mInflater;
    private final int widthPx;

    static class ViewHolder {
        @InjectView(R.id.race_name) TextView raceName;
        @InjectView(R.id.race_track) TextView raceTrack;

        public ViewHolder(final View view, final int w) {
            ButterKnife.inject(this, view);
            raceName.setWidth(w);
            raceTrack.setWidth(w);
        }
    }

    public QuestionsRaceAdapter(final Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final Race[] allRaces = Races.get(context);
        final SharedPreferences qcache = context.getSharedPreferences(Questions.QCACHE, Activity.MODE_PRIVATE);
        final SharedPreferences acache = context.getSharedPreferences(Questions.ACACHE, Activity.MODE_PRIVATE);

        for (final Race race : allRaces) {
            if (qcache.contains(Questions.cachePrefix() + race.getId())) {
                if (acache.contains(Questions.cachePrefix() + race.getId()) ||
                        race.isRecent()) {
                    mRaces.add(race);
                }
            }
        }

        if (getCount() == 0 || !getItem(getCount() - 1).isRecent()) {
            // Add next race to the list
            final Race next = Race.getNext(context, false, true);
            if (next != null) {
                if ((getCount() <= 0) || mRaces.get(getCount() - 1).getId() != next.getId()) {
                    mRaces.add(next);
                }
            }
        }

        final DisplayMetrics dm = context.getResources().getDisplayMetrics();
        widthPx = (int) (dm.widthPixels - (50 * dm.density));
    }

    @Override
    public int getCount() {
        return mRaces.size();
    }

    @Override
    public Race getItem(final int position) {
        return mRaces.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getPosition(final Race race) {
        return mRaces.indexOf(race);
    }

    @Override
    public View getView(final int pos, final View convertView, final ViewGroup parent) {
        return commonView(pos, convertView, parent, R.layout.questions_race_spinner);
    }

    @Override
    public View getDropDownView(final int pos, final View convertView, final ViewGroup parent) {
        return commonView(pos, convertView, parent, R.layout.questions_race_spinner_dropdown);
    }

    private View commonView(final int pos, final View convertView, final ViewGroup parent, final int layout) {
        final ViewHolder holder;
        View v = convertView;

        if (v == null) {
            v = mInflater.inflate(layout, parent, false);

            holder = new ViewHolder(v, widthPx);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        holder.raceName.setText(mRaces.get(pos).getName());
        holder.raceTrack.setText(mRaces.get(pos).getTrack(Race.NAME_LONG));

        return v;
    }
}
