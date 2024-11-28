/*
 * Copyright (C) 2022-2024 - Peter Korf <peter@niendo.de>
 * Copyright (C) ?   -2022 - Axel Strübing
 * Copyright (C) ?   -2016 - Martin Carpella
 * Copyright (C) ?   -2015 - nb
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

package de.niendo.ImapNotes3.Data;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import de.niendo.ImapNotes3.ImapNotes3;

public class ImapNotesAccount {

    private static final String TAG = "IN_ImapNotesAccount";
    private static final String DEFAULT_FOLDER_NAME = "Notes";

    @NonNull
    public final String accountName;
    @NonNull
    public final String username;
    @NonNull
    public final String password;
    @NonNull
    public final String server;
    @NonNull
    public final String portnum;
    @NonNull
    public final Security security;
    public final SyncInterval syncInterval;
    @NonNull
    private final String imapfolder;
    @Nullable
    private final Account account;
    private File dirForNewFiles;
    private File dirForDeletedFiles;
    private File rootDirAccount;

    public ImapNotesAccount(@NonNull String accountName,
                            @NonNull String username,
                            @NonNull String password,
                            @NonNull String server,
                            @NonNull String portNumber,
                            @NonNull Security security,
                            @NonNull SyncInterval syncInterval,
                            @NonNull String folderName) {
        account = null;
        this.accountName = accountName;
        this.username = username;
        this.password = password;
        this.server = server;
        this.portnum = portNumber;
        this.security = security;
        this.syncInterval = syncInterval;
        this.imapfolder = folderName;
    }

    public ImapNotesAccount(@NonNull Account account,
                            @NonNull Context applicationContext) {
        this.accountName = account.name;
        rootDirAccount = ImapNotes3.GetAccountDir(accountName);
        dirForNewFiles = new File(rootDirAccount, "new");
        dirForDeletedFiles = new File(rootDirAccount, "deleted");

        this.account = account;
        AccountManager am = AccountManager.get(applicationContext);
        syncInterval = SyncInterval.from(am.getUserData(account, ConfigurationFieldNames.SyncInterval));
        username = am.getUserData(account, ConfigurationFieldNames.UserName);
        password = am.getPassword(account);
        server = am.getUserData(account, ConfigurationFieldNames.Server);
        portnum = am.getUserData(account, ConfigurationFieldNames.PortNumber);
        security = Security.from(am.getUserData(account, ConfigurationFieldNames.Security));
        imapfolder = am.getUserData(account, ConfigurationFieldNames.ImapFolder);

    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void CreateLocalDirectories() {
        Log.d(TAG, "CreateLocalDirs(String: " + accountName);
        dirForNewFiles.mkdirs();
        dirForDeletedFiles.mkdirs();
    }


    public void ClearHomeDir() {
        try {
            FileUtils.deleteDirectory(rootDirAccount);
        } catch (IOException | Error e) {
            // for anbox - otherwise it will crash
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /*
    @NonNull
    public String toString() {
        return this.accountName + ":" + this.username + ":" + this.password + ":"
                + this.server + ":" + this.portnum + ":" + this.security + ":"
                + this.usesticky + ":" + this.imapfolder + ":" + Boolean.toString(this.accountHasChanged);
    }*/
/*

    public String GetAccountName() {
        return accountName;
    }

*/
    @Nullable
    public Account GetAccount() {
        return this.account;
    }

    //public void SetAccountname(String accountName) {
    //    this.accountName = accountName;
    //}
/*

    @NonNull
    public String GetUsername() {
        return this.username;
    }

    public void SetUsername(@NonNull String Username) {
        this.username = Username;
    }
*/

  /*  @NonNull
    public String GetPassword() {
        return this.password;
    }

    public void SetPassword(@NonNull String Password) {

        this.password = Password;
    }

    @NonNull
    public String GetServer() {
        return this.server;
    }

    public void SetServer(@NonNull String Server) {
        this.server = Server;
    }
*/
  /*  @NonNull
    public String GetPortnum() {
        return this.portnum;
    }

    public void SetPortnum(@NonNull String Portnum) {

        this.portnum = Portnum;
    }

    @NonNull
    public Security GetSecurity() {
        return security;
    }

    public void SetSecurity(@NonNull Security security) {

        this.security = security;
    }

    public void SetSecurity(String security) {
        Log.d(TAG, "Set: " + security);
        SetSecurity(Security.from(security));
    }

    public boolean GetUsesticky() {
        return this.usesticky;
    }
*/
    //public void SetUsesticky(boolean Usesticky) {
    //    this.usesticky = Usesticky;
    //}

   /* public String GetSyncinterval() {
        return this.syncInterval;
    }
*/
    //public void SetSyncinterval(String Syncinterval) {
    //    this.syncInterval = Syncinterval;
    //}

    /*
    public void SetaccountHasNotChanged() {
        this.accountHasChanged = false;
    }
    */
/*

    @NonNull
    public Boolean GetaccountHasChanged() {
        return this.accountHasChanged;
    }
*/


    @NonNull
    public String GetImapFolder() {
        if (this.imapfolder.isEmpty())
            return DEFAULT_FOLDER_NAME;
        return this.imapfolder;
    }

    @NonNull
    public File GetRootDirAccount() {
        return rootDirAccount;
    }

/*

    private void SetFolderName(@NonNull String folder) {
        this.imapfolder = folder;
    }

*/

/*
    public void Clear() {
        this.username = null;
        this.password = null;
        this.server = null;
        this.portnum = null;
        this.security = Security.None;
        this.usesticky = false;
        this.imapfolder = null;
        this.accountHasChanged = false;
    }*/
}
