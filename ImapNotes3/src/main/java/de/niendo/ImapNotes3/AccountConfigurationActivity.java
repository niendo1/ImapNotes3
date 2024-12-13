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

package de.niendo.ImapNotes3;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import de.niendo.ImapNotes3.Data.ConfigurationFieldNames;
import de.niendo.ImapNotes3.Data.ImapNotesAccount;
import de.niendo.ImapNotes3.Data.Security;
import de.niendo.ImapNotes3.Data.SyncInterval;
import de.niendo.ImapNotes3.Miscs.LoginThread;
import de.niendo.ImapNotes3.Miscs.Result;
import de.niendo.ImapNotes3.Miscs.SmtpServerNameFinder;
import de.niendo.ImapNotes3.Miscs.Utilities;
import eltos.simpledialogfragment.SimpleDialog;

import java.util.List;

public class AccountConfigurationActivity extends AccountAuthenticatorActivity implements OnItemSelectedListener, SimpleDialog.OnDialogResultListener, LoginThread.FinishListener {
    /**
     * Cannot be final or NonNull because it needs the application context which is not available
     * until onCreate.
     */

    public static final String ACTION = "ACTION";
    public static final String ACCOUNTNAME = "ACCOUNTNAME";
    public static final int TO_REFRESH = 999;
    public static final String AUTHORITY = Utilities.PackageName + ".provider";
    private static final String TAG = "IN_AccountConfActivity";


    @Nullable
    private static Account myAccount = null;
    private static AccountManager accountManager;
    private final OnClickListener clickListenerRemove = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Click on Remove Button
            accountManager.removeAccount(myAccount, null, null, null);
            ImapNotes3.ShowMessage(R.string.account_removed, accountnameTextView, 3);
            finish();//finishing activity
        }
    };
    private AppCompatDelegate mDelegate;
    private TextView accountnameTextView;
    private TextView usernameTextView;
    private TextView passwordTextView;
    private TextView serverTextView;
    private TextView portnumTextView;
    private Spinner syncIntervalSpinner;
    private TextView folderTextView;
    private TextView copyImapFolderNameTextView;
    private final CheckBox.OnCheckedChangeListener FinishCopyFolderCheckBox = (v, r) -> {
        copyImapFolderNameTextView.setEnabled(v.isChecked());
        if (GetTextViewText(copyImapFolderNameTextView).isEmpty())
            copyImapFolderNameTextView.setText("Trash");
    };
    private Spinner securitySpinner;
    @NonNull
    private SyncInterval syncInterval = SyncInterval.t6h;
    @NonNull
    private Security security = Security.None;
    @Nullable
    private String accountname;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        return false;
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    /* is this important?
    @Override
    @NonNull
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }
  */
    //@Override
    public void onFinishPerformed(@NonNull Result<String> result) {
        if (result.succeeded) {
            Intent intent = new Intent();
            intent.putExtra(ListActivity.EDIT_ITEM_ACCOUNTNAME, GetTextViewText(accountnameTextView));
            setResult(ListActivity.ResultCodeSuccess, intent);
            Clear();
            finish();
        } else {
            setResult(ListActivity.ResultCodeError);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.IMAP_operation_failed)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(result.result)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // Do nothing
                    })
                    .setNeutralButton(R.string.help, (dialog, which) -> {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.help)
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setMessage(R.string.imap_help_text)
                                .setPositiveButton(android.R.string.ok, (dialog1, which1) -> {
                                }).show();
                    })
                    .show();
            /*
            SimpleDialog.build()
                    .title("R.string.hello")
                    .msg("R.string.hello_world")
                    .show(ListActivity);

             */
        }

    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    @Nullable
    private Actions action;

    private final OnClickListener clickListenerLogin = v -> {
        // Click on Login Button
        Log.d(TAG, "clickListenerLogin  onClick");
        CheckNameAndLogIn();
    };
    private final OnClickListener clickListenerSave = v -> {
        // Click on Save Button
        Log.d(TAG, "clickListenerSave onClick");
        CheckNameAndLogIn();
    };
    private final OnClickListener clickListenerAbort = v -> {
        // Click on Abort Button
        Log.d(TAG, "clickListenerAbort onClick");
        finish();
    };
    private CheckBox copyImapFolderCheckBox;

    @SuppressLint("SetTextI18n")
    private final View.OnFocusChangeListener FinishEmailEdit = (v, r) -> {
        if (!v.hasFocus()) {
            String email = ((TextView) v).getText().toString();
            if (email.contains("@") && serverTextView.getText().toString().isEmpty()) {
                serverTextView.setText(SmtpServerNameFinder.getSmtpServerName(email));
            }
        }
    };

    /*
        private final TextWatcher textWatcher = new TextWatcher(){

            public void beforeTextChanged(CharSequence chars, int start, int count, int after){}
            public void afterTextChanged(Editable editable){}
            public void onTextChanged(CharSequence chars, int start, int before, int count) {

            }

        };*/

    private void CheckNameAndLogIn() {
        String name = accountnameTextView.getText().toString();
        if (name.isEmpty()) {
            name = GetTextViewText(usernameTextView);
            accountnameTextView.setText(name);
        }
        if (name.contains("'") || name.contains("\""))
            ImapNotes3.ShowMessage(R.string.quotation_marks_not_allowed, accountnameTextView, 3);
        else
            DoLogin();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView headingTextView = findTextViewById(R.id.heading);
        accountnameTextView = findTextViewById(R.id.accountnameEdit);
        usernameTextView = findTextViewById(R.id.usernameEdit);
        usernameTextView.setOnFocusChangeListener(FinishEmailEdit);
        passwordTextView = findTextViewById(R.id.passwordEdit);
        serverTextView = findTextViewById(R.id.serverEdit);
        portnumTextView = findTextViewById(R.id.portnumEdit);
        syncIntervalSpinner = findViewById(R.id.syncintervalSpinner);
        List<String> listInterval = SyncInterval.Printables(getResources());
        ArrayAdapter<String> dataAdapterInterval = new ArrayAdapter<>
                (this, R.layout.ssl_spinner_item, listInterval);
        syncIntervalSpinner.setAdapter(dataAdapterInterval);
        syncIntervalSpinner.setSelection(SyncInterval.t6h.ordinal());
        syncIntervalSpinner.setOnItemSelectedListener(this);

        folderTextView = findTextViewById(R.id.folderEdit);
        copyImapFolderNameTextView = findTextViewById(R.id.copyImapFolderName);
        copyImapFolderCheckBox = (CheckBox) findTextViewById(R.id.copyImapFolder);
        copyImapFolderCheckBox.setOnCheckedChangeListener(FinishCopyFolderCheckBox);
        securitySpinner = findViewById(R.id.securitySpinner);
        List<String> list = Security.Printables(getResources());
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>
                (this, R.layout.ssl_spinner_item, list);
        securitySpinner.setAdapter(dataAdapter);
        securitySpinner.setOnItemSelectedListener(this);
        securitySpinner.setSelection(Security.SSL_TLS.ordinal());

        Bundle extras = getIntent().getExtras();
        // TODO: find out if extras can be null.
        if (extras != null) {
            if (extras.containsKey(ACTION)) {
                action = (Actions) (extras.getSerializable(ACTION));
            }
            if (extras.containsKey(ACCOUNTNAME)) {
                accountname = extras.getString(ACCOUNTNAME);
            }
        }

        LinearLayout layout = findViewById(R.id.buttonsLayout);
        accountManager = AccountManager.get(getApplicationContext());
        Account[] accounts = accountManager.getAccountsByType(Utilities.PackageName);
        for (Account account : accounts) {
            if (account.name.equals(accountname)) {
                myAccount = account;
                break;
            }
        }

        // action can never be null
        if (myAccount == null) {
            action = Actions.CREATE_ACCOUNT;
        }

        if (action == Actions.EDIT_ACCOUNT) {
            // Here we have to edit an existing account
            headingTextView.setText(R.string.editAccount);
            accountnameTextView.setText(accountname);
            accountnameTextView.setEnabled(false);
            usernameTextView.setText(GetConfigValue(ConfigurationFieldNames.UserName));
            //passwordTextView.setText(accountManager.getPassword(myAccount));
            serverTextView.setText(GetConfigValue(ConfigurationFieldNames.Server));
            portnumTextView.setText(GetConfigValue(ConfigurationFieldNames.PortNumber));
            //Log.d(TAG, "Security: " + GetConfigValue(ConfigurationFieldNames.Security));
            security = Security.from(GetConfigValue(ConfigurationFieldNames.Security));
            securitySpinner.setSelection(security.ordinal());
            syncInterval = SyncInterval.from(GetConfigValue(ConfigurationFieldNames.SyncInterval));
            syncIntervalSpinner.setSelection(syncInterval.ordinal());
            folderTextView.setText(GetConfigValue(ConfigurationFieldNames.ImapFolder));
            copyImapFolderNameTextView.setText(GetConfigValue(ConfigurationFieldNames.copyImapFolderName));

            String myTempStr = GetConfigValue(ConfigurationFieldNames.copyImapFolder);
            copyImapFolderCheckBox.setChecked(myTempStr != null && myTempStr.equals("true") && !(GetConfigValue(ConfigurationFieldNames.copyImapFolderName).isEmpty()));

            Button buttonAbort = new Button(this);
            buttonAbort.setText(R.string.cancel);
            Log.d(TAG, "Set onclick listener abort");
            buttonAbort.setOnClickListener(clickListenerAbort);
            layout.addView(buttonAbort);
            Button buttonRemove = new Button(this);
            buttonRemove.setText(R.string.remove);
            buttonRemove.setOnClickListener(clickListenerRemove);
            layout.addView(buttonRemove);
            Button buttonSave = new Button(this);
            buttonSave.setText(R.string.save);
            Log.d(TAG, "Set onclick listener save");
            buttonSave.setOnClickListener(clickListenerSave);
            layout.addView(buttonSave);
        } else {
            Button buttonAbort = new Button(this);
            buttonAbort.setText(R.string.cancel);
            Log.d(TAG, "Set onclick listener abort");
            buttonAbort.setOnClickListener(clickListenerAbort);
            layout.addView(buttonAbort);
            // Here we have to create a new account
            Button buttonView = new Button(this);
            buttonView.setText(R.string.check_and_create_account);
            Log.d(TAG, "Set onclick listener login");
            buttonView.setOnClickListener(clickListenerLogin);
            layout.addView(buttonView);
        }

        // Don't display keyboard when on note detail, only if user touches the screen
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
    }

    private TextView findTextViewById(int id) {
        return findViewById(id);
    }

    private String GetConfigValue(@NonNull String name) {
        return accountManager.getUserData(myAccount, name);
    }

    private String GetTextViewText(@NonNull TextView textView) {
        return textView.getText().toString().trim();
    }

    private String GetCheckBoxValue(@NonNull CheckBox checkBox) {
        return checkBox.isChecked() ? "true" : "false";
    }

    // DoLogin method is defined in account_selection.xml (account_selection layout)
    private void DoLogin() {
        Log.d(TAG, "DoLogin");

        //password will not shown if account is edit and have to be loaded;
        String password = GetTextViewText(passwordTextView);
        if ((action == Actions.EDIT_ACCOUNT) && (password.isEmpty())) {
            // Server name edited: new password required (avoid password spoofing)
            if (GetTextViewText(serverTextView).equals(GetConfigValue(ConfigurationFieldNames.Server))) {
                password = accountManager.getPassword(myAccount);
            } else {
                ImapNotes3.ShowMessage(R.string.imap_server_changed_new_password, accountnameTextView, 3);
                return;
            }
        }

        final ImapNotesAccount ImapNotesAccount = new ImapNotesAccount(
                GetTextViewText(accountnameTextView),
                GetTextViewText(usernameTextView),
                password,
                GetTextViewText(serverTextView),
                GetTextViewText(portnumTextView),
                security,
                syncInterval,
                GetTextViewText(folderTextView),
                GetTextViewText(copyImapFolderNameTextView),
                GetCheckBoxValue(copyImapFolderCheckBox));
        // No need to check for valid numbers because the field only allows digits.  But it is
        // possible to remove all characters which causes the program to crash.  The easiest fix is
        // to add a zero at the beginning so that we are guaranteed to be able to parse it but that
        // leaves us with a zero sync. interval.

        new LoginThread(
                ImapNotesAccount,
                this,
                action).execute();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.syncintervalSpinner) {
            syncInterval = SyncInterval.from(position);
        } else {
            if (!security.equals(Security.from(position))) {
                security = Security.from(position);
                portnumTextView.setText(security.defaultPort);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
    }

    public void Clear() {

        accountnameTextView.setText("");
        usernameTextView.setText("");
        passwordTextView.setText("");
        serverTextView.setText("");
        portnumTextView.setText("");
        syncIntervalSpinner.setSelection(0);
        securitySpinner.setSelection(0);
        folderTextView.setText("");
        copyImapFolderNameTextView.setText("");
        copyImapFolderCheckBox.setChecked(false);
    }


    /**
     *
     */
    public enum Actions {
        CREATE_ACCOUNT,
        EDIT_ACCOUNT
    }

}
