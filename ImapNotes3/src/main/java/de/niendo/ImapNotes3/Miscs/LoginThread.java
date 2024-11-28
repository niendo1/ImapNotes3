/*
 * Copyright (C) 2022-2024 - Peter Korf <peter@niendo.de>
 * Copyright (C)           - kwhitefoot
 * and Contributors.
 *
 * This file is part of ImapNotes3.
 *
 * ImapNotes3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.niendo.ImapNotes3.Miscs;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.SyncRequest;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import de.niendo.ImapNotes3.AccountConfigurationActivity;
import de.niendo.ImapNotes3.Data.ConfigurationFieldNames;
import de.niendo.ImapNotes3.Data.ImapNotesAccount;
import de.niendo.ImapNotes3.R;
import de.niendo.ImapNotes3.Sync.SyncUtils;

public class LoginThread extends AsyncTask<Void, Void, Result<String>> {
    private static final String TAG = "LoginThread";
    private static final int THREAD_ID = 0xF00D;
    private final de.niendo.ImapNotes3.Data.ImapNotesAccount ImapNotesAccount;

    private final AccountConfigurationActivity accountConfigurationActivity;

    private final AccountConfigurationActivity.Actions action;
    private final FinishListener listener;

    public LoginThread(ImapNotesAccount ImapNotesAccount,
                       AccountConfigurationActivity accountConfigurationActivity,
                       AccountConfigurationActivity.Actions action) {
        this.ImapNotesAccount = ImapNotesAccount;
        this.listener = accountConfigurationActivity;
        this.accountConfigurationActivity = accountConfigurationActivity;
        this.action = action;
    }

    @NonNull
    protected Result<String> doInBackground(Void... none) {
        Log.d(TAG, "doInBackground");
        boolean newFolder = false;
        try {
            SyncUtils syncUtils = new SyncUtils();
            ImapNotesResult res = syncUtils.ConnectToRemote(
                    ImapNotesAccount.username,
                    ImapNotesAccount.password,
                    ImapNotesAccount.server,
                    ImapNotesAccount.portnum,
                    ImapNotesAccount.security,
                    ImapNotesAccount.GetImapFolder(),
                    THREAD_ID
            );
            if (res.returnCode == ImapNotesResult.ResultCodeImapFolderCreated) {
                newFolder = true;
            } else if (res.returnCode != ImapNotesResult.ResultCodeSuccess) {
                Log.d(TAG, "doInBackground IMAP Failed");
                return new Result<>(res.errorMessage, false);
            }

            final Account account = new Account(ImapNotesAccount.accountName, Utilities.PackageName);
            final AccountManager am = AccountManager.get(accountConfigurationActivity);
            accountConfigurationActivity.setResult(AccountConfigurationActivity.TO_REFRESH);
            if (newFolder) {
                // Database and folder not valid..get Data from new directory
                SyncUtils.SetUIDValidity(account, -1L, accountConfigurationActivity);
            }
            int resultTxtId;
            if (action == AccountConfigurationActivity.Actions.EDIT_ACCOUNT) {
                resultTxtId = R.string.account_modified;
            } else {
                if (!am.addAccountExplicitly(account, ImapNotesAccount.password, null)) {
                    return new Result<>(accountConfigurationActivity.getString(R.string.account_already_exists_or_is_null), false);
                }
                resultTxtId = R.string.account_added;
            }

            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            accountConfigurationActivity.setAccountAuthenticatorResult(result);
            setUserData(am, account);
            // Run the Sync Adapter Periodically
            ContentResolver.setIsSyncable(account, AccountConfigurationActivity.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, AccountConfigurationActivity.AUTHORITY, true);
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().syncPeriodic(ImapNotesAccount.syncInterval.time * 60L, ImapNotesAccount.syncInterval.time * 60L)
                    .setSyncAdapter(account, AccountConfigurationActivity.AUTHORITY).setExtras(new Bundle()).build();
            if (ImapNotesAccount.syncInterval.time > 0) {
                ContentResolver.requestSync(request);
            } else {
                ContentResolver.cancelSync(request);
            }

            Log.d(TAG, "doInBackground success");
            return new Result<>(accountConfigurationActivity.getString(resultTxtId), true);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result<>("Unexpected exception: " + e.getMessage(), false);
        }
    }

    private void setUserData(@NonNull AccountManager am,
                             @NonNull Account account) {
        am.setUserData(account, ConfigurationFieldNames.UserName, ImapNotesAccount.username);
        am.setUserData(account, ConfigurationFieldNames.Server, ImapNotesAccount.server);
        am.setUserData(account, ConfigurationFieldNames.PortNumber, ImapNotesAccount.portnum);
        am.setUserData(account, ConfigurationFieldNames.SyncInterval, ImapNotesAccount.syncInterval.name());
        am.setUserData(account, ConfigurationFieldNames.Security, ImapNotesAccount.security.name());
        am.setUserData(account, ConfigurationFieldNames.ImapFolder, ImapNotesAccount.GetImapFolder());
    }

    protected void onPostExecute(@NonNull Result<String> result) {
        TrafficStats.clearThreadStatsTag();
        listener.onFinishPerformed(result);
    }

    public interface FinishListener {
        void onFinishPerformed(@NonNull Result<String> result);
    }

}

