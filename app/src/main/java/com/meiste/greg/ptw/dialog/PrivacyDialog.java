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
package com.meiste.greg.ptw.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.meiste.greg.ptw.R;

public class PrivacyDialog extends BasePlayerDialog implements View.OnClickListener {

    private CheckBox mCb;
    private TextView mTv;
    private String mCurrentName;

    public PrivacyDialog(final Context context, final DialogInterface.OnClickListener listener) {
        super(context, listener);
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final View v = getLayoutInflater().inflate(R.layout.privacy, null);
        setView(v);
        setTitle(R.string.privacy);

        mCb = (CheckBox) v.findViewById(R.id.hide_name);
        mTv = (TextView) v.findViewById(R.id.player_name_label);
        mEt = (EditText) v.findViewById(R.id.player_name);

        mCb.setOnClickListener(this);

        super.onCreate(savedInstanceState);
    }

    public void show(final String name) {
        show();

        mCurrentName = name;
        mEt.setText(null);
        if (mCurrentName == null) {
            mCb.setChecked(true);
            mTv.setVisibility(View.GONE);
            mEt.setVisibility(View.GONE);
        } else {
            mCb.setChecked(false);
            mTv.setVisibility(View.VISIBLE);
            mEt.setVisibility(View.VISIBLE);
            mEt.append(mCurrentName);
        }
        mOk.setEnabled(false);
    }

    @Override
    public void onClick(final View view) {
        if (mCb.isChecked()) {
            mTv.setVisibility(View.GONE);
            mEt.setVisibility(View.GONE);
            mOk.setEnabled(true);
        } else {
            mTv.setVisibility(View.VISIBLE);
            mEt.setVisibility(View.VISIBLE);
            mEt.setText(null);
            if (mCurrentName != null)
                mEt.append(mCurrentName);
            mOk.setEnabled(false);
        }
    }

    public String getNewName() {
        if (mCb.isChecked()) {
            return null;
        }

        return mEt.getText().toString();
    }
}
