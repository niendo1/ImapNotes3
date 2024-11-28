/*
 * Copyright (C) 2022-2023 - Peter Korf <peter@niendo.de>
 * Copyright (C) ?   -2022 - kwhitefoot
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

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * Represents metadata about a note in a way that can be used by a ListAdapter.  The list adapter
 * needs objects that have a map interface because it must fetch the items by string name.
 */
public class OneNote extends HashMap<String, String> {
    public static final String SAVE_STATE_OK = "";
    public static final String SAVE_STATE_SAVING = "SAVE_STATE_SAVING";
    public static final String SAVE_STATE_SYNCING = "SAVE_STATE_SYNCING";
    public static final String SAVE_STATE_FAILED = "SAVE_STATE_FAILED";
    public static final String TITLE = "title";
    public static final String DATE = "date";
    public static final String BGCOLOR = "bgColor";
    public static final String UID = "uid";
    public static final String ACCOUNT = "account";
    public static final String SAVE_STATE = "save_state";

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    public OneNote(@NonNull String title, @NonNull String date, @NonNull String uid, @NonNull String account, @NonNull String bgColor, @NonNull String saveState) {
        super();
        put(TITLE, title);
        put(DATE, date);
        put(UID, uid);
        put(ACCOUNT, account);
        put(BGCOLOR, bgColor);
        put(SAVE_STATE, saveState);
    }

    @NonNull
    public String GetTitle() {
        return this.get(TITLE).replace("#", "");
    }

    @NonNull
    String GetDate() {
        return this.get(DATE);
    }

    @NonNull
    public String GetUid() {
        return this.get(UID);
    }

    @NonNull
    public String GetAccount() {
        return this.get(ACCOUNT);
    }

    @NonNull
    public String GetBgColor() {
        return this.get(BGCOLOR);
    }

    @NonNull
    public String GetState() {
        return this.get(SAVE_STATE);
    }

    public void SetDate(String date) {
        this.put(DATE, date);
    }

    public void SetUid(String uid) {
        this.put(UID, uid);
    }

    public void SetBGColor(String bgColor) {
        this.put(BGCOLOR, bgColor);
    }

    public void SetState(String saveState) {
        this.put(SAVE_STATE, saveState);
    }

    @NonNull
    @Override
    public String toString() {
        return ("Title:" + this.GetTitle() +
                " Date: " + this.GetDate() +
                " BgColor: " + this.GetBgColor() +
                " Account: " + this.GetAccount() +
                " State: " + this.GetState() +
                " Uid: " + this.GetUid());
    }
}
