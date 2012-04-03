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

import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.viewpagerindicator.TitleProvider;

class TabFragmentAdapter extends FragmentPagerAdapter implements TitleProvider {
	private List<TabFragment> mFragments;
	private FragmentListener mFragmentListener;
	
	public interface FragmentListener {
	    void onChangedFragment();
	}
	
	public TabFragmentAdapter(FragmentManager fm, Context context) {
		super(fm);
		
		mFragmentListener = new MyFragmentListener();
		
		mFragments = new Vector<TabFragment>();
		mFragments.add(RuleBook.newInstance(context));
		mFragments.add(Questions.newInstance(context));
		mFragments.add(Standings.newInstance(context));
		mFragments.add(Schedule.newInstance(context));
		mFragments.add(Suggest.newInstance(context));
	}
	
	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		Object o = super.instantiateItem(container, position);
		
		// HACK: On an orientation change, even though we created new
		// fragments, FragmentManager still has the old fragments, which
		// FragmentPagerAdapter checks for and uses instead of calling
		// getItem to get the new fragments. Need to give tabs updated
		// FragmentListener since old one is now invalid.
		((TabFragment)o).setFragmentListener(mFragmentListener);
		
		return o;
	}

	@Override
	public Fragment getItem(int position) {
		return this.mFragments.get(position);
	}

	@Override
	public int getCount() {
		return mFragments.size();
	}
	
	@Override
	public int getItemPosition(Object object) {
		return ((TabFragment)object).isChanged() ? POSITION_NONE : POSITION_UNCHANGED;
	}
	
	@Override
	public String getTitle(int position) {
		return mFragments.get(position).getTitle();
	}
	
	private class MyFragmentListener implements FragmentListener {
		public void onChangedFragment() {
			notifyDataSetChanged();
		}
	}
}