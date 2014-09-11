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

package com.meiste.greg.ptw.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.meiste.greg.ptw.EditPreferences;

public class AccountUtils {
    public static boolean isAccountSetupNeeded(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String account = prefs.getString(EditPreferences.KEY_ACCOUNT_EMAIL, "");

        if (account.length() == 0) {
            // Account not setup at all
            return true;
        }

        final AccountManager mgr = AccountManager.get(context);
        final Account[] accts = mgr.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        for (final Account acct : accts) {
            if (acct.name.equals(account)) {
                // Account setup and found on system
                return false;
            }
        }

        // Account setup, but no longer present on system
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(EditPreferences.KEY_ACCOUNT_EMAIL, null);
        editor.putString(EditPreferences.KEY_ACCOUNT_COOKIE, null);
        editor.apply();

        return true;
    }

    public static Account getPtwAccount(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String account = prefs.getString(EditPreferences.KEY_ACCOUNT_EMAIL, "");
        final AccountManager mgr = AccountManager.get(context);
        final Account[] accounts = mgr.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

        for (final Account acct : accounts) {
            if (acct.name.equals(account)) {
                return acct;
            }
        }

        return null;
    }

    public static Account getAnyAccount(final Context context) {
        final AccountManager mgr = AccountManager.get(context);
        final Account[] accounts = mgr.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

        if (accounts.length > 0) {
            return accounts[0];
        }

        return null;
    }
}
