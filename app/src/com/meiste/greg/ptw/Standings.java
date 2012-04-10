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
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public final class Standings extends TabFragment {
	private boolean mSetupNeeded;
	
	public static Standings newInstance(Context context) {
		Standings fragment = new Standings();
		fragment.setTitle(context.getString(R.string.tab_standings));
		
		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mSetupNeeded = GAE.isAccountSetupNeeded(getActivity());
		
		if (mSetupNeeded)
			return inflater.inflate(R.layout.no_account, container, false);
		
		TextView text = new TextView(getActivity());
		text.setGravity(Gravity.CENTER);
		text.setText("TODO");
		text.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		return text;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (mSetupNeeded != GAE.isAccountSetupNeeded(getActivity()))
			notifyChanged();
	}
	
	@Override
	public boolean isChanged() {
		return mSetupNeeded != GAE.isAccountSetupNeeded(getActivity());
	}
}
