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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.meiste.greg.ptw.tab.Questions;
import com.meiste.greg.ptw.tab.RuleBook;
import com.meiste.greg.ptw.tab.Schedule;
import com.meiste.greg.ptw.tab.Standings;
import com.meiste.greg.ptw.tab.Suggest;
import com.meiste.greg.ptw.tab.TabFragment;

public class TabFragmentAdapter extends FragmentPagerAdapter {

    private static final int[] TAB_TITLES = {
        R.string.tab_rule_book,
        R.string.tab_questions,
        R.string.tab_standings,
        R.string.tab_schedule,
        R.string.tab_suggest,
    };

    private final TabFragment[] mFragments = new TabFragment[5];
    private final FragmentListener mFragmentListener = new _FragmentListener();
    private final Context mContext;

    public interface FragmentListener {
        void onChangedFragment();
    }

    public TabFragmentAdapter(final FragmentManager fm, final Context context) {
        super(fm);
        mContext = context;
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {
        final Object o = super.instantiateItem(container, position);
        mFragments[position] = (TabFragment)o;

        // HACK: On an orientation change, even though we created new
        // fragments, FragmentManager still has the old fragments, which
        // FragmentPagerAdapter checks for and uses instead of calling
        // getItem to get the new fragments. Need to give tabs updated
        // FragmentListener since old one is now invalid.
        mFragments[position].setFragmentListener(mFragmentListener);

        return o;
    }

    @Override
    public Fragment getItem(final int position) {
        switch (position) {
        case 0:
            return new RuleBook();
        case 1:
            return new Questions();
        case 2:
            return new Standings();
        case 3:
            return new Schedule();
        case 4:
            return new Suggest();
        default:
            return null;
        }
    }

    @Override
    public int getCount() {
        return mFragments.length;
    }

    @Override
    public int getItemPosition(final Object object) {
        return ((TabFragment)object).isChanged() ? POSITION_NONE : POSITION_UNCHANGED;
    }

    @Override
    public CharSequence getPageTitle(final int position) {
        if (position >= TAB_TITLES.length) {
            return Integer.toString(position);
        }
        return mContext.getString(TAB_TITLES[position]);
    }

    private class _FragmentListener implements FragmentListener {
        @Override
        public void onChangedFragment() {
            notifyDataSetChanged();
        }
    }
}