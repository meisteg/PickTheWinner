/*
 * Copyright (C) 2013-2014 Gregory S. Meiste  <http://gregmeiste.com>
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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.Util;

public class QuestionsConnectFail extends Fragment {

    public static QuestionsConnectFail getInstance(final FragmentManager fm, final String tag) {
        QuestionsConnectFail f = (QuestionsConnectFail) fm.findFragmentByTag(tag);
        if (f == null) {
            f = new QuestionsConnectFail();
        }
        return f;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        Util.log("QuestionsConnectFail: onCreateView");
        final View v = inflater.inflate(R.layout.no_connection, container, false);

        final Button retry = (Button) v.findViewById(R.id.retry);
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                ((Questions) getParentFragment()).setSubFragment();
            }
        });

        return v;
    }
}
