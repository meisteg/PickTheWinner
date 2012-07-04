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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class PrivacyDialog extends AlertDialog implements View.OnClickListener, TextWatcher, InputFilter {

    private final DialogInterface.OnClickListener mListener;
    private CheckBox mCb;
    private TextView mTv;
    private EditText mEt;
    private Button mOk;
    private String mCurrentName;

    protected PrivacyDialog(Context context, DialogInterface.OnClickListener listener) {
        super(context);
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View v = getLayoutInflater().inflate(R.layout.privacy, null);
        setView(v);
        setTitle(R.string.privacy);

        mCb = (CheckBox) v.findViewById(R.id.hide_name);
        mTv = (TextView) v.findViewById(R.id.player_name_label);
        mEt = (EditText) v.findViewById(R.id.player_name);

        mCb.setOnClickListener(this);
        mEt.addTextChangedListener(this);
        mEt.setFilters(new InputFilter[] { this });
        setButton(DialogInterface.BUTTON_POSITIVE, getContext().getString(R.string.ok), mListener);
        setButton(DialogInterface.BUTTON_NEGATIVE, getContext().getString(R.string.cancel), mListener);

        super.onCreate(savedInstanceState);

        mOk = getButton(DialogInterface.BUTTON_POSITIVE);
    }

    public void show(String name) {
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
    public void onClick(View view) {
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

    @Override
    public void afterTextChanged(Editable editable) {
        if (editable.length() >= 5)
            mOk.setEnabled(true);
        else
            mOk.setEnabled(false);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
            Spanned dest, int dstart, int dend) {
        for (int i = start; i < end; i++) {
            if (!Character.isLetterOrDigit(source.charAt(i))) {
                return "";
            }
        }
        return null;
    }

    public String getNewName() {
        if (mCb.isChecked()) {
            return null;
        }

        return mEt.getText().toString();
    }
}
