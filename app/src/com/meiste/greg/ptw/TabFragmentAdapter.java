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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

class TabFragmentAdapter extends FragmentPagerAdapter {
    private TabFragment[] mFragments = new TabFragment[5];
    private FragmentListener mFragmentListener = new MyFragmentListener();
    private Context mContext;

    public interface FragmentListener {
        void onChangedFragment();
    }

    public TabFragmentAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object o = super.instantiateItem(container, position);
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
    public Fragment getItem(int position) {
        switch (position) {
        case 0:
            return RuleBook.newInstance(mContext);
        case 1:
            return Questions.newInstance(mContext);
        case 2:
            return Standings.newInstance(mContext);
        case 3:
            return Schedule.newInstance(mContext);
        case 4:
            return Suggest.newInstance(mContext);
        default:
            return null;
        }
    }

    @Override
    public int getCount() {
        return mFragments.length;
    }

    @Override
    public int getItemPosition(Object object) {
        return ((TabFragment)object).isChanged() ? POSITION_NONE : POSITION_UNCHANGED;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (mFragments[position] == null)
            return Integer.toString(position);

        return mFragments[position].getTitle();
    }

    private class MyFragmentListener implements FragmentListener {
        public void onChangedFragment() {
            notifyDataSetChanged();
        }
    }
}