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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.meiste.greg.ptw.ObservableScrollView;
import com.meiste.greg.ptw.ObservableScrollView.ScrollViewListener;
import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.Util;

public final class RuleBook extends TabFragment implements ScrollViewListener {
    private static final String FILENAME = "rule_book";

    private int mScroll = 0;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        setRetainInstance(true);
        final View v = inflater.inflate(R.layout.rules, container, false);

        final LinearLayout l = (LinearLayout) v.findViewById(R.id.rules_container);
        final Resources res = getActivity().getResources();
        final int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 10, res.getDisplayMetrics());

        final Rules rules = Rules.get(getActivity());
        if (rules != null) {
            for (final Rules.Section s : rules.sections) {
                final TextView header = new TextView(getActivity());
                header.setText(s.header);
                header.setTextAppearance(getActivity(), R.style.RuleBookHeader);
                header.setPadding(padding, padding, padding, 0);
                l.addView(header);

                for (final String b : s.body) {
                    final TextView body = new TextView(getActivity());
                    final SpannableStringBuilder str = new SpannableStringBuilder(b);
                    int index = 0;
                    final Pattern p = Pattern.compile("\\d+ points: ");
                    final Matcher m = p.matcher(b);
                    while (m.find()) {
                        index = b.indexOf(m.group(), index);
                        if (index > 0) {
                            str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                                    index, index + m.group().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                    body.setText(str);
                    body.setTextAppearance(getActivity(), R.style.RuleBookText);
                    body.setPadding(padding, padding, padding, padding);
                    l.addView(body);
                }
            }
        }

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

    public static boolean isValid(final Context context, final long time) {
        final Rules rules = Rules.get(context);
        if ((rules != null) && (time <= rules.timestamp)) {
            return true;
        }
        return false;
    }

    public static void update(final Context context, final String json) {
        try {
            final FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(json.getBytes());
            fos.close();
        } catch (final Exception e) {
            Util.log("Failed to save new rule book");
        }
    }

    private static class Rules {
        public static class Section {
            public String header;
            public String[] body;
        }

        public long timestamp;
        public Section[] sections;

        private static Rules get(final Context context) {
            try {
                final InputStream is = context.openFileInput(FILENAME);
                final BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line;
                final StringBuilder buffer = new StringBuilder();
                while ((line = in.readLine()) != null)
                    buffer.append(line).append('\n');
                in.close();
                return new Gson().fromJson(buffer.toString(), Rules.class);
            } catch (final JsonSyntaxException e) {
                Util.log(e.toString());
                context.deleteFile(FILENAME);
            } catch (final IOException e) {
            }
            return null;
        }
    }
}
