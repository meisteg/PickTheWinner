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
package com.meiste.greg.ptw.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.meiste.greg.ptw.R;

public class FriendMethodDialog extends BaseDialog implements OnClickListener {

    public static final int METHOD_NONE = 0;
    public static final int METHOD_MANUAL = 1;
    public static final int METHOD_NFC = 2;

    private RadioButton mRadioManual;
    private RadioButton mRadioNfc;
    private LinearLayout mLayoutManual;
    private LinearLayout mLayoutNfc;

    public FriendMethodDialog(final Context context, final DialogInterface.OnClickListener listener) {
        super(context, listener);
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final View v = getLayoutInflater().inflate(R.layout.friend_method, null);
        setView(v);
        setTitle(R.string.friend_method);

        mRadioManual = (RadioButton) v.findViewById(R.id.radio_manual);
        mRadioManual.setOnClickListener(this);
        mRadioNfc = (RadioButton) v.findViewById(R.id.radio_nfc);
        mRadioNfc.setOnClickListener(this);

        mLayoutManual = (LinearLayout) v.findViewById(R.id.friend_layout_manual);
        mLayoutManual.setOnClickListener(this);
        mLayoutNfc = (LinearLayout) v.findViewById(R.id.friend_layout_nfc);
        mLayoutNfc.setOnClickListener(this);

        super.onCreate(savedInstanceState);
        reset();
    }

    public void reset() {
        if (mOk != null) {
            mOk.setEnabled(false);
        }
        if (mRadioManual != null) {
            mRadioManual.setChecked(false);
        }
        if (mRadioNfc != null) {
            mRadioNfc.setChecked(false);
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
        case R.id.friend_layout_manual:
            mRadioManual.setChecked(true);
        case R.id.radio_manual:
            mRadioNfc.setChecked(false);
            mOk.setEnabled(true);
            break;

        case R.id.friend_layout_nfc:
            mRadioNfc.setChecked(true);
        case R.id.radio_nfc:
            mRadioManual.setChecked(false);
            mOk.setEnabled(true);
            break;
        }
    }

    public int getSelectedMethod() {
        if ((mRadioManual != null) && (mRadioManual.isChecked())) {
            return METHOD_MANUAL;
        } else if ((mRadioNfc != null) && (mRadioNfc.isChecked())) {
            return METHOD_NFC;
        }
        return METHOD_NONE;
    }
}
