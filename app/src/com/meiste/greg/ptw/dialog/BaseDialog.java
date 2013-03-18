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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;

import com.meiste.greg.ptw.R;

public abstract class BaseDialog extends AlertDialog {

    protected final DialogInterface.OnClickListener mListener;
    protected boolean mResume = false;
    protected Button mOk;

    public BaseDialog(final Context context, final DialogInterface.OnClickListener listener) {
        super(context);
        mListener = listener;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setButton(DialogInterface.BUTTON_POSITIVE, getContext().getString(R.string.ok), mListener);
        setButton(DialogInterface.BUTTON_NEGATIVE, getContext().getString(R.string.cancel), mListener);

        super.onCreate(savedInstanceState);

        mOk = getButton(DialogInterface.BUTTON_POSITIVE);
    }

    public void onPause() {
        if (isShowing()) {
            dismiss();
            mResume = true;
        }
    }

    public void onResume() {
        if (mResume) {
            mResume = false;
            show();
        }
    }
}
