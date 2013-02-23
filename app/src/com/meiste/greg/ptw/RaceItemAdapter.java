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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class RaceItemAdapter extends ArrayAdapter<Race> {
    private Race[] mRaces;
    private final Context mContext;

    private class ViewHolder {
        public LinearLayout row;
        public TextView raceNum;
        public TextView startDate;
        public TextView startTime;
        public TextView name;
        public TextView trackLong;
        public TextView trackShort;
        public TextView tv;
    }

    public RaceItemAdapter(final Context context, final int textViewResourceId) {
        super(context, textViewResourceId);
        mContext = context;
        mRaces = Races.get(context);
    }

    @Override
    public int getCount() {
        return mRaces.length;
    }

    @Override
    public void notifyDataSetChanged() {
        mRaces = Races.get(mContext);
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(final int pos, final View convertView, final ViewGroup parent) {
        final ViewHolder holder;
        View v = convertView;

        if (v == null) {
            final LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.schedule_row, null);

            holder = new ViewHolder();
            holder.row = (LinearLayout) v.findViewById(R.id.row);
            holder.raceNum = (TextView) v.findViewById(R.id.race_num);
            holder.startDate = (TextView) v.findViewById(R.id.race_date);
            holder.startTime = (TextView) v.findViewById(R.id.race_time);
            holder.name = (TextView) v.findViewById(R.id.race_name);
            holder.trackLong = (TextView) v.findViewById(R.id.race_track);
            holder.trackShort = (TextView) v.findViewById(R.id.race_track_short);
            holder.tv = (TextView) v.findViewById(R.id.race_tv);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        if (!mRaces[pos].isFuture() && !mRaces[pos].isRecent())
            holder.row.setBackgroundResource(R.drawable.schedule_past);
        else if (mRaces[pos].isInChase())
            holder.row.setBackgroundResource(R.drawable.schedule_chase);
        else
            holder.row.setBackgroundResource(R.drawable.schedule_future);

        if (holder.raceNum != null)
            holder.raceNum.setText(mRaces[pos].getRaceNum());

        if (holder.startDate != null)
            holder.startDate.setText(mRaces[pos].getStartDate(mContext));

        if (holder.startTime != null)
            holder.startTime.setText(mRaces[pos].getStartTime(mContext));

        if (holder.name != null)
            holder.name.setText(mRaces[pos].getName());

        if (holder.trackLong != null)
            holder.trackLong.setText(mRaces[pos].getTrack(Race.NAME_LONG));

        if (holder.trackShort != null)
            holder.trackShort.setText(mRaces[pos].getTrack(Race.NAME_SHORT));

        if (holder.tv != null)
            holder.tv.setText(mRaces[pos].getTv());

        return v;
    }
}
