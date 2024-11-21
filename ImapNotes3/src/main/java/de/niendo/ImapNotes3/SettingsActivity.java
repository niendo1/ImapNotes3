/*
 * Copyright (C) 2024-2024 - Peter Korf <peter@niendo.de>
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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.niendo.ImapNotes3.Miscs.AboutDialogFragment;
import de.niendo.ImapNotes3.Miscs.Utilities;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.color.SimpleColorDialog;


public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "IN_SettingsActivity";
    public static final String MAIN_PREFERENCE_NAME = Utilities.PackageName + "_preferences";
    private final SettingsFragment settingsFragment = new SettingsFragment();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        settingsFragment.setActivity(this);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, settingsFragment)
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setResult(ListActivity.ResultCodeSuccess);
    }


    public static class SettingsFragment extends PreferenceFragmentCompat implements SimpleDialog.OnDialogResultListener {
        public static final String DLG_PREF_EDITOR_BG_COLOR = "EditorBgColorDefault";
        public static final String DLG_PREF_EDITOR_TXT_COLOR = "EditorTxtColor";
        public static SettingsActivity settingsActivity;

        public void setActivity(SettingsActivity activity) {
            settingsActivity = activity;
        }
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);


            Preference myPref = findPreference(DLG_PREF_EDITOR_BG_COLOR);
            myPref.setOnPreferenceClickListener(preference -> {
                int pallet = getString(R.string.ColorMode).equals("light") ? SimpleColorDialog.MATERIAL_COLOR_PALLET_LIGHT : SimpleColorDialog.MATERIAL_COLOR_PALLET_DARK;
                SimpleColorDialog.build()
                        .colors(getContext(), pallet)
                        .title(getString(R.string.selectBgColor))
                        .colorPreset(ImapNotes3.loadPreferenceColor(DLG_PREF_EDITOR_BG_COLOR, getResources().getColor(R.color.EditorBgColorDefault)))
                        .setupColorWheelAlpha(false)
                        .allowCustom(true)
                        .neg(R.string.cancel)
                        .neut(R.string.default_color)
                        //.show((FragmentActivity) getContext(), DLG_PREF_EDITOR_BG_COLOR);
                        .show(this, DLG_PREF_EDITOR_BG_COLOR);
                return true;
            });

            myPref = findPreference(DLG_PREF_EDITOR_TXT_COLOR);
            myPref.setOnPreferenceClickListener(preference -> {
                int pallet = getString(R.string.ColorMode).equals("light") ? SimpleColorDialog.MATERIAL_COLOR_PALLET_DARK : SimpleColorDialog.MATERIAL_COLOR_PALLET_LIGHT;
                SimpleColorDialog.build()
                        .colors(getContext(), pallet)
                        .title(getString(R.string.selectTextColor))
                        .colorPreset(ImapNotes3.loadPreferenceColor(DLG_PREF_EDITOR_TXT_COLOR, getResources().getColor(R.color.EditorTxtColor)))
                        .setupColorWheelAlpha(false)
                        .allowCustom(true)
                        .neg(R.string.cancel)
                        .neut(R.string.default_color)
                        .show(this, DLG_PREF_EDITOR_TXT_COLOR);
                return true;
            });

            myPref = findPreference("make_archive");
            myPref.setOnPreferenceClickListener(preference -> {

                settingsActivity.setResult(ListActivity.ResultCodeMakeArchive);
                settingsActivity.finish();
                return true;
            });

            myPref = findPreference("restore_archive");
            myPref.setOnPreferenceClickListener(preference -> {

                settingsActivity.setResult(ListActivity.ResultCodeRestoreArchive);
                settingsActivity.finish();
                return true;
            });


            myPref = findPreference("send_debug_report");
            myPref.setOnPreferenceClickListener(preference -> {
                SendLogcatMail();
                return true;
            });

            myPref = findPreference("about");
            myPref.setOnPreferenceClickListener(preference -> {
                AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
                aboutDialogFragment.show(((FragmentActivity) getContext()).getSupportFragmentManager(), "about_dialog");
                return true;
            });

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

        @Override
        public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
            if (which == BUTTON_NEGATIVE) return false;
            switch (dialogTag) {
                case DLG_PREF_EDITOR_BG_COLOR: {
                    if (which == BUTTON_POSITIVE) {
                        ImapNotes3.savePreferenceColor(DLG_PREF_EDITOR_BG_COLOR, extras.getInt(SimpleColorDialog.COLOR));
                    }
                    if (which == BUTTON_NEUTRAL) {
                        ImapNotes3.savePreferenceColor(DLG_PREF_EDITOR_BG_COLOR, getResources().getColor(R.color.EditorBgColorDefault));
                    }
                    return true;
                }
                case DLG_PREF_EDITOR_TXT_COLOR: {
                    if (which == BUTTON_POSITIVE) {
                        ImapNotes3.savePreferenceColor(DLG_PREF_EDITOR_TXT_COLOR, extras.getInt(SimpleColorDialog.COLOR));
                    }
                    if (which == BUTTON_NEUTRAL) {
                        ImapNotes3.savePreferenceColor(DLG_PREF_EDITOR_TXT_COLOR, getResources().getColor(R.color.EditorTxtColor));
                    }
                    return true;
                }
            }
            return false;
        }
    }


}