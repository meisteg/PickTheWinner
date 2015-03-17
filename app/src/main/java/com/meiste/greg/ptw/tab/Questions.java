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
package com.meiste.greg.ptw.tab;

import java.util.Calendar;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.meiste.greg.ptw.Analytics;
import com.meiste.greg.ptw.GAE;
import com.meiste.greg.ptw.GAE.GaeListener;
import com.meiste.greg.ptw.GameActivity;
import com.meiste.greg.ptw.PTW;
import com.meiste.greg.ptw.PlayerAdapter;
import com.meiste.greg.ptw.PlayerHistory;
import com.meiste.greg.ptw.QuestionAlarm;
import com.meiste.greg.ptw.QuestionsRaceAdapter;
import com.meiste.greg.ptw.R;
import com.meiste.greg.ptw.Race;
import com.meiste.greg.ptw.Util;
import com.meiste.greg.ptw.sync.AccountUtils;

public final class Questions extends TabFragment implements GaeListener {

    public final static String QCACHE = "question_cache";
    public final static String ACACHE = "answer_cache";
    public final static String CACACHE = "correct_answer_cache";

    private boolean mSetupNeeded;
    private boolean mChanged = false;
    private Race mRaceNext;
    private Race mRaceSelected = null;
    private boolean mFailedConnect = false;
    private boolean mSending = false;
    private long mSubFragmentTime = 0;
    private long mAccountSetupTime = 0;
    private Spinner mRaceSpinner = null;
    private ListView mRaceList = null;
    private QuestionsRaceAdapter mRaceAdapter;

    public static String cachePrefix() {
        return Calendar.getInstance().get(Calendar.YEAR) + "_race";
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        mRaceNext = Race.getNext(getActivity(), false, true);
        mSetupNeeded = AccountUtils.isAccountSetupNeeded(getActivity());
        mChanged = false;
        mRaceAdapter = new QuestionsRaceAdapter(getActivity());
        mAccountSetupTime = Util.getAccountSetupTime(getActivity());
        setRetainInstance(true);

        if ((mRaceSelected == null) && (mRaceAdapter.getCount() > 0)) {
            mRaceSelected = mRaceAdapter.getItem(mRaceAdapter.getCount() - 1);
        }

        if (mSetupNeeded) {
            return Util.getAccountSetupView(getActivity(), inflater, container);
        } else if (mRaceSelected == null) {
            return inflater.inflate(R.layout.questions_no_race, container, false);
        }

        final View v = inflater.inflate(R.layout.questions, container, false);
        final int position = mRaceAdapter.getPosition(mRaceSelected);
        mRaceSpinner = (Spinner) v.findViewById(R.id.race_spinner);
        if (mRaceSpinner != null) {
            mRaceSpinner.setAdapter(mRaceAdapter);
            mRaceSpinner.setSelection(position);
            mRaceSpinner.setOnItemSelectedListener(new RaceSelectedListener());
        } else {
            mRaceList = (ListView) v.findViewById(R.id.race_list);
            if (mRaceList != null) {
                mRaceList.setAdapter(mRaceAdapter);
                mRaceList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                mRaceList.setSelection(position);
                mRaceList.setItemChecked(position, true);
                mRaceList.setOnItemClickListener(new RaceSelectedListener());
            }
        }

        setSubFragment();
        return v;
    }

    private String getJson(final String from) {
        final SharedPreferences cache = getActivity().getSharedPreferences(from, Activity.MODE_PRIVATE);
        return cache.getString(cachePrefix() + mRaceSelected.getId(), null);
    }

    public void setSubFragment() {
        mSubFragmentTime = System.currentTimeMillis();

        // Sanity check to verify time has not gone backwards (must likely due
        // to user manually setting back time). This is needed so the check in
        // onResume() will not continuously (and incorrectly) set mChanged=true
        if (mSubFragmentTime < PlayerHistory.getTime(getActivity())) {
            Util.log("Reseting player history time to " + mSubFragmentTime);
            PlayerHistory.setTime(getActivity(), mSubFragmentTime);
        }

        boolean selectEnable = true;
        final Fragment f;
        final String ftag;

        if (mSending) {
            selectEnable = false;
            ftag = QuestionsConnecting.class.getName();
            f = QuestionsConnecting.getInstance(getChildFragmentManager(), ftag);
        } else if (mFailedConnect) {
            mFailedConnect = false;
            ftag = QuestionsConnectFail.class.getName();
            f = QuestionsConnectFail.getInstance(getChildFragmentManager(), ftag);
        } else if (mRaceSelected.isRecent() && getJson(QCACHE) != null && getJson(ACACHE) == null) {
            ftag = QuestionsLate.class.getName();
            f = QuestionsLate.getInstance(getChildFragmentManager(), ftag);
        } else if (mRaceSelected.inProgress() || !mRaceSelected.isFuture()) {
            final String qjson = getJson(QCACHE);
            if (qjson == null) {
                selectEnable = false;
                ftag = QuestionsConnecting.class.getName();
                f = QuestionsConnecting.getInstance(getChildFragmentManager(), ftag);
                GAE.getInstance(getActivity()).getPage(this, "questions");
            } else {
                final String ajson = getJson(ACACHE);
                if (ajson == null) {
                    ftag = qjson;
                    f = QuestionsForm.getInstance(getChildFragmentManager(), qjson);
                } else {
                    final String cajson = getJson(CACACHE);
                    if (cajson == null) {
                        final int raceAfterNum = new PlayerAdapter(getActivity()).getRaceAfterNum();
                        if (raceAfterNum >= mRaceSelected.getId()) {
                            // Standings are available for race, so correct answers
                            // should be available for download.
                            ((GameActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(true);
                            selectEnable = false;
                            GAE.getInstance(getActivity()).getPage(
                                    new CorrectAnswersListener(mRaceSelected.getId()),
                                    "answers?year=" + mRaceSelected.getStartYear() +
                                    "&race_id=" + mRaceSelected.getId());
                        }
                    }

                    ftag = QuestionsAnswers.getTag(qjson, ajson, cajson);
                    f = QuestionsAnswers.getInstance(getChildFragmentManager(), qjson, ajson, cajson);
                }
            }
        } else {
            ftag = QuestionsNotYet.getTag(mRaceSelected);
            f = QuestionsNotYet.getInstance(getChildFragmentManager(), mRaceSelected);
        }

        try {
            getChildFragmentManager().beginTransaction().replace(R.id.race_questions, f, ftag).commit();
        } catch (final IllegalStateException e) {
            // This can occur if the user presses HOME or BACK during transaction
            // with server. When server response is received, the asynchronous
            // callback will try to update the fragment and fail, so set changed
            // flag so UI is updated on next resume.
            mChanged = true;
            Util.log("Questions fragment commit failed. Setting changed flag instead.");
        }

        if (mRaceSpinner != null) {
            mRaceSpinner.setEnabled(selectEnable);
        } else if (mRaceList != null) {
            mRaceList.setEnabled(selectEnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final IntentFilter filter = new IntentFilter(PTW.INTENT_ACTION_SCHEDULE);
        filter.addAction(PTW.INTENT_ACTION_RACE_ALARM);
        filter.addAction(PTW.INTENT_ACTION_HISTORY);
        getActivity().registerReceiver(mBroadcastReceiver, filter);

        if ((mRaceSelected != null) && mRaceSelected.inProgress()) {
            QuestionAlarm.clearNotification(getActivity().getApplicationContext());
        }

        // Check if user changed their account status
        mChanged |= mSetupNeeded != AccountUtils.isAccountSetupNeeded(getActivity());
        // Check if user has switched accounts
        mChanged |= mAccountSetupTime != Util.getAccountSetupTime(getActivity());

        if (mSubFragmentTime > 0) {
            // Check if user submitted answers on a different device
            mChanged |= mSubFragmentTime < PlayerHistory.getTime(getActivity());
            // Check if app has been running for awhile and should be refreshed
            mChanged |= (System.currentTimeMillis() - mSubFragmentTime) > DateUtils.HOUR_IN_MILLIS;
        }
        if (mRaceNext != null) {
            if (mSubFragmentTime > 0) {
                // See if race questions are now available but weren't previously
                mChanged |= (mSubFragmentTime < mRaceNext.getQuestionTimestamp()) && mRaceNext.inProgress();
            }
            // Check if questions form needs to disappear because race started
            mChanged |= !mRaceNext.isFuture();
        }

        if (mChanged) {
            Util.log("Questions: onResume: notifyChanged");
            resetRaceSelected();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public boolean isChanged() {
        return mChanged;
    }

    @Override
    protected void notifyChanged() {
        mChanged = true;
        super.notifyChanged();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Util.log("Questions.onReceive: " + intent.getAction());
            mChanged = true;
            resetRaceSelected();
        }
    };

    private void resetRaceSelected() {
        final int lastPos = mRaceAdapter.getCount() - 1;

        // Workaround to ensure the spinner is set back to next race
        if ((mRaceSpinner != null) && (mRaceAdapter.getCount() > 0) &&
                (mRaceSpinner.getSelectedItemPosition() != lastPos)) {
            // Making new selection causes notifyChanged to be called by listener
            mRaceSpinner.setSelection(lastPos);
        } else if ((mRaceList != null) && (mRaceAdapter.getCount() > 0) &&
                (mRaceList.getSelectedItemPosition() != lastPos)) {
            mRaceList.performItemClick(null, lastPos, 0);
            mRaceList.smoothScrollToPosition(lastPos);
            mRaceList.setItemChecked(lastPos, true);
        } else {
            notifyChanged();
        }
    }

    private class RaceSelectedListener implements OnItemSelectedListener, OnItemClickListener {
        @Override
        public void onItemSelected(final AdapterView<?> parent, final View v, final int pos, final long id) {
            final Race race = (Race) parent.getItemAtPosition(pos);
            if (mRaceSelected.getId() != race.getId()) {
                mRaceSelected = race;
                Util.log("Questions: Selected race = " + mRaceSelected.getId());
                if (mChanged) {
                    notifyChanged();
                } else {
                    setSubFragment();
                }
            }
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {}

        @Override
        public void onItemClick(final AdapterView<?> parent, final View v, final int pos, final long id) {
            onItemSelected(parent, v, pos, id);
        }
    }

    private class CorrectAnswersListener implements GaeListener {
        private final int raceId;

        public CorrectAnswersListener(final int id) {
            raceId = id;
        }

        @Override
        public void onFailedConnect(final Context context) {
            Util.log("CorrectAnswersListener: onFailedConnect");

            if (mRaceSpinner != null) {
                mRaceSpinner.setEnabled(true);
            } else if (mRaceList != null) {
                mRaceList.setEnabled(true);
            }

            // Verify application wasn't closed before callback returned
            if (getActivity() != null) {
                ((GameActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(false);
            }
        }

        @Override
        public void onGet(final Context context, final String json) {
            Util.log("CorrectAnswersListener: onGet: " + json);

            final SharedPreferences cache = context.getSharedPreferences(CACACHE, Activity.MODE_PRIVATE);
            cache.edit().putString(cachePrefix() + raceId, json).apply();

            if (mRaceSpinner != null) {
                mRaceSpinner.setEnabled(true);
            } else if (mRaceList != null) {
                mRaceList.setEnabled(true);
            }

            // Verify application wasn't closed before callback returned
            if (getActivity() != null) {
                ((GameActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(false);
                setSubFragment();
            }
        }

        @Override
        public void onConnectSuccess(final Context context, final String json) {}

        @Override
        public void onLaunchIntent(final Intent launch) {}
    }

    public void onSubmitAnswers(final String json) {
        mSending = true;
        setSubFragment();

        GAE.getInstance(getActivity()).postPage(this, "questions", json);
        Analytics.trackEvent(getActivity(), "Questions", "button", "send");
    }

    @Override
    public void onFailedConnect(final Context context) {
        Util.log("Questions: onFailedConnect");

        // Verify application wasn't closed before callback returned
        if (getActivity() != null) {
            mSending = false;
            mFailedConnect = true;
            setSubFragment();
        }
    }

    @Override
    public void onGet(final Context context, final String json) {
        Util.log("Questions: onGet: " + json);

        final SharedPreferences cache = context.getSharedPreferences(QCACHE, Activity.MODE_PRIVATE);
        cache.edit().putString(cachePrefix() + mRaceNext.getId(), json).apply();

        // Verify application wasn't closed before callback returned
        if (getActivity() != null) {
            setSubFragment();
        }
    }

    @Override
    public void onConnectSuccess(final Context context, final String json) {
        Util.log("Questions: onConnectSuccess: " + json);
        Toast.makeText(context, R.string.questions_success, Toast.LENGTH_SHORT).show();

        final SharedPreferences cache = context.getSharedPreferences(ACACHE, Activity.MODE_PRIVATE);
        cache.edit().putString(cachePrefix() + mRaceSelected.getId(), json).apply();

        context.sendBroadcast(new Intent(PTW.INTENT_ACTION_ANSWERS));

        // Verify application wasn't closed before callback returned
        if (getActivity() != null) {
            mSending = false;
            setSubFragment();
        }
    }

    @Override
    public void onLaunchIntent(final Intent launch) {}
}
