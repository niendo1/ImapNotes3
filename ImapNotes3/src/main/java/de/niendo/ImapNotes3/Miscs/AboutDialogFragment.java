/*
 * Copyright (C) 2024      - Peter Korf <peter@niendo.de>
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import de.niendo.ImapNotes3.BuildConfig;
import de.niendo.ImapNotes3.Data.NotesDb;
import de.niendo.ImapNotes3.R;

public class AboutDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String about = getString(R.string.license) + " <a href=\"https://www.gnu.org/licenses/gpl-3.0.html\">GPL v3.0</a><br>";
        about += "ID: " + BuildConfig.APPLICATION_ID + "<br>";
        about += "Version: " + BuildConfig.VERSION_NAME + "<br>";
        about += "Code: " + BuildConfig.VERSION_CODE + "<br>";
        about += "DB-Version: " + NotesDb.NOTES_VERSION + "<br>";
        about += "Build typ: " + BuildConfig.BUILD_TYPE + "<br>";
        about += getString(R.string.internet) + " <a href=\"https://github.com/niendo1/ImapNotes3/\">github.com/niendo1/ImapNotes3</a><br>";
        about += getString(R.string.appstore) + " <a href=\"" + getString(R.string.appstorelink) + "\">" + getString(R.string.appstorename) + "</a><br>";
        builder.setTitle(R.string.about)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(Html.fromHtml(about, Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(getString(R.string.ok), (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            TextView messageTextView = dialog.findViewById(android.R.id.message);
            if (messageTextView != null) {
                messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        });

        return dialog;
    }
}