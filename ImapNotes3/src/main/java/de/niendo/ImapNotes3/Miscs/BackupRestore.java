package de.niendo.ImapNotes3.Miscs;

import static de.niendo.ImapNotes3.ImapNotes3.GetDocumentDir;
import static de.niendo.ImapNotes3.ImapNotes3.getAppContext;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;

import de.niendo.ImapNotes3.ImapNotes3;
import de.niendo.ImapNotes3.R;
import de.niendo.ImapNotes3.Sync.SyncUtils;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.SimpleProgressDialog;
import eltos.simpledialogfragment.SimpleProgressTask;
import eltos.simpledialogfragment.form.Check;
import eltos.simpledialogfragment.form.FormElement;
import eltos.simpledialogfragment.form.Hint;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

public class BackupRestore extends DialogFragment implements SimpleDialog.OnDialogResultListener {
    public static final String TAG = "IN_BackupDialog";
    private static final String DLG_ACCOUNTNAME = "DLG_ACCOUNTNAME";
    private static final String DLG_DIR_ACCOUNTNAME = "DLG_DIR_ACCOUNTNAME";
    private static final String DLG_BACKUP_RESTORE_DIALOG = "DLG_BACKUP_RESTORE_DIALOG";
    private static final String DLG_BACKUP_RESTORE_DIALOG_ACCOUNT = "DLG_BACKUP_RESTORE_DIALOG_ACCOUNT";
    private static final String PROGRESS_DIALOG_RESTORE = "PROGRESS_DIALOG_RESTORE";
    private static final String PROGRESS_DIALOG_BACKUP = "PROGRESS_DIALOG_BACKUP";
    private static final String DIALOG_RESTORE_NO_FILES = "DIALOG_RESTORE_NO_FILES";
    private final Uri uri;
    private final List<String> accountList;
    private final List<String> allNotes = new ArrayList<>();
    private final List<String> allMessages = new ArrayList<>();
    private final List<String> allMessageDates = new ArrayList<>();


    public BackupRestore(Uri uri, List<String> accountList) {
        this.uri = uri;
        accountList.remove(0);
        this.accountList = accountList;
    }

    public BackupRestore() {
        this.uri = null;
        this.accountList = null;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        RestoreArchive();
        return builder.create();
    }


    public void RestoreArchive() {
        try {
            List<String> dirsInZip = ZipUtils.listDirectories(getAppContext(), uri);
            if (dirsInZip.isEmpty()) dirsInZip.add(""); // old zip format, notes in root
            if (dirsInZip.size() == 1) {
                SelectNotesDialog(dirsInZip.get(0));
            } else {
                SimpleFormDialog.build()
                        .title(R.string.restore_archive)
                        .msg(R.string.restore_more_then_one_account_found)
                        .icon(R.drawable.ic_action_restore_archive)
                        .fields(
                                Input.spinner(DLG_DIR_ACCOUNTNAME, (ArrayList<String>) dirsInZip)
                                        .hint(R.string.select_account_name_restore_import)
                                        .required(true))
                        .neg(R.string.cancel)
                        .pos(R.string.ok)
                        .show(this, DLG_BACKUP_RESTORE_DIALOG_ACCOUNT);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private INotesRestore mCallback;

    static public void CreateArchive(Activity activity, String accountname) {
        Log.d(TAG, "CreateArchive");
        String directory;
        String title;
        String basePath;
        Context context = ImapNotes3.getAppContext();

        if (accountname.isEmpty()) {
            directory = ImapNotes3.GetRootDir().toString();
            title = context.getString(R.string.all_accounts);
            basePath = "";
        } else {
            directory = ImapNotes3.GetAccountDir(accountname).toString();
            title = ImapNotes3.RemoveReservedChars(accountname);
            basePath = ImapNotes3.RemoveReservedChars(accountname) + "/";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            title = title + "_" + currentDateTime.format(formatter);
        }
        File extStorage = GetDocumentDir();
        boolean isPresent = true;
        if (!extStorage.exists()) {
            isPresent = extStorage.mkdir();
        }
        File outfile = new File(extStorage, title + ".zip");

        if (!ZipUtils.checkPermissionStorage(context)) {
            ZipUtils.requestPermission(activity);
        }

        SimpleProgressDialog sd = new SimpleProgressDialog();
        BackupTask task = new BackupTask(directory,
                outfile.toString(),
                basePath,
                sd);
        task.execute();

        boolean cancelable = true;
        boolean autoDismiss = false;
        String msg = context.getResources().getString(R.string.create_archive) + outfile;
        sd.type(SimpleProgressDialog.Type.CIRCLE);
        sd.title(R.string.make_archive);
        sd.msg(msg);
        sd.task(task, cancelable, autoDismiss);
        sd.show((FragmentActivity) activity, PROGRESS_DIALOG_BACKUP);
    }

    private void SelectNotesDialog(String dir) {
        RestoreTask task = new RestoreTask(dir,
                allNotes,
                allMessages,
                allMessageDates,
                uri);
        task.execute();

        boolean cancelable = true;
        boolean autoDismiss = true;
        Bundle extra = new Bundle();
        extra.putString(DLG_DIR_ACCOUNTNAME, dir);

        SimpleProgressDialog.buildBar()
                .extra(extra)
                .title(R.string.restore_archive)
                .msg(R.string.reading_archive_file)
                .task(task, cancelable, autoDismiss)
                .show(this, PROGRESS_DIALOG_RESTORE);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mCallback = (INotesRestore) context;
        } catch (ClassCastException e) {
            Log.d(TAG, "Activity doesn't implement the INotesRestore interface");
        }
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle bundle) {
        if (which == SimpleProgressDialog.COMPLETED) {
            if (dialogTag.equals(PROGRESS_DIALOG_RESTORE)) {
                if (allMessages.isEmpty()) {
                    SimpleDialog.build()
                            .title(R.string.restore_archive)
                            .msg(R.string.no_notes_in_file)
                            .show(this, DIALOG_RESTORE_NO_FILES);
                    return false;
                }
                String dir = bundle.getString(DLG_DIR_ACCOUNTNAME);

                FormElement<?, ?>[] formElements = new FormElement[(2 * allMessages.size()) + 1];
                int i = 0;
                formElements[i++] = Input.spinner(DLG_ACCOUNTNAME, (ArrayList<String>) accountList)
                        .hint(R.string.account_name_restore)
                        .required(true);

                int idx = 0;
                for (String file : allNotes) {

                    formElements[i++] = Check.box(file)
                            .label(allMessages.get(idx))
                            .check(false);

                    formElements[i++] = Hint.plain(allMessageDates.get(idx++));
                }
                Bundle extra = new Bundle();
                extra.putString(DLG_DIR_ACCOUNTNAME, dir);
                String msg = getResources().getString(R.string.select_notes_for_restore, dir);
                SimpleFormDialog.build()
                        //.fullscreen(true) //theme is broken
                        .title(R.string.restore_archive)
                        .msg(msg) // sometimes not shown
                        .icon(R.drawable.ic_action_restore_archive)
                        .fields(formElements)
                        .extra(extra)
                        .neg(R.string.cancel)
                        .neut(R.string.select_all_notes_for_restore)
                        .show(this, DLG_BACKUP_RESTORE_DIALOG);
                return true;
            } else if (dialogTag.equals(PROGRESS_DIALOG_BACKUP)) {
                return true;
            }
        }
        if (which == BUTTON_NEGATIVE) return false;
        switch (dialogTag) {
            case DLG_BACKUP_RESTORE_DIALOG_ACCOUNT: {
                String dir = bundle.getString(DLG_DIR_ACCOUNTNAME);
                SelectNotesDialog(dir);
                break;
            }
            case DLG_BACKUP_RESTORE_DIALOG: {
                String accountName = bundle.getString(DLG_ACCOUNTNAME);
                String dir = bundle.getString(DLG_DIR_ACCOUNTNAME);
                ArrayList<Uri> messageUris = new ArrayList<>();
                for (String file : allNotes) {
                    if (bundle.getBoolean(file) || which == BUTTON_NEUTRAL) {
                        String destDirectory = getAppContext().getCacheDir().toString() + "/Import/" + dir + "/";
                        messageUris.add(Uri.fromFile(new File(destDirectory + file)));
                    }
                }
                if (!messageUris.isEmpty()) mCallback.onSelectedData(messageUris, accountName);
                break;
            }
        }
        return false;
    }

    public interface INotesRestore {
        void onSelectedData(ArrayList<Uri> messageUris, String accountName);
    }

    static class RestoreTask extends SimpleProgressTask<Void, Integer, Integer> {
        private final List<String> allNotes;
        private final List<String> allMessages;
        private final List<String> allMessageDates;
        String dir;
        Uri uri;

        public RestoreTask(String dir,
                           List<String> allNotes,
                           List<String> allMessages,
                           List<String> allMessagesDate,
                           Uri uri) {
            this.dir = dir;
            this.uri = uri;
            this.allNotes = allNotes;
            this.allMessages = allMessages;
            this.allMessageDates = allMessagesDate;

        }

        @Override
        protected Integer doInBackground(Void... voids) {
            List<String> allNotesTmp;
            try {
                allNotesTmp = ZipUtils.listFilesInDirectory(getAppContext(), uri, dir);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            String destDirectory = getAppContext().getCacheDir().toString() + "/Import/" + dir + "/";
            int i = 0;
            for (String file : allNotesTmp) {
                publishProgress(i, allNotesTmp.size());
                if (isCancelled()) return null;
                Message message;
                String subject = "";
                String date = "";
                try {
                    message = SyncUtils.ReadMailFromFile(new File(ZipUtils.extractFile(getAppContext(), uri, file, destDirectory)));
                    if (message == null) {
                        Exception e = new Exception("File could not be extracted");
                        throw new RuntimeException(e);
                    } else {
                        subject = message.getSubject();
                        date = DateFormat.getDateTimeInstance().format(message.getSentDate());
                    }
                } catch (Exception e) {
                    if (subject.isEmpty())
                        subject = "Error reading: " + file + " - " + e.getMessage();
                    date = "";
                    e.printStackTrace();
                } finally {
                    allNotes.add(file);
                    allMessages.add(subject);
                    allMessageDates.add(date);
                }
                i++;
            }
            return null;
        }
    }

    static class BackupTask extends SimpleProgressTask<Void, Integer, Integer> {
        private final String directory;
        private final String outfile;
        private final String basePath;
        private final SimpleProgressDialog sd;

        public BackupTask(String directory,
                          String outfile,
                          String basePath,
                          SimpleProgressDialog sd) {
            this.directory = directory;
            this.outfile = outfile;
            this.basePath = basePath;
            this.sd = sd;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            sd.updateInfoText(getAppContext().getResources().getString(R.string.archiving));
            try {
                ZipUtils.zipDirectory(directory, outfile, basePath);
                sd.updateInfoText(getAppContext().getResources().getString(R.string.success));
                return 1;
            } catch (IOException e) {
                sd.updateInfoText(getAppContext().getResources().getString(R.string.failed) + e.getMessage());
            }
            return -1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
        }

    }

}
