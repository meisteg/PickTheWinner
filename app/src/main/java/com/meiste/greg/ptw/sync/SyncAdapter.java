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
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;

import com.meiste.greg.ptw.GAE;
import com.meiste.greg.ptw.Race;
import com.meiste.greg.ptw.Util;
import com.meiste.greg.ptw.provider.PtwContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit.RestAdapter;
import retrofit.RetrofitError;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final int FLAG_SCHEDULE = 0x0001;

    private static final String SYNC_EXTRAS_FLAGS = "ptw_flags";
    private static final int FLAGS_ALLOWED_NO_ACCOUNT = FLAG_SCHEDULE;

    private final PtwServer mPtwServer;

    public static Account requestSync(final Context context, int flags, final boolean isUserRequested) {
        if (isUserRequested && !Util.isNetworkConnected(context)) {
            return null;
        }

        Account account;
        if (AccountUtils.isAccountSetupNeeded(context)) {
            flags &= FLAGS_ALLOWED_NO_ACCOUNT;
            if (flags > 0) {
                account = AccountUtils.getAnyAccount(context);
            } else {
                return null;
            }
        } else {
            account = AccountUtils.getPtwAccount(context);
        }

        final Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, isUserRequested);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, isUserRequested);
        b.putInt(SYNC_EXTRAS_FLAGS, flags);
        ContentResolver.requestSync(account, PtwContract.CONTENT_AUTHORITY, b);

        return account;
    }

    public SyncAdapter(final Context context, final boolean autoInitialize) {
        super(context, autoInitialize);

        final RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(GAE.PROD_URL)
                .build();
        mPtwServer = restAdapter.create(PtwServer.class);
    }

    @Override
    public void onPerformSync(final Account account, final Bundle extras, final String authority,
                              final ContentProviderClient provider, final SyncResult syncResult) {
        final int flags = extras.getInt(SYNC_EXTRAS_FLAGS);
        Util.log("Beginning network sync for " + account.name + ", flags=0x" + Integer.toHexString(flags));

        if ((flags & FLAG_SCHEDULE) != 0) {
            try {
                getSchedule(syncResult);
            } catch (final RetrofitError e) {
                Util.log("Error reading from network: " + e);
                syncResult.stats.numIoExceptions++;
            } catch (final RemoteException | OperationApplicationException e) {
                Util.log("Error updating database: " + e);
                syncResult.databaseError = true;
            }
        }

        Util.log("Network synchronization complete");
    }

    private void getSchedule(final SyncResult syncResult)
            throws RemoteException, OperationApplicationException {
        Util.log("Downloading schedule");

        final List<Race> schedule = mPtwServer.getSchedule();
        final ContentResolver contentResolver = getContext().getContentResolver();
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        // Build hash table of incoming races
        final HashMap<Integer, Race> raceMap = new HashMap<>();
        for (final Race race : schedule) {
            raceMap.put(race.getId(), race);
        }
        Util.log("Schedule downloaded. Found " + raceMap.size() + " races.");

        // Get list of all items
        final Cursor c = contentResolver.query(PtwContract.Race.CONTENT_URI,
                PtwContract.Race.PROJECTION_ALL, null, null, null);
        Util.log("Found " + c.getCount() + " local races. Computing merge solution...");

        // Find stale data
        while (c.moveToNext()) {
            syncResult.stats.numEntries++;
            final int id = c.getInt(PtwContract.Race.COLUMN_ID);
            final Race localRace = new Race(c);
            final Race remoteRace = raceMap.get(localRace.getId());
            if (remoteRace != null) {
                // Race exists. Remove from race map to prevent insert later.
                raceMap.remove(localRace.getId());
                // Check to see if the entry needs to be updated
                if (!localRace.equals(remoteRace)) {
                    // Update existing record
                    final Uri existingUri = PtwContract.Race.CONTENT_URI.buildUpon()
                            .appendPath(Integer.toString(id)).build();
                    Util.log("Scheduling update: " + existingUri);
                    final ContentProviderOperation.Builder b =
                            ContentProviderOperation.newUpdate(existingUri);
                    batch.add(getRaceOperation(b, remoteRace));
                    syncResult.stats.numUpdates++;
                }
            } else {
                // Race doesn't exist. Remove it from the database.
                final Uri deleteUri = PtwContract.Race.CONTENT_URI.buildUpon()
                        .appendPath(Integer.toString(id)).build();
                Util.log("Scheduling delete: " + deleteUri);
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();

        // Add new items
        for (final Race race : raceMap.values()) {
            Util.log("Scheduling insert: race_id=" + race.getId());
            final ContentProviderOperation.Builder b =
                    ContentProviderOperation.newInsert(PtwContract.Race.CONTENT_URI);
            batch.add(getRaceOperation(b, race));
            syncResult.stats.numInserts++;
        }

        Util.log("Merge solution ready. Applying batch update.");
        contentResolver.applyBatch(PtwContract.CONTENT_AUTHORITY, batch);
        contentResolver.notifyChange(PtwContract.Race.CONTENT_URI,
                null,   // No local observer
                false); // Do not sync to network
    }

    private ContentProviderOperation getRaceOperation(final ContentProviderOperation.Builder b,
                                                      final Race race) {
        return b.withValue(PtwContract.Race.COLUMN_NAME_RACE_ID, race.getId())
                .withValue(PtwContract.Race.COLUMN_NAME_RACE_NUM, race.getRaceNum())
                .withValue(PtwContract.Race.COLUMN_NAME_TRACK_LONG, race.getTrack(Race.NAME_LONG))
                .withValue(PtwContract.Race.COLUMN_NAME_TRACK_SHORT, race.getTrack(Race.NAME_SHORT))
                .withValue(PtwContract.Race.COLUMN_NAME_NAME, race.getName())
                .withValue(PtwContract.Race.COLUMN_NAME_TV, race.getTv())
                .withValue(PtwContract.Race.COLUMN_NAME_SIZE, race.getTrackSize())
                .withValue(PtwContract.Race.COLUMN_NAME_START, race.getStartTimestamp())
                .withValue(PtwContract.Race.COLUMN_NAME_QUESTION, race.getQuestionTimestamp())
                .withValue(PtwContract.Race.COLUMN_NAME_LAYOUT, race.getAbbr())
                .withValue(PtwContract.Race.COLUMN_NAME_CITY_STATE, race.getCityState())
                .build();
    }
}
