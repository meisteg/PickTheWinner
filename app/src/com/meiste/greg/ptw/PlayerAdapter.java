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

import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

public final class PlayerAdapter extends ArrayAdapter<Player> {
    private class _Standings {
        public int race_id;
        public Player[] standings;
        public Player self;
        public Player[] wildcards;
    }

    private class _ViewHolder {
        public LinearLayout row;
        public TextView rank;
        public TextView name;
        public TextView points;
        public TextView races;
        public TextView wins;
    }

    private _Standings mStandings;
    private Context mContext;

    public PlayerAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mContext = context;
        init();
    }

    private void init() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(mContext.openFileInput(Standings.FILENAME)));
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null)
                buffer.append(line).append('\n');
            in.close();
            mStandings = new Gson().fromJson(buffer.toString(), _Standings.class);
            findWildCards();
        } catch (Exception e) {
            Util.log("Standings file not found");
        }
    }

    private void findWildCards() {
        mStandings.wildcards = new Player[2];

        // Verify we have at least 12 players (remember, counting from zero)
        if (mStandings.standings.length < 11)
            return;

        // Start by assuming 11th and 12th in standings are the wild cards
        if (mStandings.standings[10].wins >= mStandings.standings[11].wins) {
            mStandings.wildcards[0] = mStandings.standings[10];
            mStandings.wildcards[1] = mStandings.standings[11];
        } else {
            mStandings.wildcards[0] = mStandings.standings[11];
            mStandings.wildcards[1] = mStandings.standings[10];
        }

        // Then, check 13th - 20th to see if they have more wins
        for (int i = 12; i < Math.min(20, mStandings.standings.length); ++i) {
            if (mStandings.standings[i].wins > mStandings.wildcards[0].wins) {
                mStandings.wildcards[1] = mStandings.wildcards[0];
                mStandings.wildcards[0] = mStandings.standings[i];
            } else if (mStandings.standings[i].wins > mStandings.wildcards[1].wins) {
                mStandings.wildcards[1] = mStandings.standings[i];
            }
        }
    }

    private boolean isWildCard(Player p) {
        if (p.equals(mStandings.wildcards[0]) || p.equals(mStandings.wildcards[1]))
            return true;
        return false;
    }

    @Override
    public int getCount() {
        if (mStandings.self.rank > mStandings.standings.length)
            return mStandings.standings.length + 1;
        return mStandings.standings.length;
    }

    @Override
    public void notifyDataSetChanged() {
        init();
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        _ViewHolder holder;
        Player p;
        View v = convertView;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.standings_row, null);

            holder = new _ViewHolder();
            holder.row = (LinearLayout) v.findViewById(R.id.player_row);
            holder.rank = (TextView) v.findViewById(R.id.player_rank);
            holder.name = (TextView) v.findViewById(R.id.player_name);
            holder.points = (TextView) v.findViewById(R.id.player_points);
            holder.races = (TextView) v.findViewById(R.id.player_races);
            holder.wins = (TextView) v.findViewById(R.id.player_wins);
            v.setTag(holder);
        } else {
            holder = (_ViewHolder) v.getTag();
        }

        if (pos < mStandings.standings.length)
            p = mStandings.standings[pos];
        else
            p = mStandings.self;

        if (mStandings.self.rank == p.rank)
            holder.row.setBackgroundResource(R.color.standings_self);
        else if (p.inChase() || isWildCard(p))
            holder.row.setBackgroundResource(R.color.standings_chase);
        else
            holder.row.setBackgroundResource(R.color.standings_other);

        holder.rank.setText(p.getRank());
        holder.name.setText(p.getName());
        holder.points.setText(p.getPoints());

        if (holder.races != null)
            holder.races.setText(p.getRaces());

        if (holder.wins != null)
            holder.wins.setText(p.getWins());

        return v;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public String getRaceAfter() {
        return Race.getInstance(mContext, mStandings.race_id).getTrack(Race.NAME_SHORT);
    }
}