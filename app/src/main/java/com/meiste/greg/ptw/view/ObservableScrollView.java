/*
 * Copyright (C) 2012, 2014 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.greg.ptw.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public final class ObservableScrollView extends ScrollView {
    public interface ScrollViewListener {
        void onScrollChanged(ObservableScrollView sv, int x, int y, int oldx, int oldy);
    }

    private ScrollViewListener mScrollViewListener = null;

    public ObservableScrollView(final Context context) {
        super(context);
    }

    public ObservableScrollView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public ObservableScrollView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollViewListener(final ScrollViewListener l) {
        mScrollViewListener = l;
    }

    @Override
    protected void onScrollChanged(final int x, final int y, final int oldx, final int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        if (mScrollViewListener != null) {
            mScrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
        }
    }

    public void postScrollTo(final int x, final int y) {
        post(new Runnable() {
            @Override
            public void run() {
                ObservableScrollView.this.scrollTo(x, y);
            }
        });
    }
}
