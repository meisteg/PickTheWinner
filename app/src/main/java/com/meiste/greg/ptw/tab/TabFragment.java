/*
 * Copyright (C) 2012-2014 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.greg.ptw.tab;

import android.os.Handler;
import android.support.v4.app.Fragment;

import com.meiste.greg.ptw.TabFragmentAdapter.FragmentListener;
import com.meiste.greg.ptw.Util;

public abstract class TabFragment extends Fragment {

    private FragmentListener mFragmentListener;

    public void setFragmentListener(final FragmentListener fl) {
        mFragmentListener = fl;
    }

    protected void notifyChanged() {
        final String tab = this.getClass().getSimpleName();
        Util.log("notifyChanged() called by " + tab);

        try {
            if (mFragmentListener != null)
                mFragmentListener.onChangedFragment();
        } catch (final Exception e) {
            Util.log(tab + ": Failed to notifyChanged! - " + e);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Util.log(tab + ": Trying notifyChanged again...");
                    mFragmentListener.onChangedFragment();
                }
            }, 1000);
        }
    }

    public boolean isChanged() {
        return false;
    }
}
