/*
 * Copyright (C) 2022-2025 - Peter Korf <peter@niendo.de>
 * Copyright (C)      2016 - Martin Carpella
 * Copyright (C) 2014-2015 - nb
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
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import android.text.Html;
import android.util.Log;

import de.niendo.ImapNotes3.Data.ImapNotesAccount;
import de.niendo.ImapNotes3.Data.NotesDb;
import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.ImapNotes3;
import de.niendo.ImapNotes3.NotesListAdapter;
import de.niendo.ImapNotes3.Sync.SyncUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

// TODO: move arguments from execute to constructor.
public class UpdateThread extends AsyncTask<Object, Void, Boolean> {

    //https://stackoverflow.com/questions/54346609/returning-boolean-from-onpostexecute-and-doinbackground
    //https://fernandocejas.com/2014/09/03/architecting-android-the-clean-way/
    private final FinishListener listener;
    private static final String TAG = "IN_UpdateThread";
    private final ImapNotesAccount ImapNotesAccount;
    private final @StringRes
    int resId;
    private final NotesListAdapter adapter;
    private final ArrayList<OneNote> notesList;
    private final String noteBody;
    private final String bgColor;
    private String ErrorMsg;

    private final String accountName;
    private final Action action;
    private String suid;
    private final NotesDb storedNotes;
    private OneNote currentNote;
    private int indexToDelete;
    private final ArrayList<Uri> uris;

    /*
    Assign all fields in the constructor because we never reuse this object.  This makes the code
    typesafe.  Make them final to prevent accidental reuse.
    */
    public UpdateThread(String accountName,
                        FinishListener listener,
                        ArrayList<OneNote> noteList,
                        NotesListAdapter listToView,
                        @StringRes int resId,
                        String suid,
                        String noteBody,
                        String bgColor,
                        Context context,
                        Action action) {
        //ImapNotesAccount ImapNotesAccount
        Log.d(TAG, "UpdateThread: ");
        this.accountName = accountName;
        Account account = new Account(accountName, Utilities.PackageName);
        this.ImapNotesAccount = new ImapNotesAccount(account, context);
        this.listener = listener;
        this.notesList = noteList;
        this.adapter = listToView;
        this.resId = resId;
        this.suid = suid;
        this.noteBody = noteBody;
        this.bgColor = bgColor;
        this.action = action;
        this.storedNotes = NotesDb.getInstance(context);
        currentNote = null;
        uris = null;
        indexToDelete = -1;
    }


    /*
Assign all fields in the constructor because we never reuse this object.  This makes the code
typesafe.  Make them final to prevent accidental reuse.
*/
    public UpdateThread(String accountName,
                        FinishListener listener,
                        ArrayList<OneNote> noteList,
                        NotesListAdapter listToView,
                        @StringRes int resId,
                        ArrayList<Uri> uris,
                        Context context) {
        //ImapNotesAccount ImapNotesAccount
        Log.d(TAG, "UpdateThread Uri: ");
        this.accountName = accountName;
        Account account = new Account(accountName, Utilities.PackageName);
        this.ImapNotesAccount = new ImapNotesAccount(account, context);
        this.listener = listener;
        this.notesList = noteList;
        this.adapter = listToView;
        this.resId = resId;
        this.action = Action.InsertMultipleMessages;
        this.storedNotes = NotesDb.getInstance(context);
        this.noteBody = null;
        this.bgColor = null;

        currentNote = null;
        indexToDelete = -1;
        this.uris = uris;
    }

    @Override
    protected Boolean doInBackground(Object... stuffs) {
        ErrorMsg = "";
        Log.d(TAG, "doInBackground");
            // Do we have a note to remove?
            if (action == Action.Delete) {
                Log.v(TAG, "Received request to delete message #" + suid);
                // Here we delete the note from the local notes list
                indexToDelete = getIndexByNumber(suid);
                storedNotes.DeleteANote(suid, accountName);
                MoveMailToDeleted(suid);
                return true;
            }

            // Do we have a note to add?
            if ((action == Action.Insert) || (action == Action.Update)) {
                Log.v(TAG, "Action Insert/Update:" + suid);
                String oldSuid = suid;
                storedNotes.SetSaveState(suid, OneNote.SAVE_STATE_SAVING, accountName);
                // Use the first line as the tile
                String[] tok = Html.fromHtml(noteBody, Html.FROM_HTML_MODE_LEGACY).toString().split("\n", 2);
                String title = tok[0];
                //"<html><head></head><body>" + noteBody + "</body></html>";

                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat(Utilities.internalDateFormatString, Locale.ROOT);
                String stringDate = sdf.format(date);
                currentNote = new OneNote(title, stringDate, "", accountName, bgColor, OneNote.SAVE_STATE_SAVING);
                // Add note to database
                if (!suid.startsWith("-")) {
                    // no temp. suid in use
                    suid = storedNotes.GetTempNumber(currentNote);
                }
                storedNotes.SetSaveState(suid, OneNote.SAVE_STATE_SAVING, accountName);
                currentNote.SetUid(suid);
                // Here we ask to add the new note to the new note folder
                // Must be done AFTER uid has been set in currentNote
                Log.v(TAG, "doInBackground body: ");
                try {
                    WriteMailToNew(currentNote, noteBody);
                } catch (RuntimeException e) {
                    // something went wrong; set new state for message
                    // for now all changes are lost
                    ErrorMsg = "WriteMailToNew failed (discard changes): " + e.getLocalizedMessage();
                    Log.e(TAG, "WriteMailToNew failed (discard changes): ", e);
                    storedNotes.RemoveTempNumber(currentNote);
                    storedNotes.SetSaveState(oldSuid, OneNote.SAVE_STATE_OK, accountName);
                    return false;
                }
                if ((action == Action.Update) && (!oldSuid.startsWith("-"))) {
                    MoveMailToDeleted(oldSuid);
                }
                storedNotes.DeleteANote(oldSuid, currentNote.GetAccount());
                currentNote.SetState(OneNote.SAVE_STATE_OK);
                storedNotes.InsertANoteInDb(currentNote);

                // Add note to noteList but change date format before
                //DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(applicationContext);
                String sdate = DateFormat.getDateTimeInstance().format(date);
                currentNote.SetDate(sdate);
                indexToDelete = getIndexByNumber(oldSuid);
                //throw new IllegalStateException("testtest");
                return true;
            }

        // Do we have multiple notes to add?
            if ((action == Action.InsertMultipleMessages)) {
                Message message;
                for (Uri uri : uris) {
                    Log.d(TAG, "Action InsertMultipleMessages:");
                    message = SyncUtils.ReadMailFromString(ImapNotes3.UriToString(uri));
                    if (message != null) {
                        HtmlNote htmlNote = HtmlNote.GetNoteFromMessage(message);
                        //storedNotes.SetSaveState("", OneNote.SAVE_STATE_SAVING, accountName);

                        Date date = null;
                        try {
                            date = message.getSentDate();
                        } catch (MessagingException e) {
                            Log.e(TAG, "getSentDate failed: ", e);
                        }
                        if (date == null) date = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat(Utilities.internalDateFormatString, Locale.ROOT);
                        String stringDate = sdf.format(date);

                        String subject;
                        try {
                            subject = message.getSubject();
                        } catch (MessagingException e) {
                            Log.e(TAG, "getSubject failed: ", e);
                            subject = "subject not found: " + e.getLocalizedMessage();
                        }

                        currentNote = new OneNote(subject, stringDate, "", accountName, htmlNote.color, OneNote.SAVE_STATE_SAVING);
                        // Add note to database

                        suid = storedNotes.GetTempNumber(currentNote);
                        storedNotes.SetSaveState(suid, OneNote.SAVE_STATE_SAVING, accountName);
                        currentNote.SetUid(suid);
                        // Here we ask to add the new note to the new note folder
                        // Must be done AFTER uid has been set in currentNote
                        Log.d(TAG, "doInBackground body: ");
                        try {
                            WriteMailToNew(currentNote, htmlNote.text);
                        } catch (RuntimeException e) {
                            // something went wrong; undo all actions in database
                            storedNotes.RemoveTempNumber(currentNote);
                            ErrorMsg = "WriteMailToNew failed (discard changes): " + e.getLocalizedMessage();
                            Log.e(TAG, "WriteMailToNew failed: ", e);
                            continue;
                        }
                        currentNote.SetState(OneNote.SAVE_STATE_OK);
                        storedNotes.InsertANoteInDb(currentNote);

                        // Add note to noteList but change date format before
                        //DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(applicationContext);
                        String sdate = DateFormat.getDateTimeInstance().format(date);
                        currentNote.SetDate(sdate);
                        indexToDelete = -1;
                        return true;
                    }
                }
            }
        return false;
    }

    protected void onPostExecute(Boolean result) {
        if (result) {
            if (indexToDelete >= 0) notesList.remove(indexToDelete);
            if (!(currentNote == null)) notesList.add(0, currentNote);
        }

        adapter.notifyDataSetChanged();
        if (action == Action.Delete) result = false;
        listener.onFinishPerformed(result, ErrorMsg);
    }

    private int getIndexByNumber(String pNumber) {
        for (OneNote _item : notesList) {
            if (_item.GetUid().equals(pNumber))
                return notesList.indexOf(_item);
        }
        return -1;
    }

    /**
     * @param suid IMAP ID of the note.
     */
    private void MoveMailToDeleted(@NonNull String suid) {
        File directory = ImapNotesAccount.GetRootDirAccount();
        // TODO: Explain why we need to omit the first character of the UID
        File from = new File(directory, Utilities.addMailExt(suid));
        if (!from.exists()) {
            String positiveUid = suid.substring(1);
            from = new File(directory + "/new", Utilities.addMailExt(positiveUid));
            // TODO: Explain why it is safe to ignore the result of delete.
            //noinspection ResultOfMethodCallIgnored
            from.delete();
        } else {
            File to = new File(directory + "/deleted/" + Utilities.addMailExt(suid));
            // TODO: Explain why it is safe to ignore the result of rename.
            //noinspection ResultOfMethodCallIgnored
            from.renameTo(to);
        }
    }

    @NonNull
    private Message MakeMessageWithAttachment(String subject,
                                              String message,
                                              String filePath,
                                              Session session)
            throws IOException, MessagingException {

        Message msg = new MimeMessage(session);


        msg.setSubject(subject);
        msg.setSentDate(new Date());

        // creates message part
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(message, "text/html");

        // creates multi-part
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        // add attachment

        MimeBodyPart attachPart = new MimeBodyPart();

        attachPart.attachFile(filePath);


        multipart.addBodyPart(attachPart);

        // sets the multi-part as e-mail's content
        msg.setContent(multipart);
        return msg;
    }

    private void WriteMailToNew(@NonNull OneNote note,
                                String noteBody) {
        //throws MessagingException, IOException
        Log.d(TAG, "WriteMailToNew: " + noteBody.length() + "Bytes");
        //String body = null;

        // Here we add the new note to the new note folder
        //Log.d(TAG,"Add new note");
        Message message;
        try {
            message = HtmlNote.GetMessageFromNote(note, noteBody);
        } catch (MessagingException e) {
            Log.e(TAG, "WriteMailToNew: GetMessageFromNote fatal failed:", e);
            throw new RuntimeException(e);
        }

        try {
            message.setSubject(note.GetTitle());
        } catch (MessagingException e) {
            Log.e(TAG, "WriteMailToNew: setSubject failed:", e);
        }
        MailDateFormat mailDateFormat = new MailDateFormat();
        // Remove (CET) or (GMT+1) part as asked in github issue #13
        String headerDate = (mailDateFormat.format(new Date())).replaceAll("\\(.*$", "");
        try {
            message.addHeader("Date", headerDate);
        } catch (MessagingException e) {
            Log.e(TAG, "WriteMailToNew: addHeader Date failed:", e);
        }
        // Get temporary UID
        String uid = Integer.toString(Math.abs(Integer.parseInt(note.GetUid())));
        File accountDirectory = ImapNotesAccount.GetRootDirAccount();
        File directory = new File(accountDirectory, "new");
        try {
            message.setFrom(UserNameToEmail(ImapNotesAccount.username));
        } catch (MessagingException e) {
            Log.e(TAG, "WriteMailToNew: UsernameToEmail failed:", e);
        }
        File outfile = new File(directory, Utilities.addMailExt(uid));
        OutputStream str = null;
        try {
            str = new FileOutputStream(outfile, false);
            message.writeTo(str);
            str.close();
        } catch (IOException | MessagingException e) {
            Log.e(TAG, "WriteMailToNew: write file fatal failed:", e);
        }
    }

    public Address UserNameToEmail(@NonNull String name) {
        InternetAddress internetAddress = new InternetAddress();
        internetAddress.setAddress(name.contains("@") ? name : name + "@" + Utilities.ApplicationName);
        return internetAddress;
    }

    public interface FinishListener {
        void onFinishPerformed(Boolean result, String ErrText);
    }

    public enum Action {
        Update,
        Insert,
        Delete,
        InsertMultipleMessages
    }

}
