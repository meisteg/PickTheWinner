/*
 * Copyright (C) 2012-2015 Gregory S. Meiste  <http://gregmeiste.com>
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
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

public final class RaceItemAdapter extends CursorAdapter {

    private final LayoutInflater mInflater;

    static class ViewHolder {
        @InjectView(R.id.row) LinearLayout row;
        @InjectView(R.id.race_num) TextView raceNum;
        @InjectView(R.id.race_date) TextView startDate;
        @Optional @InjectView(R.id.race_time) TextView startTime;
        @Optional @InjectView(R.id.race_name) TextView name;
        @Optional @InjectView(R.id.race_track) TextView trackLong;
        @Optional @InjectView(R.id.race_track_short) TextView trackShort;
        @Optional @InjectView(R.id.race_tv) TextView tv;

        public ViewHolder(final View view) {
            ButterKnife.inject(this, view);
        }
    }

    public RaceItemAdapter(final Context context) {
        super(context, null, 0);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final View v = mInflater.inflate(R.layout.schedule_row, parent, false);
        v.setTag(new ViewHolder(v));
        return v;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        final Race race = new Race(cursor);

        if (!race.isFuture() && !race.isRecent())
            holder.row.setBackgroundResource(R.drawable.schedule_past);
        else if (race.isInChase())
            holder.row.setBackgroundResource(R.drawable.schedule_chase);
        else
            holder.row.setBackgroundResource(R.drawable.schedule_future);

        holder.raceNum.setText(race.getRaceNum());
        holder.startDate.setText(race.getStartDate(context));

        if (holder.startTime != null) {
            holder.startTime.setText(race.getStartTime(context));
        }
        if (holder.name != null) {
            holder.name.setText(race.getName());
        }
        if (holder.trackLong != null) {
            holder.trackLong.setText(race.getTrack(Race.NAME_LONG));
        }
        if (holder.trackShort != null) {
            holder.trackShort.setText(race.getTrack(Race.NAME_SHORT));
        }
        if (holder.tv != null) {
            holder.tv.setText(race.getTv());
        }
    }
}
