/*
 * Copyright (C) 2013 Gregory S. Meiste  <http://gregmeiste.com>
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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.widget.EditText;

public abstract class BasePlayerDialog extends BaseDialog implements TextWatcher, InputFilter {

    protected EditText mEt;

    public BasePlayerDialog(final Context context, final DialogInterface.OnClickListener listener) {
        super(context, listener);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        if (mEt != null) {
            mEt.addTextChangedListener(this);
            mEt.setFilters(new InputFilter[] { this });
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void afterTextChanged(final Editable editable) {
        if (editable.length() >= 5)
            mOk.setEnabled(true);
        else
            mOk.setEnabled(false);
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {}

    @Override
    public CharSequence filter(final CharSequence source, final int start, final int end,
            final Spanned dest, final int dstart, final int dend) {
        for (int i = start; i < end; i++) {
            if (!Character.isLetterOrDigit(source.charAt(i))) {
                return "";
            }
        }
        return null;
    }
}
