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
import android.widget.TextView;

import com.meiste.greg.ptw.Player;
import com.meiste.greg.ptw.R;

public class FriendActionDialog extends BaseDialog {

    private Player mPlayer;
    private TextView mTv;

    public FriendActionDialog(final Context context, final DialogInterface.OnClickListener listener) {
        super(context, listener);
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final View v = getLayoutInflater().inflate(R.layout.friend_action, null);
        setView(v);
        mTv = (TextView) v.findViewById(R.id.friend_action);

        setupView();
        super.onCreate(savedInstanceState);
    }

    private void setupView() {
        setTitle(mPlayer.isFriend() ? R.string.remove_friend : R.string.add_friend);

        final int res = mPlayer.isFriend() ? R.string.remove_friend_confirm : R.string.add_friend_confirm;
        final String name = (mPlayer.getName() == null) ? getContext().getString(R.string.private_name) :
            mPlayer.getName();
        mTv.setText(getContext().getString(res, name));
    }

    public void show(final Player player) {
        mPlayer = player;

        if (mTv != null) {
            setupView();
        }
        show();
    }

    public Player getPlayer() {
        return mPlayer;
    }
}
