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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.niendo.ImapNotes3.Miscs.AboutDialogFragment;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.color.SimpleColorDialog;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat implements SimpleDialog.OnDialogResultListener {
        public static final String DLG_PREF_EDITOR_BG_COLOR = "EditorBgColorDefault";
        public static final String DLG_PREF_EDITOR_TXT_COLOR = "EditorTxtColor";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference myPref = findPreference(DLG_PREF_EDITOR_BG_COLOR);
            myPref.setOnPreferenceClickListener(preference -> {
                SimpleColorDialog.build()
                        .colors(getContext(), SimpleColorDialog.MATERIAL_COLOR_PALLET_DARK)
                        .title(getString(R.string.selectTextColor))
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
                SimpleColorDialog.build()
                        .colors(getContext(), SimpleColorDialog.MATERIAL_COLOR_PALLET_DARK)
                        .title(getString(R.string.selectTextColor))
                        .colorPreset(ImapNotes3.loadPreferenceColor(DLG_PREF_EDITOR_TXT_COLOR, getResources().getColor(R.color.EditorTxtColor)))
                        .setupColorWheelAlpha(false)
                        .allowCustom(true)
                        .neg(R.string.cancel)
                        .neut(R.string.default_color)
                        .show(this, DLG_PREF_EDITOR_TXT_COLOR);
                return true;
            });

            myPref = findPreference("about");
            myPref.setOnPreferenceClickListener(preference -> {
                AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
                aboutDialogFragment.show(((FragmentActivity) getContext()).getSupportFragmentManager(), "about_dialog");
                return true;
            });

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