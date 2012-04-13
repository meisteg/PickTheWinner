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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class RaceItemAdapter extends ArrayAdapter<Race> {
    private Race[] mRaces;
    private Context mContext;

    public RaceItemAdapter(Context context, int textViewResourceId, Race[] races) {
        super(context, textViewResourceId, races);
        mContext = context;
        mRaces = races;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.schedule_row, null);
        }

        if (mRaces[pos] == null) {
            mRaces[pos] = new Race(mContext, pos);
        }

        LinearLayout row = (LinearLayout) v.findViewById(R.id.row);
        TextView raceNum = (TextView) v.findViewById(R.id.race_num);
        TextView startDate = (TextView) v.findViewById(R.id.race_date);
        TextView startTime = (TextView) v.findViewById(R.id.race_time);
        TextView name = (TextView) v.findViewById(R.id.race_name);
        TextView trackLong = (TextView) v.findViewById(R.id.race_track);
        TextView trackShort = (TextView) v.findViewById(R.id.race_track_short);
        TextView tv = (TextView) v.findViewById(R.id.race_tv);

        if (!mRaces[pos].isFuture())
            row.setBackgroundResource(R.drawable.schedule_past);
        else if (mRaces[pos].isInChase())
            row.setBackgroundResource(R.drawable.schedule_chase);
        else
            row.setBackgroundResource(R.drawable.schedule_future);

        if (raceNum != null)
            raceNum.setText(mRaces[pos].getRaceNum());

        if (startDate != null)
            startDate.setText(mRaces[pos].getStartDate());

        if (startTime != null)
            startTime.setText(mRaces[pos].getStartTime());

        if (name != null)
            name.setText(mRaces[pos].getName());

        if (trackLong != null)
            trackLong.setText(mRaces[pos].getTrack(Race.NAME_LONG));

        if (trackShort != null)
            trackShort.setText(mRaces[pos].getTrack(Race.NAME_SHORT));

        if (tv != null)
            tv.setText(mRaces[pos].getTv());

        return v;
    }
}
