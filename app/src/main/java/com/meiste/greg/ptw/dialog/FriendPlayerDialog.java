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
import android.widget.EditText;

import com.meiste.greg.ptw.R;

public class FriendPlayerDialog extends BasePlayerDialog {

    public FriendPlayerDialog(final Context context, final DialogInterface.OnClickListener listener) {
        super(context, listener);
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final View v = getLayoutInflater().inflate(R.layout.friend_by_player_name, null);
        setView(v);
        setTitle(R.string.add_friend);

        mEt = (EditText) v.findViewById(R.id.player_name);
        super.onCreate(savedInstanceState);

        mOk.setEnabled(false);
    }

    public void reset() {
        if (mEt != null) {
            mEt.setText(null);
        }
    }

    public String getPlayerName() {
        return mEt.getText().toString();
    }
}
