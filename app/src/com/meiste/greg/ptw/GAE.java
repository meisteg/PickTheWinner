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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

public final class GAE {
	
	private static final String PROD_URL = "https://ptwgame.appspot.com";
	private static final String AUTH_COOKIE_NAME = "SACSID";
	
	private Activity mActivity;
	private GaeListener mListener;
	private String mAccountName;
	private boolean mNeedInvalidate = true;
	private String mGetPage;
	
	static interface GaeListener {
        void onFailedConnect();
        void onLaunchIntent(Intent launch);
        void onConnectSuccess();
        void onGet(String json);
    }
	
	public static boolean isAccountSetupNeeded(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(EditPreferences.KEY_ACCOUNT_EMAIL, "").length() == 0;
	}
	
	public GAE(Activity activity, GaeListener listener) {
		mActivity = activity;
		mListener = listener;
	}
	
	public List<String> getGoogleAccounts() {
        ArrayList<String> result = new ArrayList<String>();
        Account[] accounts = AccountManager.get(mActivity).getAccountsByType("com.google");
        for (Account account : accounts) {
        	result.add(account.name);
        }

        return result;
    }
	
	public void connect(String account) {
    	Util.log("Connect using " + account);
    	mAccountName = account;
    	
    	// Only reset the preferences if performing new connect
    	if (mGetPage == null) {
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(EditPreferences.KEY_ACCOUNT_EMAIL, null);
			editor.putString(EditPreferences.KEY_ACCOUNT_COOKIE, null);
			editor.commit();
    	}
    	
    	// Obtain an auth token and register
        AccountManager mgr = AccountManager.get(mActivity);
        Account[] accts = mgr.getAccountsByType("com.google");
        for (Account acct : accts) {
            if (acct.name.equals(mAccountName)) {
            	mgr.getAuthToken(acct, "ah", null, mActivity, new AuthTokenCallback(), null);
            	break;
            }
        }
    }
	
	public void getPage(String page) {
		// Reset status variable in case object re-used
		mGetPage = null;
		
		Util.log("Getting page " + page);
		new GetPageTask().execute(page);
	}
	
	private class AuthTokenCallback implements AccountManagerCallback<Bundle> {
    	public void run(AccountManagerFuture<Bundle> future) {
    		try {
    			Bundle result = future.getResult();
    			
    			Intent launch = (Intent) result.get(AccountManager.KEY_INTENT);
    			if (launch != null) {
    				Util.log("Need to launch activity before getting authToken");
    				mListener.onLaunchIntent(launch);
    				return;
    			}
    			
    			String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
    			if (mNeedInvalidate) {
    				Util.log("Invalidating token and starting over");
    				mNeedInvalidate = false;
    				
    				AccountManager mgr = AccountManager.get(mActivity);
    				mgr.invalidateAuthToken("com.google", authToken);
    				connect(mAccountName);
    			} else {
	    			Util.log("authToken=" + authToken);
	    			
	    			// Phase 2: get authCookie from PTW server
	    			new GetCookieTask().execute(authToken);
    			}
    		} catch (Exception e) {
    			Util.log("Get auth token failed with exception " + e);
    			mListener.onFailedConnect();
    		}
    	}
    }
	
	private class GetCookieTask extends AsyncTask<String, Integer, Boolean> {
    	protected Boolean doInBackground(String... tokens) {
	    	String authCookie = null;
	    	
	    	try {
	    		DefaultHttpClient client = new DefaultHttpClient();
	    		String continueURL = PROD_URL;
	    		URI uri = new URI(PROD_URL + "/_ah/login?continue="
	    				+ URLEncoder.encode(continueURL, "UTF-8") + "&auth=" + tokens[0]);
	    		HttpGet method = new HttpGet(uri);
	    		final HttpParams getParams = new BasicHttpParams();
	    		HttpClientParams.setRedirecting(getParams, false);
	    		method.setParams(getParams);
	    		
	    		HttpResponse res = client.execute(method);
	    		Header[] headers = res.getHeaders("Set-Cookie");
	    		int statusCode = res.getStatusLine().getStatusCode();
	    		if (statusCode != 302 || headers.length == 0) {
	    			Util.log("Get auth cookie failed: statusCode=" + statusCode);
	    			return false;
	    		}
	    		
	    		for (Cookie cookie : client.getCookieStore().getCookies()) {
	    			if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
	    				authCookie = AUTH_COOKIE_NAME + "=" + cookie.getValue();
	    				Util.log(authCookie);
	    			}
	    		}
	    	} catch (Exception e) {
	    		Util.log("Get auth cookie failed with exception " + e);
	    		return false;
	    	}
	    	
	    	final SharedPreferences prefs =
	    			PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(EditPreferences.KEY_ACCOUNT_EMAIL, mAccountName);
			editor.putString(EditPreferences.KEY_ACCOUNT_COOKIE, authCookie);
			editor.commit();
			
			return true;
	    }
    	
    	protected void onPostExecute(Boolean success) {
    		if (success) {
    			if (mGetPage != null)
    				getPage(mGetPage);
    			else
    				mListener.onConnectSuccess();
    		} else
    			mListener.onFailedConnect();
    	}
    }
	
	private class GetPageTask extends AsyncTask<String, Integer, Boolean> {
		StringBuilder mBuilder = new StringBuilder();
		
    	protected Boolean doInBackground(String... pages) {
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
    		DefaultHttpClient client = new DefaultHttpClient();
    		HttpGet method = new HttpGet(PROD_URL + "/" + pages[0]);
    		
    		final HttpParams getParams = new BasicHttpParams();
    		HttpClientParams.setRedirecting(getParams, false);
    		method.setParams(getParams);
    		method.setHeader("Cookie", prefs.getString(EditPreferences.KEY_ACCOUNT_COOKIE, null));
    		
    		try {
        		HttpResponse resp = client.execute(method);
        		
        		switch(resp.getStatusLine().getStatusCode()) {
        		case HttpStatus.SC_OK:
        			BufferedReader reader = new BufferedReader(
        					new InputStreamReader(resp.getEntity().getContent()));
        			String line;
        			while ((line = reader.readLine()) != null) {
        				mBuilder.append(line);
        			}
        			break;
        		case HttpStatus.SC_MOVED_TEMPORARILY:
        			if (mAccountName != null) {
        				Util.log("Get page failed (status 302)");
        				return false;
        			}
        			Util.log("Cookie expired? Attempting reconnect");
        			mGetPage = pages[0];
        			connect(prefs.getString(EditPreferences.KEY_ACCOUNT_EMAIL, null));
        			break;
        		default:
        			Util.log("Get page failed (invalid status code)");
        			return false;
        		}
    		} catch (Exception e) {
    			Util.log("Get page failed with exception " + e);
    			return false;
    		}
    		
    		return true;
    	}
    	
    	protected void onPostExecute(Boolean success) {
    		if (success) {
    			if (mGetPage == null)
    				mListener.onGet(mBuilder.toString());
    		} else
    			mListener.onFailedConnect();
    	}
	}
}
