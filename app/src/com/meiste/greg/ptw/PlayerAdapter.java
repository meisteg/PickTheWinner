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
    private final Context mContext;

    public PlayerAdapter(final Context context) {
        super(context, R.layout.schedule_row);
        mContext = context;
        init();
    }

    private void init() {
        try {
            final BufferedReader in =
                    new BufferedReader(new InputStreamReader(mContext.openFileInput(Standings.FILENAME)));
            String line;
            final StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null)
                buffer.append(line).append('\n');
            in.close();
            mStandings = new Gson().fromJson(buffer.toString(), _Standings.class);
            findWildCards();
        } catch (final Exception e) {
            Util.log("Standings file not found");
        }
    }

    private void findWildCards() {
        mStandings.wildcards = new Player[2];

        // Wild card rule does not apply once in the Chase
        if (Race.getInstance(mContext, mStandings.race_id).isInChase())
            return;

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

    private boolean isWildCard(final Player p) {
        if (p.equals(mStandings.wildcards[0]) || p.equals(mStandings.wildcards[1]))
            return true;
        return false;
    }

    @Override
    public int getCount() {
        final Integer rank = mStandings.self.rank;
        if ((rank == null) || (rank > mStandings.standings.length))
            return mStandings.standings.length + 1;
        return mStandings.standings.length;
    }

    public int getCountWithoutPlayer() {
        return mStandings.standings.length;
    }

    @Override
    public void notifyDataSetChanged() {
        init();
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(final int pos, final View convertView, final ViewGroup parent) {
        final _ViewHolder holder;
        final Player p;
        View v = convertView;

        if (v == null) {
            final LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

        p = getItem(pos);

        if (isSelf(p))
            holder.row.setBackgroundResource(R.drawable.standings_self);
        else if (p.inChase() || isWildCard(p))
            holder.row.setBackgroundResource(R.drawable.standings_chase);
        else
            holder.row.setBackgroundResource(R.drawable.standings_other);

        holder.rank.setText(p.getRank());
        holder.points.setText(p.getPoints());

        if (p.getName() != null)
            holder.name.setText(p.getName());
        else
            holder.name.setText(mContext.getString(R.string.private_name));

        if (holder.races != null)
            holder.races.setText(p.getRaces());

        if (holder.wins != null)
            holder.wins.setText(p.getWins());

        return v;
    }

    @Override
    public Player getItem(final int position) {
        if (position < mStandings.standings.length) {
            if (mStandings.standings[position].rank != mStandings.self.rank) {
                return mStandings.standings[position];
            }
        }
        return mStandings.self;
    }

    public boolean isSelf(final Player p) {
        return p == mStandings.self;
    }

    public boolean isFriend(final Player p) {
        // TODO: Stub until friends supported on server
        return false;
    }

    public String getRaceAfterName() {
        return Race.getInstance(mContext, mStandings.race_id).getTrack(Race.NAME_SHORT);
    }

    public int getRaceAfterNum() {
        if (mStandings == null) {
            return -1;
        }
        return mStandings.race_id;
    }

    public String getPlayerName() {
        if (mStandings == null) {
            // This is safe since null indicates player name is private
            return null;
        }
        return mStandings.self.getName();
    }
}
