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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.meiste.greg.ptw.ObservableScrollView.ScrollViewListener;

public final class RuleBook extends TabFragment implements ScrollViewListener {
    private int mScroll = 0;

    public static RuleBook newInstance(final Context context) {
        final RuleBook fragment = new RuleBook();
        fragment.setTitle(context.getString(R.string.tab_rule_book));

        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        setRetainInstance(true);
        final View v = inflater.inflate(R.layout.rules, container, false);
        final ObservableScrollView sv = (ObservableScrollView) v.findViewById(R.id.scroll_rules);
        sv.postScrollTo(0, mScroll);
        sv.setScrollViewListener(this);

        return v;
    }

    @Override
    public void onScrollChanged(final ObservableScrollView sv, final int x, final int y,
            final int oldx, final int oldy) {
        mScroll = y;
    }
}
