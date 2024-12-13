/*
 * Copyright (C) 2022-2024 - Peter Korf <peter@niendo.de>
 * Copyright (C)      2016 - Axel Strübing
 * Copyright (C)      2016 - Martin Carpella
 * Copyright (C) 2015-2016 - nb
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

package de.niendo.ImapNotes3.Sync;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.net.TrafficStats;
import android.util.Log;

import de.niendo.ImapNotes3.Data.NotesDb;
import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.Data.Security;
import de.niendo.ImapNotes3.ImapNotes3;
import de.niendo.ImapNotes3.ListActivity;
import de.niendo.ImapNotes3.Miscs.HtmlNote;
import de.niendo.ImapNotes3.Miscs.ImapNotesResult;

import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.util.MailSSLSocketFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;

import de.niendo.ImapNotes3.Miscs.Utilities;


public class SyncUtils {

    private static final String TAG = "IN_SyncUtils";
    private final Object myLock = new Object();
    // TODO: Why do we have two folder fields and why are they both nullable?
    private Store store;
    @Nullable
    private IMAPFolder remoteIMAPNotesFolder;
    private String copyImapFolderName;
    private Long UIDValidity;

    /**
     * Do we really need the Context argument or could we call getApplicationContext instead?
     *
     * @param rootDirAccount Name of the account as defined by the user, this is not the email address.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void CreateLocalDirectories(@NonNull File rootDirAccount) {
        Log.d(TAG, "CreateDirs(String: " + rootDirAccount);
        (new File(rootDirAccount, "new")).mkdirs();
        (new File(rootDirAccount, "deleted")).mkdirs();
    }

    @NonNull
    public ImapNotesResult ConnectToRemote(@NonNull String username,
                                           @NonNull String password,
                                           @NonNull String server,
                                           String portnum,
                                           @NonNull Security security,
                                           @NonNull String ImapFolderName,
                                           @NonNull String copyImapFolderName,
                                           int threadID
    ) {
        Log.d(TAG, "ConnectToRemote: " + username);

        this.copyImapFolderName = copyImapFolderName;

        TrafficStats.setThreadStatsTag(threadID);

        //final ImapNotesResult res = new ImapNotesResult();
        if (IsConnected()) {
            try {
                store.close();
            } catch (MessagingException e) {
                // Log the error but do not propagate the exception because the connection is now
                // closed even if an exception was thrown.
                Log.d(TAG, "ConnectToRemote Store.Close(): " + e.getMessage());
            }
        }
        //boolean acceptcrt = security.acceptcrt;

        MailSSLSocketFactory sf;
        try {
            sf = new MailSSLSocketFactory();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return new ImapNotesResult(ImapNotesResult.ResultCodeCantConnect,
                    "Can't connect to server: " + e.getMessage(), -1);
        }
        Properties props = new Properties();

        String proto = security.proto;
        props.setProperty(String.format("mail.%s.host", proto), server);
        props.setProperty(String.format("mail.%s.port", proto), portnum);
        props.setProperty("mail.store.protocol", proto);
        if (security.acceptcrt) {
            sf.setTrustedHosts(server);
            if (proto.equals("imap")) {
                props.put("mail.imap.ssl.socketFactory", sf);
                props.put("mail.imap.starttls.enable", "true");
            }
        } else if (security != Security.None) {
            props.put(String.format("mail.%s.ssl.checkserveridentity", proto), "true");
            if (proto.equals("imap")) {
                props.put("mail.imap.starttls.enable", "true");
            }
        }

        // FIXME: spelling error?
        if (proto.equals("imaps")) {
            props.put("mail.imaps.socketFactory", sf);
        }
        props.setProperty("mail.imap.connectiontimeout", "1000");

        /*
        TODO: use user defined proxy.
        Boolean useProxy = false;
        if (useProxy) {
            props.put("mail.imap.socks.host", "10.0.2.2");
            props.put("mail.imap.socks.port", "1080");
        }
         */
        try {
            Session session = Session.getInstance(props, null);
            //session.setDebug(true);
            store = session.getStore(proto);
            store.connect(server, username, password);
            //res.hasUIDPLUS = ((IMAPStore) store).hasCapability("UIDPLUS");
            //Log.d(TAG, "has UIDPLUS="+res.hasUIDPLUS);

            Folder[] folders = store.getPersonalNamespaces();
            Folder rootFolder = folders[0];
            Log.d(TAG, "Personal Namespaces=" + rootFolder.getFullName());
            // TODO: this the wrong place to make decisions about the name of the notes folder, that
            // should be done where it is created.
            String sfolder = ImapFolderName;
            if (!rootFolder.getFullName().isEmpty()) {
                char separator = rootFolder.getSeparator();
                sfolder = rootFolder.getFullName() + separator + ImapFolderName;
            }
            // Get UIDValidity
            remoteIMAPNotesFolder = (IMAPFolder) store.getFolder(sfolder);
            if (!remoteIMAPNotesFolder.exists()) {
                if (remoteIMAPNotesFolder.create(Folder.HOLDS_MESSAGES)) {
                    remoteIMAPNotesFolder.setSubscribed(true);
                    Log.d(TAG, "Folder was created successfully");
                    return new ImapNotesResult(ImapNotesResult.ResultCodeImapFolderCreated,
                            "", -1);
                } else {
                    Exception e = new Exception("ImapFolder on server not found and could not created");
                    throw new RuntimeException(e);
                }
            }
            return new ImapNotesResult(ImapNotesResult.ResultCodeSuccess,
                    "",
                    remoteIMAPNotesFolder.getUIDValidity());
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            return new ImapNotesResult(ImapNotesResult.ResultCodeException,
                    e.getMessage(),
                    -1);
        }

    }

    // Put values in shared preferences
    synchronized public static void SetUIDValidity(@NonNull Account account,
                                            Long UIDValidity,
                                            @NonNull Context ctx) {
        Log.d(TAG, "SetUIDValidity: " + account.name);
        SharedPreferences preferences = ctx.getSharedPreferences(ImapNotes3.RemoveReservedChars(account.name), Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Name", "valid_data");
        //Log.d(TAG, "UIDValidity set to in shared_prefs:"+UIDValidity);
        editor.putLong("UIDValidity", UIDValidity);
        editor.apply();
    }

    synchronized private boolean IsConnected() {
        return store != null && store.isConnected();
    }

    // Retrieve values from shared preferences:
    synchronized Long GetUIDValidity(@NonNull Account account,
                                            @NonNull Context ctx) {
        Log.d(TAG, "GetUIDValidity: " + account.name);
        UIDValidity = (long) -1;
        SharedPreferences preferences = ctx.getSharedPreferences(ImapNotes3.RemoveReservedChars(account.name), Context.MODE_MULTI_PROCESS);
        String name = preferences.getString("Name", "");
        if (!name.equalsIgnoreCase("")) {
            UIDValidity = preferences.getLong("UIDValidity", -1);
            //Log.d(TAG, "UIDValidity got from shared_prefs:"+UIDValidity);
        }
        return UIDValidity;
    }

    /**
     * @param uid     ID of the message as created by the IMAP server
     * @param fileDir Name of the account with which this message is associated, used to find the
     *                directory in which it is stored.
     * @return A Java mail message object.
     */
    @Nullable
    public static Message ReadMailFromFileRootAndNew(@NonNull String uid,
                                                     @NonNull File fileDir) {
        Log.d(TAG, "ReadMailFromFileRootAndNew: " + fileDir.getPath() + " " + uid);

        // new or changed file
        if (uid.startsWith("-")) {
            uid = uid.substring(1);
            fileDir = new File(fileDir, "new");
        }
        return ReadMailFromNoteFile(fileDir, uid);
    }

    /**
     * @param uid     ID of the message as created by the IMAP server
     * @param nameDir Name of the account with which this message is associated, used to find the
     *                directory in which it is stored.
     * @return A Java mail message object.
     */
    @Nullable
    public static Message ReadMailFromNoteFile(@NonNull File nameDir,
                                               @NonNull String uid) {
        Log.d(TAG, "ReadMailFromFile: " + nameDir.getPath() + " " + Utilities.addMailExt(uid));

        File mailFile;
        mailFile = new File(nameDir, Utilities.addMailExt(uid));
        if (!mailFile.exists()) {
            // old: only UID is used
            mailFile = new File(nameDir, uid);
            if (!mailFile.exists()) {
                mailFile = new File(nameDir, uid);
                if (!mailFile.exists()) {
                    Log.d(TAG, "ReadMailFromFile: file not found.." + mailFile);
                    return null;
                }
            }
        }
        return (ReadMailFromFile(mailFile));
    }

    /**
     * @param mailFile Name of the account with which this message is associated, used to find the
     *                 directory in which it is stored.
     * @return A Java mail message object.
     */
    @Nullable
    public static Message ReadMailFromFile(@NonNull File mailFile) {
        try (InputStream mailFileInputStream = new FileInputStream(mailFile)) {
            try {
                Properties props = new Properties();
                Session session = Session.getDefaultInstance(props);
                Message message = new MimeMessage(session, mailFileInputStream);
                Log.d(TAG, "ReadMailFromFile return new MimeMessage.");
                return message;
            } catch (MessagingException e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "Exception getting MimeMessage.");
                e.printStackTrace();
            } catch (Exception e2) {
                //TODO: handle this properly
                Log.d(TAG, "exception opening mailFile: ");
                e2.printStackTrace();
            } finally {
                mailFileInputStream.close();
            }
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            Log.d(TAG, "File not found opening mailFile: " + mailFile.getAbsolutePath());
            e1.printStackTrace();
        } catch (IOException exIO) {
            //TODO: handle this properly
            Log.d(TAG, "IO exception opening mailFile: " + mailFile.getAbsolutePath());
            exIO.printStackTrace();
        }
        Log.d(TAG, "ReadMailFromFile return null.");
        return null;
    }

    /**
     * @param msgString
     * @return A Java mail message object.
     */
    @Nullable
    public static Message ReadMailFromString(String msgString) {
            try {
                Properties props = new Properties();
                Session session = Session.getDefaultInstance(props);
                return new MimeMessage(session, new ByteArrayInputStream(msgString.getBytes()));
            } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    synchronized void DisconnectFromRemote() {
        Log.d(TAG, "DisconnectFromRemote");
        try {
            store.close();
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "DisconnectFromRemote Error:" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @param outfile      Name of local file in which to store the note.
     * @param notesMessage The note in the form of a mail message.
     */
    private static void SaveNote(@NonNull File outfile,
                                 @NonNull Message notesMessage) {
        try (OutputStream str = new FileOutputStream(outfile)) {
            Log.d(TAG, "SaveNote: " + outfile.getCanonicalPath());
            notesMessage.writeTo(str);
        } catch (IOException | MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    synchronized static void RemoveAccount(@NonNull Context context, @NonNull Account account) {
        Log.d(TAG, "RemoveAccount: " + account.name);
        // Delete account name entries in database
        NotesDb storedNotes = NotesDb.getInstance(context);
        storedNotes.ClearDb(account.name);
        // remove Shared Preference file
        File toDelete = new File(ImapNotes3.GetSharedPrefsDir(), ImapNotes3.RemoveReservedChars(account.name) + ".xml");
        //noinspection ResultOfMethodCallIgnored
        toDelete.delete();
        // Remove all files and sub directories
        File[] files = ImapNotes3.GetRootDir().listFiles();
        if (files != null)
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
    }

    AppendUID[] sendMessageToRemote(@NonNull Message[] message) throws MessagingException {
        assert remoteIMAPNotesFolder != null;
        synchronized (myLock) {
            OpenRemoteIMAPNotesFolder(Folder.READ_WRITE);
            return (remoteIMAPNotesFolder.appendUIDMessages(message));
        }
    }

    synchronized private void SaveNoteAndUpdateDatabase(@NonNull File directory,
                                                        @NonNull Message notesMessage,
                                                        @NonNull NotesDb storedNotes,
                                                        @NonNull String accountName,
                                                        @NonNull String suid,
                                                        @NonNull String bgColor) throws IOException {
        File outfile = new File(directory, Utilities.addMailExt(suid));
        Log.d(TAG, "SaveNoteAndUpdateDatabase: " + outfile.getCanonicalPath() + " " + accountName);
        SaveNote(outfile, notesMessage);

        // Now update or save the metadata about the message

        String title = "";
        try {
            title = notesMessage.getSubject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Some servers (such as posteo.de) don't encode non us-ascii characters in subject
        // This is a workaround to handle them
        // "lä ö ë" subject should be stored as =?charset?encoding?encoded-text?=
        // either =?utf-8?B?bMOkIMO2IMOr?=  -> Quoted printable
        // or =?utf-8?Q?l=C3=A4 =C3=B6 =C3=AB?=  -> Base64
        // Hard coding the wrong servers is not possible, as some subjects are correct encoded, and some not
        try {
            String[] rawvalue = notesMessage.getHeader("Subject");
            if (rawvalue != null && rawvalue[0] != null && (!(rawvalue[0].startsWith("=?")))) {
                title = new String(title.getBytes(StandardCharsets.ISO_8859_1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get INTERNALDATE
        String internaldate = Utilities.internalDateFormatString;
        try {
            Date MessageInternaldate = notesMessage.getReceivedDate();
            internaldate = Utilities.internalDateFormat.format(MessageInternaldate);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (title == null)
            title = "no title";

        OneNote aNote = new OneNote(
                title,
                internaldate,
                suid,
                accountName,
                bgColor,
                OneNote.SAVE_STATE_OK);

        storedNotes.InsertANoteInDb(aNote);
        List<String> tags = ListActivity.searchHTMLTags(directory, suid, Utilities.HASHTAG_PATTERN, true);
        storedNotes.UpdateTags(tags, suid, accountName);
    }

    synchronized boolean handleRemoteNotes(@NonNull File rootFolderAccount,
                                           @NonNull NotesDb storedNotes,
                                           @NonNull String accountName)
            throws MessagingException, IOException {
        assert remoteIMAPNotesFolder != null;
        Log.d(TAG, "handleRemoteNotes: " + remoteIMAPNotesFolder.getFullName() + " " + accountName);

        Message notesMessage;
        boolean result = false;
        ArrayList<Long> uids = new ArrayList<>();
        ArrayList<String> localListOfNotes = new ArrayList<>();

        // Get local list of notes uids
        File[] files = rootFolderAccount.listFiles();
        for (File file : files) {
            if (file.isFile() && (file.length() > 1)) {
                localListOfNotes.add(Utilities.removeMailExt(file.getName()));
            }
        }
        synchronized (myLock) {
            OpenRemoteIMAPNotesFolder(Folder.READ_ONLY);

            // Add to local device, new notes added to remote
            Message[] notesMessages = remoteIMAPNotesFolder.getMessagesByUID(1, UIDFolder.LASTUID);
            for (int index = notesMessages.length - 1; index >= 0; index--) {
                    notesMessage = notesMessages[index];
                    long uid = remoteIMAPNotesFolder.getUID(notesMessage);
                    // Get FLAGS
                    //flags = notesMessage.getFlags();
                    boolean deleted = notesMessage.isSet(Flags.Flag.DELETED);
                    // Builds remote list while in the loop, but only if not deleted on remote


                    if (!deleted) {
                        uids.add(remoteIMAPNotesFolder.getUID(notesMessage));
                    }
                    String suid = Long.toString(uid);
                    if (!(localListOfNotes.contains(suid))) {
                        String bgColor = HtmlNote.GetNoteFromMessage(notesMessage).color;
                        SaveNoteAndUpdateDatabase(rootFolderAccount, notesMessage, storedNotes, accountName, suid, bgColor);
                        result = true;
                    }
            }
        }

        // Remove from local device, notes removed from remote
        for (String suid : localListOfNotes) {
            Long uid = Long.valueOf(suid);
            if (!(uids.contains(uid))) {
                // Remove note from database
                storedNotes.DeleteANote(suid, accountName);
                // remove file from deleted
                File toDelete;
                toDelete = new File(rootFolderAccount, Utilities.addMailExt(suid));
                //noinspection ResultOfMethodCallIgnored
                toDelete.delete();
                // old: only SUID is used
                toDelete = new File(rootFolderAccount, suid);
                //noinspection ResultOfMethodCallIgnored
                toDelete.delete();
                result = true;
            }
        }

        return result;
    }

    private void OpenRemoteIMAPNotesFolder(int mode) throws MessagingException {
        // FIX for Race Condition..needs more work
        // sendMessageToRemote sometime closes the working folder
        if (!remoteIMAPNotesFolder.isOpen()) {
            remoteIMAPNotesFolder.open(Folder.READ_WRITE);
        }
    }

    /* Copy all notes from the IMAP server to the local directory using the UID as the file name.
     */
    void GetNotes(@NonNull Account account,
                               @NonNull File RootDirAccount,
                               @NonNull Context applicationContext,
                               @NonNull NotesDb storedNotes) throws MessagingException, IOException {
        Log.d(TAG, "GetNotes: " + account.name);
        assert remoteIMAPNotesFolder != null;
        synchronized (myLock) {
            OpenRemoteIMAPNotesFolder(Folder.READ_ONLY);

            UIDValidity = GetUIDValidity(account, applicationContext);
            SetUIDValidity(account, UIDValidity, applicationContext);
            // From the docs: "Folder implementations are expected to provide light-weight Message
            // objects, which get filled on demand. "
            // This means that at this point we can ask for the subject without getting the rest of the
            // message.
            Message[] notesMessages = remoteIMAPNotesFolder.getMessages();
            //Log.d(TAG,"number of messages in folder="+(notesMessages.length));
            // TODO: explain why we enumerate the messages in descending order of index.
            for (int index = notesMessages.length - 1; index >= 0; index--) {
                Message notesMessage = notesMessages[index];
                // write every message in files/{accountname} directory
                // filename is the original message uid
                Long UIDM = remoteIMAPNotesFolder.getUID(notesMessage);
                String suid = UIDM.toString();
                String bgColor = HtmlNote.GetNoteFromMessage(notesMessage).color;
                SaveNoteAndUpdateDatabase(RootDirAccount, notesMessage, storedNotes, account.name, suid, bgColor);
            }
        }
    }

    void DeleteNote(String fileName) throws MessagingException {
        Log.d(TAG, "DeleteNote: " + fileName);
        assert remoteIMAPNotesFolder != null;
        int numMessage = Integer.parseInt(Utilities.removeMailExt(fileName));
        synchronized (myLock) {
            OpenRemoteIMAPNotesFolder(Folder.READ_WRITE);
            //Log.d(TAG,"UID to remove:"+numMessage);
            Message[] msgs = {(remoteIMAPNotesFolder).getMessageByUID(numMessage)};

            Folder saveFolder;
            if (!copyImapFolderName.isEmpty()) {
                try {
                    saveFolder = remoteIMAPNotesFolder.getParent().getFolder(copyImapFolderName);
                    remoteIMAPNotesFolder.copyMessages(msgs, saveFolder);
                } catch (Exception e) {
                    Log.d(TAG, "Cannot move note:" + e.getMessage());
                }
            }
            remoteIMAPNotesFolder.setFlags(msgs, new Flags(Flags.Flag.DELETED), true);
            remoteIMAPNotesFolder.expunge(msgs);
        }
    }

    @Override
    protected void finalize() {
     /*   if (IsConnected()) {
            try {
                store.close();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

      */
    }

}
