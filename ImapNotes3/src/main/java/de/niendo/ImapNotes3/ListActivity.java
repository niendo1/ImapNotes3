/*
 * Copyright (C) 2022-2023 - Peter Korf <peter@niendo.de>
 * Copyright (C)      2023 - woheller69
 * Copyright (C)         ? - kwhitefoot
 * Copyright (C)      2016 - Martin Carpella
 * Copyright (C) 2014-2015 - c0238
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

package de.niendo.ImapNotes3;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import de.niendo.ImapNotes3.Data.ImapNotesAccount;
import de.niendo.ImapNotes3.Data.NotesDb;
import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.Data.SyncInterval;
import de.niendo.ImapNotes3.Miscs.HtmlNote;
import de.niendo.ImapNotes3.Miscs.Imaper;
import de.niendo.ImapNotes3.Miscs.SyncThread;
import de.niendo.ImapNotes3.Miscs.UpdateThread;
import de.niendo.ImapNotes3.Miscs.Utilities;
import de.niendo.ImapNotes3.Sync.SyncUtils;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.list.SimpleListDialog;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static de.niendo.ImapNotes3.AccountConfigurationActivity.ACTION;


public class ListActivity extends AppCompatActivity implements OnItemSelectedListener, Filterable, SimpleDialog.OnDialogResultListener, UpdateThread.FinishListener {
    private static final int SEE_DETAIL = 2;
    public static final int DELETE_BUTTON = 3;
    private static final int NEW_BUTTON = 4;
    private static final int SAVE_BUTTON = 5;
    private static final int EDIT_BUTTON = 6;
    private static final int ADD_ACCOUNT = 7;

    public static final int ResultCodeSuccess = 0;
    public static final int ResultCodeError = -1;

    //region Intent item names
    public static final String EDIT_ITEM_NUM_IMAP = "EDIT_ITEM_NUM_IMAP";
    public static final String EDIT_ITEM_TXT = "EDIT_ITEM_TXT";
    public static final String EDIT_ITEM_COLOR = "EDIT_ITEM_COLOR";
    public static final String EDIT_ITEM_ACCOUNTNAME = "EDIT_ITEM_ACCOUNTNAME";
    public static final String ACCOUNTNAME = "ACCOUNTNAME";
    public static final String SYNCINTERVAL = "SYNCINTERVAL";
    public static final String CHANGED = "CHANGED";
    public static final String SYNCED = "SYNCED";
    public static final String REFRESH_TAGS = "REFRESH_TAGS";
    public static final String SYNCED_ERR_MSG = "SYNCED_ERR_MSG";
    private static final String SAVE_ITEM_COLOR = "SAVE_ITEM_COLOR";
    private static final String SAVE_ITEM = "SAVE_ITEM";
    private static final String DELETE_ITEM_NUM_IMAP = "DELETE_ITEM_NUM_IMAP";
    private static final String ACCOUNTSPINNER_POS = "ACCOUNTSPINNER_POS";
    private static final String SORT_BY_DATE = "SORT_BY_DATE";
    private static final String SORT_BY_TITLE = "SORT_BY_TITLE";
    private static final String SORT_BY_COLOR = "SORT_BY_COLOR";
    private static final String DLG_FILTER_HASHTAG = "DLG_FILTER_HASHTAG";
    //endregion
    private Intent intentActionSend;
    private ArrayList<OneNote> noteList;
    private NotesListAdapter listToView;
    private ArrayAdapter<String> spinnerList;
    private static final String AUTHORITY = Utilities.PackageName + ".provider";
    private Spinner accountSpinner;
    public static ImapNotesAccount ImapNotesAccount;
    private static AccountManager accountManager;
    @Nullable
    private static NotesDb storedNotes = null;
    private static List<String> currentList;
    private static Menu actionMenu;
    private static CharSequence mFilterString = "";
    static String[] hashFilter;
    private static ArrayList<String> hashFilterSelected = new ArrayList<>();
    @NonNull
    private ContentObserver mObserver;

    // FIXME
    // Hack! accountManager.addOnAccountsUpdatedListener
    // OnAccountsUpdatedListener is called to early - so not all
    // Date in AccountManager is saved - it gives crashes on the very first start
    public Boolean EnableAccountsUpdate = true;
    // Ensure that we never have to check for null by initializing reference.
    @NonNull
    private static Account[] accounts = new Account[0];
    private static String OldStatus;
    private final OnClickListener clickListenerEditAccount = v -> {
        if (getSelectedAccountName().equals("")) {
            ImapNotes3.ShowMessage(R.string.select_one_account, accountSpinner, 3);
            return;
        }
        Intent res = new Intent();
        String mPackage = Utilities.PackageName;
        String mClass = ".AccountConfigurationActivity";
        res.setComponent(new ComponentName(mPackage, mPackage + mClass));
        res.putExtra(ACTION, AccountConfigurationActivity.Actions.EDIT_ACCOUNT);
        res.putExtra(AccountConfigurationActivity.ACCOUNTNAME, ListActivity.ImapNotesAccount.accountName);
        startActivity(res);
    };
    private static final String TAG = "IN_Listactivity";
    //@Nullable
    private TextView status;
    private ListView listview;
    private AsyncTask updateThread;

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mObserver);
    }

    public static ArrayList getAccountList() {
        ArrayList accounts = new ArrayList<>();
        for (Account mAccount : ListActivity.accounts) {
            accounts.add(mAccount.name);
        }
        return accounts;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setElevation(0); // or other
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.ActionBgColor)));

        this.accountSpinner = findViewById(R.id.accountSpinner);
        ListActivity.currentList = new ArrayList<>();

        this.accountSpinner.setOnItemSelectedListener(this);
        ImapNotes3.setContent(findViewById(android.R.id.content));

        //ImapNotesAccount = new ImapNotesAccount();
        ListActivity.accountManager = AccountManager.get(getApplicationContext());
        ListActivity.accountManager.addOnAccountsUpdatedListener(
                new AccountsUpdateListener(), null, true);

        status = findViewById(R.id.status);

        spinnerList = new ArrayAdapter<>
                (this, R.layout.account_spinner_item, ListActivity.currentList);
        accountSpinner.setAdapter(spinnerList);

        this.noteList = new ArrayList<>();
        //((de.niendo.ImapNotes3) this.getApplicationContext()).SetNotesList(this.noteList);
        this.listToView = new NotesListAdapter(
                this,
                this.noteList,
                new String[]{OneNote.TITLE, OneNote.DATE},
                new int[]{R.id.noteTitle, R.id.noteLastChange},
                OneNote.BGCOLOR);

        listview = findViewById(R.id.notesList);
        listview.setAdapter(this.listToView);

        listview.setTextFilterEnabled(true);

        Imaper imapFolder = new Imaper();
        ((ImapNotes3) this.getApplicationContext()).SetImaper(imapFolder);

        storedNotes = NotesDb.getInstance(getApplicationContext());

        // When item is clicked, we go to NoteDetailActivity
        listview.setOnItemClickListener((parent, widget, selectedNote, rowId) -> {
            Log.d(TAG, "onItemClick");
            Intent toDetail;

            String saveState = noteList.get(selectedNote).GetState();
            saveState = storedNotes.GetSaveState(noteList.get(selectedNote).GetUid(), noteList.get(selectedNote).GetAccount());

            if (saveState.equals(OneNote.SAVE_STATE_SAVING)) {
                ImapNotes3.ShowMessage(R.string.save_wait_necessary, listview, 3);
                return;
            } else if (saveState.equals(OneNote.SAVE_STATE_SYNCING)) {
                ImapNotes3.ShowMessage(R.string.sync_wait_necessary, listview, 3);
                return;
            }

            if (intentActionSend != null)
                // FIXME StrictMode policy violation: android.os.strictmode.UnsafeIntentLaunchViolation: Launch of unsafe intent: Intent
                toDetail = intentActionSend;
            else
                toDetail = new Intent(widget.getContext(), NoteDetailActivity.class);
            toDetail.putExtra(NoteDetailActivity.selectedNote, (OneNote) parent.getItemAtPosition(selectedNote));
            if (ListActivity.ImapNotesAccount != null)
                 toDetail.putExtra(NoteDetailActivity.ActivityType, NoteDetailActivity.ActivityTypeEdit);
            startActivityForResult(toDetail, SEE_DETAIL);
            if (intentActionSend != null)
                intentActionSend.putExtra(NoteDetailActivity.ActivityTypeProcessed, true);
            //intentActionSend=null;
            Log.d(TAG, "onItemClick, back from detail.");

            //TriggerSync(status);
        });

        Button editAccountButton = findViewById(R.id.editAccountButton);
        editAccountButton.setOnClickListener(clickListenerEditAccount);

        mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            public void onChange(boolean selfChange) {
                Log.d(TAG, "ContentObserver.OnChange");
                if (selfChange || ImapNotes3.intent == null) return;
                String accountName = ImapNotes3.intent.getStringExtra(ACCOUNTNAME);
                boolean isChanged = ImapNotes3.intent.getBooleanExtra(CHANGED, false);
                boolean isSynced = ImapNotes3.intent.getBooleanExtra(SYNCED, false);
                String errorMessage = ImapNotes3.intent.getStringExtra(SYNCED_ERR_MSG);
                SyncInterval syncInterval = SyncInterval.from(ImapNotes3.intent.getStringExtra(SYNCINTERVAL));
                if ((ImapNotesAccount != null) && accountName.equals(ImapNotesAccount.accountName)) {
                    Log.d(TAG, "if " + accountName + " " + ImapNotesAccount.accountName);
                    String statusText = OldStatus;
                    if (isSynced) {
                        Date date = new Date();
                        String sdate;
                        try {
                            sdate = DateFormat.getDateTimeInstance().format(date);
                        } catch (Exception e) {
                            sdate = "";
                        }

                        statusText = getText(R.string.Last_sync) + sdate;
                        if (!syncInterval.equals("0"))
                            statusText += " (" + getText(syncInterval.textID) + ")";
                    }
                    status.setBackgroundColor(getColor(R.color.StatusBgColor));
                    if (!errorMessage.isEmpty()) {
                        statusText = errorMessage;
                        status.setBackgroundColor(getColor(R.color.StatusBgErrColor));
                    }
                    status.setText(statusText);
                }
                if (isChanged) RefreshList();
            }
        };
        getContentResolver().registerContentObserver(Uri.parse("content://" + BuildConfig.APPLICATION_ID + "/"), false, mObserver);

    }


    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (which == BUTTON_NEGATIVE) return false;
        switch (dialogTag) {
            case DLG_FILTER_HASHTAG:
                if (which == BUTTON_POSITIVE) {
                    hashFilterSelected = extras.getStringArrayList(SimpleListDialog.SELECTED_LABELS);
                    if (hashFilterSelected.size() == 0)
                        hashFilter = null;
                    else {
                        hashFilter = new String[hashFilterSelected.size()];
                        hashFilterSelected.toArray(hashFilter);
                    }
                    ;
                }
                if (which == BUTTON_NEUTRAL) {
                    hashFilter = null;
                    hashFilterSelected.clear();
                }
                RefreshList();
                return true;
        }
        return false;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        Check_Action_Send(intent);
    }

    private void Check_Action_Send(Intent intent) {
        Log.d(TAG, "Check_Action_Send");
        // Get intent, action and MIME type
        if (intent == null)
            intent = getIntent();
        String action = intent.getAction();

        intentActionSend = null;
        if (action.equals(Intent.ACTION_SEND)) {
            intentActionSend = (Intent) intent.clone();
            intentActionSend.setClass(this, NoteDetailActivity.class);
            intentActionSend.setFlags(0);
            intentActionSend.putExtra(ListActivity.EDIT_ITEM_ACCOUNTNAME, ImapNotesAccount.accountName);
            intentActionSend.putExtra(NoteDetailActivity.ActivityType, NoteDetailActivity.ActivityTypeAddShare);

            ImapNotes3.ShowAction(listview, R.string.insert_as_new_note, R.string.ok, 0,
                    () -> {
                        startActivityForResult(intentActionSend, ListActivity.NEW_BUTTON);
                        intentActionSend = null;
                    });
        }
    }

    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        setPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        savePreferences();
        if (!(updateThread == null)) {
            // for some reason this helps...
            synchronized (updateThread) {
                if (updateThread.getStatus() == AsyncTask.Status.RUNNING) {
                    Log.d(TAG, "onPause RUNNING");
                }
            }
        }
    }

    @Override
    public void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.d(TAG, "onPostCreate");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        savePreferences();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState");
    }

    private void savePreferences() {
        SharedPreferences.Editor preferences = getApplicationContext().getSharedPreferences(Utilities.PackageName, MODE_PRIVATE).edit();
        preferences.putLong(ACCOUNTSPINNER_POS, accountSpinner.getSelectedItemId());
        if (actionMenu == null) return;
        preferences.putBoolean(SORT_BY_DATE, actionMenu.findItem(R.id.sort_date).isChecked());
        preferences.putBoolean(SORT_BY_TITLE, actionMenu.findItem(R.id.sort_title).isChecked());
        preferences.putBoolean(SORT_BY_COLOR, actionMenu.findItem(R.id.sort_color).isChecked());
        preferences.apply();
    }

    private void setPreferences() {
        Log.d(TAG, "setPreferences:");
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Utilities.PackageName, MODE_PRIVATE);
        accountSpinner.setSelection((int) preferences.getLong(ACCOUNTSPINNER_POS, 0));
    }


    private void RefreshList() {
        listToView.setSortOrder(getSortOrder());
        listToView.setAccountName(getSelectedAccountName());
        synchronized (this) {
            new SyncThread(
                    getSelectedAccountName(),
                    noteList,
                    listToView,
                    R.string.refreshing_notes_list,
                    getSortOrder(),
                    hashFilter,
                    mFilterString,
                    // FIXME: this. ?
                    getApplicationContext()).execute();
        }
        status.setBackgroundColor(getColor(R.color.StatusBgColor));
        status.setText(R.string.welcome);
    }

    private void UpdateList(
            String suid,
            String noteBody,
            String bgColor,
            String accountName,
            UpdateThread.Action action) {
        synchronized (this) {
                updateThread = new UpdateThread(accountName,
                        this,
                        noteList,
                        listToView,
                        R.string.updating_notes_list,
                        suid,
                        noteBody,
                        bgColor,
                        // FIXME: this. ?
                        getApplicationContext(),
                        action).execute();
            }
    }

    @Override
    public void onFinishPerformed(Boolean result) {
        if (result) TriggerSync(false);
    }

    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        actionMenu = menu;
        getMenuInflater().inflate(R.menu.menu_list, menu);

        MenuBuilder m = (MenuBuilder) menu;
        m.setOptionalIconsVisible(true);

        // Associate searchable configuration with the SearchView
        // disable SearchManager and setSearchableInfo .. it seems confusing and useless
        //SearchManager searchManager =
        //        (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem menuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint(getText(R.string.search_any_keyword));

        // searchView.setSearchableInfo(
        //         searchManager.getSearchableInfo(getComponentName()));
        SearchView.OnQueryTextListener textChangeListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                // FIXME THREAD!
                // search only if user pushes glases or enter .. its faster
                // this is your adapter that will be filtered
                // mFilterString = newText;
                // listToView.getFilter().filter(newText);
                if ((newText == null) || (newText.isEmpty())) {
                    mFilterString = "";
                    listToView.ResetFilterData(noteList);
                    RefreshList();
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                // this is your adapter that will be filtered
                mFilterString = query;
                listToView.getFilter().filter(query);
                return true;
            }
        };
        // restore List and Filter after closing search
        searchView.setOnCloseListener(() -> {
            mFilterString = "";
            this.listToView.ResetFilterData(noteList);
            RefreshList();
            return true;
        });

        menuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                //searchView.requestFocus(); - doesn't work properly
                searchView.setIconifiedByDefault(false);
                searchView.setIconified(false);
                mFilterString = "";
                listToView.getFilter().filter("");
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mFilterString = "";
                searchView.clearFocus();
                listToView.ResetFilterData(noteList);
                return true;
            }
        });

        // searchView.requestFocus(); - doesn't work properly
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);

        searchView.setOnQueryTextListener(textChangeListener);
        // load values from disk
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(Utilities.PackageName, MODE_PRIVATE);

        if (preferences.getBoolean(SORT_BY_TITLE, false))
            actionMenu.findItem(R.id.sort_title).setChecked(true);
        else if (preferences.getBoolean(SORT_BY_COLOR, false))
            actionMenu.findItem(R.id.sort_color).setChecked(true);
        else
            actionMenu.findItem(R.id.sort_date).setChecked(true);

        return true;
    }

    private String getSortOrder() {
        if (actionMenu.findItem(R.id.sort_title).isChecked())
            return "UPPER(" + OneNote.TITLE + ") ASC";
        if (actionMenu.findItem(R.id.sort_color).isChecked()) return OneNote.BGCOLOR + " ASC";

        return OneNote.DATE + " DESC";
    }

    public static List<String> searchHTMLTags(@NonNull File nameDir, @NonNull String uid, String searchTerm, boolean useRegex) {
        // Compile the regular expression pattern if necessary
        Pattern pattern = null;
        List<String> retVal = new ArrayList<String>();
        if (useRegex) {
            try {
                pattern = Pattern.compile(searchTerm);
            } catch (PatternSyntaxException e) {
                return retVal;
            }
        } else {
            pattern = Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE);
        }

        String html = Jsoup.parse(HtmlNote.GetNoteFromMessage(SyncUtils.ReadMailFromFile(nameDir, uid)).text).text();

        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            retVal.add(matcher.group());
        }
        return retVal;
    }

    synchronized private void TriggerSync(boolean refreshTags) {
        Log.d(TAG, "TriggerSync");
        if (ListActivity.ImapNotesAccount == null) {
            Log.d(TAG, "TriggerSync: Account==null");
            return;
        }

        OldStatus = status.getText().toString();
        status.setText(R.string.syncing);
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        settingsBundle.putBoolean(REFRESH_TAGS, refreshTags);
        //Log.d(TAG,"Request a sync for:"+mAccount);

        Account mAccount = ListActivity.ImapNotesAccount.GetAccount();
        ContentResolver.cancelSync(mAccount, AUTHORITY);
        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.newaccount:
                Intent res = new Intent();
                String mPackage = Utilities.PackageName;
                String mClass = ".AccountConfigurationActivity";
                res.setComponent(new ComponentName(mPackage, mPackage + mClass));
                res.putExtra(ACTION, AccountConfigurationActivity.Actions.CREATE_ACCOUNT);
                startActivityForResult(res, ListActivity.ADD_ACCOUNT);
                return true;
            case R.id.refresh:
                if (getSelectedAccountName().equals(""))
                    ImapNotes3.ShowMessage(R.string.select_one_account, accountSpinner, 3);
                else
                    TriggerSync(true);
                return true;
            case R.id.newnote:
                Intent toNew;
                if (intentActionSend != null)
                    toNew = intentActionSend;
                else
                    toNew = new Intent(this, NoteDetailActivity.class);
                //toNew.putExtra(NoteDetailActivity.useSticky, ListActivity.ImapNotesAccount.usesticky);
                toNew.putExtra(NoteDetailActivity.ActivityType, NoteDetailActivity.ActivityTypeAdd);
                toNew.putExtra(ListActivity.ACCOUNTNAME, getSelectedAccountName());
                startActivityForResult(toNew, ListActivity.NEW_BUTTON);
                if (intentActionSend != null)
                    intentActionSend.putExtra(NoteDetailActivity.ActivityTypeProcessed, true);
                return true;
            case R.id.sort_date:
            case R.id.sort_title:
            case R.id.sort_color: {
                item.setChecked(true);
                RefreshList();
                return true;
            }
            case R.id.filter_by_hash: {
                NotesDb storedNotes = NotesDb.getInstance(getApplicationContext());
                List<String> tags = storedNotes.GetTags("", getSelectedAccountName());
                List<Integer> positions = new ArrayList<>();
                for (String tag : tags) {
                    if (hashFilterSelected.contains(tag))
                        positions.add(tags.indexOf(tag));
                }
                String[] tagArray = new String[tags.size()];
                tags.toArray(tagArray);
                SimpleListDialog.build()
                        .title(R.string.filter_by_hash)
                        .choiceMode(SimpleListDialog.MULTI_CHOICE)
                        .filterable(true)
                        .choicePreset(positions)
                        .items(tagArray)
                        .filterable(true)
                        .neg(R.string.cancel)
                        .neut(R.string.reset_filter)
                        .show(this, DLG_FILTER_HASHTAG);
                return true;
            }
            case R.id.about:
                String about = getString(R.string.license) + "<br>";
                about += "ID: " + BuildConfig.APPLICATION_ID + "<br>";
                about += "Version: " + BuildConfig.VERSION_NAME + "<br>";
                about += "Code: " + BuildConfig.VERSION_CODE + "<br>";
                about += "DB-Version: " + NotesDb.NOTES_VERSION + "<br>";
                about += "Build typ: " + BuildConfig.BUILD_TYPE + "<br>";
                about += getString(R.string.internet) + "<br>";
                SimpleDialog.build()
                        .title(getString(R.string.about) + " " + BuildConfig.APPLICATION_NAME)
                        .icon(R.mipmap.ic_launcher)
                        .msgHtml(about)
                        .show(this);
                return true;
            case R.id.send_debug_report:
                SendLogcatMail();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Integer getSpinnerPos(String accountName) {
        ArrayAdapter adapter = (ArrayAdapter) accountSpinner.getAdapter();
        int n = adapter.getCount();
        for (int i = 1; i < n; i++) {
            if (accountName.equals(adapter.getItem(i).toString())) {
                return (i);
            }
        }
        return 0;
    }

    // Spinner item selected listener
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Log.d(TAG, "onItemSelected");
        if (pos > 0) {
            Account account = ListActivity.accounts[pos - 1];
            ListActivity.ImapNotesAccount = new ImapNotesAccount(account, getApplicationContext());
        }
        RefreshList();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
    }

    // Hack: if the Spinner isDisabled Search is active->
    //all accounts are selected
    public String getSelectedAccountName() {
        if ((ImapNotesAccount == null) || accountSpinner.getSelectedItemId() == 0) {
            if (accounts.length == 1) {
                return accounts[0].name;
            }
            return "";
        }
        return ImapNotesAccount.accountName;
    }

    ;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " + requestCode + " " + resultCode);
        switch (requestCode) {
            case ListActivity.SEE_DETAIL:
                // Returning from NoteDetailActivity
                if (resultCode == ListActivity.DELETE_BUTTON) {
                    // Delete Message asked for
                    // String suid will contain the Message Imap UID to delete
                    String suid = data.getStringExtra(DELETE_ITEM_NUM_IMAP);
                    String accountName = data.getStringExtra(EDIT_ITEM_ACCOUNTNAME);
                    ListActivity.ImapNotesAccount = new ImapNotesAccount(getAccountFromName(accountName), getApplicationContext());
                    UpdateList(suid, null, null, accountName, UpdateThread.Action.Delete);
                }
                if (resultCode == ListActivity.EDIT_BUTTON) {
                    String txt = ImapNotes3.AvoidLargeBundle; //data.getStringExtra(EDIT_ITEM_TXT);
                    String suid = data.getStringExtra(EDIT_ITEM_NUM_IMAP);
                    String bgcolor = data.getStringExtra(EDIT_ITEM_COLOR);
                    String accountName = data.getStringExtra(EDIT_ITEM_ACCOUNTNAME);
                    //Log.d(TAG,"Received request to edit message:"+suid);
                    //Log.d(TAG,"Received request to replace message with:"+txt);
                    ListActivity.ImapNotesAccount = new ImapNotesAccount(getAccountFromName(accountName), getApplicationContext());
                    UpdateList(suid, txt, bgcolor, accountName, UpdateThread.Action.Update);
                }
                break;
            case ListActivity.NEW_BUTTON:
                // Returning from NewNoteActivity
                if (resultCode == ListActivity.EDIT_BUTTON) {
                    //String res = data.getStringExtra(SAVE_ITEM);
                    String txt = ImapNotes3.AvoidLargeBundle; //data.getStringExtra(EDIT_ITEM_TXT);
                    //Log.d(TAG,"Received request to save message:"+res);
                    String bgcolor = data.getStringExtra(EDIT_ITEM_COLOR);
                    String accountName = data.getStringExtra(EDIT_ITEM_ACCOUNTNAME);
                    ListActivity.ImapNotesAccount = new ImapNotesAccount(getAccountFromName(accountName), getApplicationContext());
                    UpdateList("", txt, bgcolor, accountName, UpdateThread.Action.Insert);
                }
                break;
            case ListActivity.ADD_ACCOUNT:
                Log.d(TAG, "onActivityResult AccountsUpdateListener");
                // Hack! accountManager.addOnAccountsUpdatedListener
                if (resultCode == ResultCodeSuccess) {
                    EnableAccountsUpdate = true;
                    ListActivity.accountManager.addOnAccountsUpdatedListener(
                            new AccountsUpdateListener(), null, true);
                    if (data != null) {
                        Integer pos = getSpinnerPos(data.getStringExtra(ACCOUNTNAME));
                        if (pos > 0) {
                            accountSpinner.setSelection(pos);
                            Account account = ListActivity.accounts[pos - 1];
                            ListActivity.ImapNotesAccount = new ImapNotesAccount(account, getApplicationContext());
                        }
                    }
                    ;
                }
                break;
            default:
                Log.d(TAG, "Received wrong request to save message");
        }
    }

    ;

    public Account getAccountFromName(String accountname) {
        for (Account account : ListActivity.accounts
        ) {
            if (account.name.equals(accountname))
                return account;
        }
        return null;
    }

    ;

    private void updateAccountSpinner() {
        Log.d(TAG, "updateAccountSpinner");
        this.accountSpinner.setEnabled(true);
        this.spinnerList.notifyDataSetChanged();
        setPreferences();
        long id = this.accountSpinner.getSelectedItemId();
        // only one account active..disable selection
        if (ListActivity.currentList.size() == 2) {
            this.accountSpinner.setEnabled(false);
            this.accountSpinner.setSelection(1);
            id = 1;
        }
        if ((id == android.widget.AdapterView.INVALID_ROW_ID) || (id >= ListActivity.currentList.size())) {
            this.accountSpinner.setSelection(1);
            id = 1;
        }

        if ((ListActivity.currentList.size() > 1) && (id >= 1)) {
            Account account = ListActivity.accounts[(int) id - 1];
            ListActivity.ImapNotesAccount = new ImapNotesAccount(account, getApplicationContext());
        }
        // FIXME his place is not nice..but no other is working
        Check_Action_Send(null);

    }

    // In case of necessary debug  with user approval
    public void SendLogcatMail() {
        Log.d(TAG, "SendLogcatMail");
        String emailData = "";
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            emailData = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //send file using email
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        String[] to = {""};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        // the attachment
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailData);
        // the mail subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Logcat content for " + Utilities.FullApplicationName + " debugging");
        emailIntent.setType("message/rfc822");
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    @Nullable
    @Override
    public Filter getFilter() {
        return null;
    }

    private class AccountsUpdateListener implements OnAccountsUpdateListener {

        @Override
        public void onAccountsUpdated(@NonNull Account[] accounts) {
            Log.d(TAG, "onAccountsUpdated");
            List<String> newList;
            //Integer newListSize = 0;
            //invoked when the AccountManager starts up and whenever the account set changes
            ArrayList<Account> newAccounts = new ArrayList<>();
            for (final Account account : accounts) {
                if (account.type.equals(Utilities.PackageName)) {
                    newAccounts.add(account);
                }
            }
            // Hack! accountManager.addOnAccountsUpdatedListener
            if ((newAccounts.size() > 0) & (EnableAccountsUpdate)) {
                Account[] ImapNotesAccounts = new Account[newAccounts.size()];
                int i = 0;
                for (final Account account : newAccounts) {
                    ImapNotesAccounts[i] = account;
                    i++;
                }
                ListActivity.accounts = ImapNotesAccounts;
                newList = new ArrayList<>();
                newList.add(getString(R.string.all_accounts));
                for (Account account : ListActivity.accounts) {
                    newList.add(account.name);
                }
                if (newList.size() == 1) return;

                boolean equalLists = true;
                ListIterator<String> iter = ListActivity.currentList.listIterator();
                boolean first = true;
                while (iter.hasNext()) {
                    // skip first entry (All)
                    if (first) iter.next();
                    first = false;
                    String s = iter.next();

                    if (!(newList.contains(s))) {
                        iter.remove();
                        // Why try here?
                        try {
                            FileUtils.deleteDirectory(ImapNotes3.GetAccountDir(s));
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        equalLists = false;
                    }
                }
                first = true;
                for (String accountName : newList) {
                    if (!(ListActivity.currentList.contains(accountName))) {
                        ListActivity.currentList.add(accountName);
                        equalLists = false;
                        // skip first entry (All)
                        if (!first)
                            SyncUtils.CreateLocalDirectories(ImapNotes3.GetAccountDir(accountName));
                    }
                    first = false;
                }
                if (equalLists) return;
                updateAccountSpinner();
            } else {
                // Hack! accountManager.addOnAccountsUpdatedListener
                if (EnableAccountsUpdate) {
                    File filesDir = ImapNotes3.GetRootDir();
                    EnableAccountsUpdate = false;
                    ListActivity.accountManager.removeOnAccountsUpdatedListener(new AccountsUpdateListener());
                    try {
                        FileUtils.cleanDirectory(filesDir);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (Error err) {
                        // TODO Auto-generated catch block
                        err.printStackTrace();
                    }

                    Intent res = new Intent();
                    String mPackage = Utilities.PackageName;
                    String mClass = ".AccountConfigurationActivity";
                    res.setComponent(new ComponentName(mPackage, mPackage + mClass));
                    // Hack! accountManager.addOnAccountsUpdatedListener
                    startActivityForResult(res, ListActivity.ADD_ACCOUNT);
                }
            }
        }
    }
}

