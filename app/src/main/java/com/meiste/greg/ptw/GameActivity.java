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
package com.meiste.greg.ptw;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdRequest.Builder;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.tagmanager.Container;
import com.meiste.greg.ptw.GtmHelper.OnContainerAvailableListener;
import com.meiste.greg.ptw.gcm.Gcm;
import com.meiste.greg.ptw.iab.IabHelper;
import com.meiste.greg.ptw.iab.IabResult;
import com.meiste.greg.ptw.iab.Inventory;
import com.meiste.greg.ptw.iab.Purchase;

public class GameActivity extends BaseActivity implements Eula.OnEulaAgreedTo, OnContainerAvailableListener {

    private static final String LAST_TAB = "tab.last";
    private static final String SKU_AD_FREE = "ad_free";
    private static final int IAB_REQUEST = 10001;
    private static final int GPS_REQUEST = 9000;

    private Container mContainer;
    private IabHelper mHelper;
    private ViewPager mPager;
    private AdView mAdView;
    private Dialog mDialog;
    private boolean mIsAdFree = false;
    private boolean mIabReady = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This activity is always started via the MainActivity, which ensures
        // the container is loaded before starting this activity. So the call
        // to getContainer() here should immediately call onContainerAvailable().
        GtmHelper.getInstance(getApplicationContext()).getContainer(this);
    }

    @Override
    public void onContainerAvailable(final Container container) {
        mContainer = container;

        mHelper = new IabHelper(this, PTW.PUB_KEY);
        mHelper.enableDebugLogging(BuildConfig.DEBUG, PTW.TAG);

        // This has to be called before setContentView
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.main);

        // Need to explicitly set to false else it will incorrectly appear on
        // older Android versions. Must be after setContentView.
        setSupportProgressBarIndeterminateVisibility(false);

        if (Eula.show(this))
            onEulaAgreedTo();

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(2);
        mPager.setAdapter(new TabFragmentAdapter(getSupportFragmentManager(), this));
        mPager.setCurrentItem(getTab(getIntent()));

        final PagerTabStrip tabs = (PagerTabStrip) findViewById(R.id.pager_title_strip);
        tabs.setTabIndicatorColorResource(R.color.tab_footer);
        tabs.setDrawFullUnderline(true);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        Util.log(GameActivity.class.getSimpleName() + ".onNewIntent: " + intent);

        setIntent(intent);
        mPager.setCurrentItem(getTab(intent));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mAdView != null) {
            mAdView.pause();
        }

        Util.log("Saving state: tab=" + mPager.getCurrentItem());
        Util.getState(this).edit().putInt(LAST_TAB, mPager.getCurrentItem()).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAdView != null) {
            mAdView.resume();
        }

        if (checkPlayServices()) {
            Gcm.registerIfNeeded(getApplicationContext());
        }
    }

    @Override
    public void onDestroy() {
        // Hide dialogs to prevent window leaks on orientation changes
        Eula.hide();
        if ((mDialog != null) && (mDialog.isShowing())) {
            mDialog.dismiss();
        }

        if (mAdView != null) {
            mAdView.removeAllViews();
            mAdView.destroy();
            mAdView = null;
        }

        if (mHelper != null) {
            try {
                mHelper.dispose();
            } catch (final IllegalArgumentException e) {
                // If IabHelper never managed to connect to billing service
                // (likely because the Play Store version is too old), then
                // calling dispose will cause an exception when it tries to
                // unbind from the service it never connected to. Working
                // around issue here instead of fixing Google code.
                Util.log("Error when disposing IabHelper: " + e);
            }
            mHelper = null;
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu, menu);

        if (!mIsAdFree && mIabReady && mContainer.getBoolean(GtmHelper.KEY_ALLOW_REMOVE_ADS)) {
            menu.add(Menu.NONE, R.string.ads_remove, Menu.NONE, R.string.ads_remove)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }

        if (Util.isGooglePlusInstalled(this) &&
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)) {
            menu.add(Menu.NONE, R.string.google_plus, Menu.NONE, R.string.google_plus)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.settings:
            startActivity(new Intent(this, EditPreferences.class));
            return true;

        case R.id.legal:
            trackEvent("onOptionsItemSelected", "legal");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.legal);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.ok, null);
            builder.setMessage(R.string.legal_content);
            mDialog = builder.create();
            mDialog.show();
            return true;

        case R.string.google_plus:
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://plus.google.com/109696002032953241222/posts")));
            return true;

        case R.string.ads_remove:
            try {
                mHelper.launchPurchaseFlow(this, SKU_AD_FREE, IAB_REQUEST, mPurchaseFinishedListener);
                trackEvent("onOptionsItemSelected", "ads_remove");
            } catch (final IllegalStateException e) {
                // Can be caused by user double clicking option item
                Util.log("Unable to launch purchase flow: " + e);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // Not handled by in-app billing
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onEulaAgreedTo() {
        RaceAlarm.set(this);
        QuestionAlarm.set(this);

        try {
            mHelper.startSetup(mIabSetupListener);
        } catch (final NullPointerException e) {
            // Happens when the Play Store updates at this exact moment.
            // IabHelper cannot handle that use case. For now, do nothing.
            Util.log("Unable to start IAB setup!");
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                mDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        GPS_REQUEST, new OnCancelListener() {
                    @Override
                    public void onCancel(final DialogInterface dialog) {
                        finish();
                    }
                });
                mDialog.show();
            } else {
                Util.log("This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private int getTab(final Intent intent) {
        // Recent applications caches intent with extras. Only want to listen
        // to INTENT_TAB extra if launched from notification.
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            final int intent_tab = intent.getIntExtra(PTW.INTENT_EXTRA_TAB, -1);
            if (intent_tab >= 0) {
                return intent_tab;
            }
        }

        return Util.getState(this).getInt(LAST_TAB, 0);
    }

    private void loadAd() {
        final Builder adReqBuilder = new AdRequest.Builder();
        adReqBuilder.addKeyword("NASCAR");
        adReqBuilder.addKeyword("racing");

        if (BuildConfig.DEBUG) {
            adReqBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
            adReqBuilder.addTestDevice("CB529BCBD1E778FAD10EE145EE29045F"); // Atrix 4G
            adReqBuilder.addTestDevice("36A52B9CBB347B995EA40ACDD0D36376"); // XOOM
            adReqBuilder.addTestDevice("E64392AEFC7C9A13D2A6A76E9EA034C4"); // RAZR
        }

        final String adUnitId = mContainer.getString(GtmHelper.KEY_AD_ID);
        if (!TextUtils.isEmpty(adUnitId) && (mAdView == null) && !ActivityManager.isUserAMonkey()) {
            mAdView = new AdView(this);
            mAdView.setAdListener(mAdListener);
            mAdView.setAdUnitId(adUnitId);
            mAdView.setAdSize(Util.str2AdSize(mContainer.getString(GtmHelper.KEY_AD_SIZE)));
            mAdView.setBackgroundResource(R.color.ad_background);
            mAdView.loadAd(adReqBuilder.build());
        }
    }

    private void trackEvent(final String action, final String label) {
        Analytics.trackEvent(this, "Main", action, label);
    }

    private final AdListener mAdListener = new AdListener() {
        @Override
        public void onAdLoaded() {
            Util.log("onAdLoaded");
            final LinearLayout l = (LinearLayout) findViewById(R.id.main_layout);
            if ((mAdView != null) && (mAdView.getParent() == null)) {
                l.addView(mAdView);
            }
        }

        @Override
        public void onAdFailedToLoad(final int errorCode) {
            Util.log("onAdFailedToLoad: " + errorCode);
        }
    };

    private final IabHelper.OnIabSetupFinishedListener mIabSetupListener =
            new IabHelper.OnIabSetupFinishedListener() {
        @Override
        public void onIabSetupFinished(final IabResult result) {
            if (result.isSuccess()) {
                if (mHelper != null)
                    mHelper.queryInventoryAsync(false, mGotInventoryListener);
            } else {
                Util.log("Problem setting up in-app billing: " + result);
                trackEvent("onIabSetupFinished", result.getMessage());
                loadAd();
            }
        }
    };

    private final IabHelper.QueryInventoryFinishedListener mGotInventoryListener =
            new IabHelper.QueryInventoryFinishedListener() {
        @SuppressLint("NewApi")
        @Override
        public void onQueryInventoryFinished(final IabResult result, final Inventory inventory) {
            if (result.isFailure()) {
                trackEvent("onQueryInventoryFinished", result.getMessage());
                loadAd();
                return;
            }

            mIsAdFree = inventory.hasPurchase(SKU_AD_FREE);
            mIabReady = true;
            invalidateOptionsMenu();
            if (!mIsAdFree) loadAd();
        }
    };

    private final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
            new IabHelper.OnIabPurchaseFinishedListener() {
        @SuppressLint("NewApi")
        @Override
        public void onIabPurchaseFinished(final IabResult result, final Purchase purchase) {
            if (result.isFailure()) {
                final String err = IabHelper.getResponseDesc(result.getResponse());
                final String msg = GameActivity.this.getString(R.string.ads_error, err);
                Toast.makeText(GameActivity.this, msg, Toast.LENGTH_SHORT).show();
                trackEvent("onIabPurchaseFinished", err);
                return;
            }

            if (purchase.getSku().equals(SKU_AD_FREE)) {
                Toast.makeText(GameActivity.this, R.string.ads_success, Toast.LENGTH_LONG).show();
                trackEvent("onIabPurchaseFinished", "success");
                mIsAdFree = true;
                invalidateOptionsMenu();

                if (mAdView != null) {
                    mAdView.setVisibility(View.GONE);
                    mAdView.removeAllViews();
                    mAdView.destroy();
                    mAdView = null;
                }
            }
        }
    };
}