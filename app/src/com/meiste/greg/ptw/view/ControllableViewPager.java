/*
 * Copyright (C) 2014 Gregory S. Meiste  <http://gregmeiste.com>
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
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ControllableViewPager extends ViewPager {

    private boolean mPagingEnabled;

    public ControllableViewPager(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mPagingEnabled = true;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (mPagingEnabled) {
            return super.onTouchEvent(event);
        }

        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent event) {
        if (mPagingEnabled) {
            return super.onInterceptTouchEvent(event);
        }

        return true;
    }

    public void setPagingEnabled(final boolean enabled) {
        mPagingEnabled = enabled;
    }
}
