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
        String h = "<b>";
        String hf = ": </b>";
        String h1 = "<em>";
        String h1f = ":</em>";
        String d = " ";
        String df = "<br>";


        String about = "";
        about += h + getString(R.string.license) + hf + d + "<a href=\"https://www.gnu.org/licenses/gpl-3.0.html\">GPL v3.0</a>" + df;
        about += h + "ID" + hf + d + BuildConfig.APPLICATION_ID + df;
        about += h + "Version" + hf + d + BuildConfig.VERSION_NAME + df;
        about += h + "Code" + hf + d + BuildConfig.VERSION_CODE + df;
        about += h + "DB-Version" + hf + d + NotesDb.NOTES_VERSION + df;
        about += h + "Build typ" + hf + d + BuildConfig.BUILD_TYPE + df;
        about += h + getString(R.string.internet) + hf + d + "<a href=\"https://github.com/niendo1/ImapNotes3/\">github.com</a>" + df;
        about += h + getString(R.string.appstore) + hf + d + "<a href=\"" + getString(R.string.appstorelink) + "\">" + getString(R.string.appstorename) + "</a>" + df;
        about += "<br>";
        about += h + getString(R.string.translation) + hf + d + "<a href=\"https://hosted.weblate.org/projects/ImapNotes3/\">weblate.org</a>" + df;
        about += h1 + getString(R.string.chinese) + h1f;
        about += d + "sr093906, hamburger2048, pyccl, Xue Xuan, 大王叫我来巡山" + df;
        about += h1 + getString(R.string.czech) + h1f;
        about += d + "LibreTranslate" + df;
        about += h1 + getString(R.string.french) + h1f;
        about += d + "z97febao, e2jk" + df;
        about += h1 + getString(R.string.german) + h1f;
        about += d + "niendo1" + df;
        about += h1 + getString(R.string.italian) + h1f;
        about += d + "77nnit" + df;
        about += h1 + getString(R.string.norwegian) + h1f;
        about += d + "kwhitefood" + df;
        about += h1 + getString(R.string.portuguese) + h1f;
        about += d + "weblate" + df;
        about += h1 + getString(R.string.russian) + h1f;
        about += d + "pazengaz" + df;
        about += h1 + getString(R.string.spanish) + h1f;
        about += d + "gallegonovato" + df;

        about += "<br>";
        about += h + getString(R.string.contributors) + hf + "<br>";
        about += d + "nb(enm), c0238, Axel Strübing, Poussinou, woheller69, Martin Carpella, john-p-williams, ..." + df;

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