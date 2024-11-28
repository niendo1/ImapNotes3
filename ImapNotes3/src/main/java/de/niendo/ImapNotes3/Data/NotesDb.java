/*
 * Copyright (C) 2022-2024 - Peter Korf <peter@niendo.de>
 * Copyright (C) ?   -2016 - Martin Carpella
 * Copyright (C) ?   -2015 - nb
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.niendo.ImapNotes3.Miscs.Utilities;

public class NotesDb extends SQLiteOpenHelper {

    private static final String TAG = "IN_NotesDb";


    private static final String COL_TITLE_NOTE = "title";

    private static final String COL_DATE = "date";
    private static final String COL_NUMBER = "number";
    private static final String COL_ACCOUNT_NAME = "accountname";
    private static final String COL_BGCOLOR = "bgcolor";
    private static final String COL_SAVE_STATE = "saveState";
    private static final String TABLE_NAME_NOTES = "notesTable";
    private static final String COL_TITLE_TAG = "tag";
    private static final String TABLE_NAME_TAGS = "tagsTable";
    public static final String CREATE_TAGS_DB = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME_TAGS
            + " (pk integer primary key autoincrement, "
            + COL_NUMBER + " text not null, "
            + COL_TITLE_TAG + " text not null, "
            + COL_BGCOLOR + " text not null, "
            + COL_ACCOUNT_NAME + " text not null);";

    public static final String CREATE_NOTES_DB = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME_NOTES
            + " (pk integer primary key autoincrement, "
            + COL_TITLE_NOTE + " text not null, "
            + COL_DATE + " text not null, "
            + COL_NUMBER + " text not null, "
            + COL_BGCOLOR + " text not null, "
            + COL_SAVE_STATE + " text not null, "
            + COL_ACCOUNT_NAME + " text not null);";
    private static final String VIEW_NAME_TAGS = "tagsView";
    public static final String CREATE_TAGS_VIEW = "CREATE VIEW IF NOT EXISTS "
            + VIEW_NAME_TAGS
            + "\n AS SELECT "
            + TABLE_NAME_NOTES + "." + COL_NUMBER + " AS " + COL_NUMBER + ","
            + COL_TITLE_NOTE + ","
            + COL_DATE + ","
            + TABLE_NAME_NOTES + "." + COL_BGCOLOR + " AS " + COL_BGCOLOR + ","
            + TABLE_NAME_NOTES + "." + COL_ACCOUNT_NAME + " AS " + COL_ACCOUNT_NAME + ","
            + TABLE_NAME_NOTES + "." + COL_SAVE_STATE + " AS " + COL_SAVE_STATE + ","
            + TABLE_NAME_TAGS + "." + COL_TITLE_TAG + " AS " + COL_TITLE_TAG
            + "\n FROM "
            + TABLE_NAME_NOTES
            + "\n INNER JOIN " + TABLE_NAME_TAGS + " ON " + TABLE_NAME_NOTES + "." + COL_NUMBER + " = " + TABLE_NAME_TAGS + "." + COL_NUMBER
            + "\n AND " + TABLE_NAME_NOTES + "." + COL_ACCOUNT_NAME + " = " + TABLE_NAME_TAGS + "." + COL_ACCOUNT_NAME + ";";

    public static final int NOTES_VERSION = 4;
    private static final String DATABASE_NAME = "NotesDb";

    private static NotesDb instance = null;

    private NotesDb(@NonNull Context context) {
        super(context, DATABASE_NAME, null, NOTES_VERSION);
    }

    public static NotesDb getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new NotesDb(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_NOTES_DB);
        db.execSQL(CREATE_TAGS_DB);
        // Log.d(TAG, CREATE_TAGS_VIEW);
        db.execSQL(CREATE_TAGS_VIEW);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            //SQLiteDatabase db = this.getWritableDatabase();

            // Version 3: ImapNotes2
            // Version 4: new TABLE_NAME_TAGS and VIEW_NAME_TAGS and SaveState
            if (oldVersion < 3) {
                try {
                    db.execSQL("Drop table " + TABLE_NAME_NOTES + ";");
                } catch (Exception e) {
                }
                try {
                    db.execSQL("Drop table " + TABLE_NAME_TAGS + ";");
                } catch (Exception e) {
                }
                try {
                    db.execSQL("Drop view " + VIEW_NAME_TAGS + ";");
                } catch (Exception e) {
                }
            } else {
                try {
                    db.execSQL("ALTER TABLE " + TABLE_NAME_NOTES + " ADD " + COL_SAVE_STATE + " text not null DEFAULT '';");
                } catch (Exception e) {
                }
            }
            db.execSQL(CREATE_NOTES_DB);
            db.execSQL(CREATE_TAGS_DB);
            db.execSQL(CREATE_TAGS_VIEW);
        }
    }

    public synchronized void RemoveTempNumber(@NonNull OneNote noteElement) {
        SQLiteDatabase db = this.getWritableDatabase();
        // delete DS with TempNumber
        db.execSQL("delete from notesTable where number = '" + noteElement.GetUid() +
                "' and accountname = '" + noteElement.GetAccount() + "' and date = '" + noteElement.GetDate() + "'");
        db.close();
    }

    public synchronized void InsertANoteInDb(@NonNull OneNote noteElement) {
        RemoveTempNumber(noteElement);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues tableRow = new ContentValues();
        tableRow.put(COL_TITLE_NOTE, noteElement.GetTitle());
        tableRow.put(COL_DATE, noteElement.GetDate());
        tableRow.put(COL_NUMBER, noteElement.GetUid());
        tableRow.put(COL_BGCOLOR, noteElement.GetBgColor());
        tableRow.put(COL_ACCOUNT_NAME, noteElement.GetAccount());
        tableRow.put(COL_SAVE_STATE, noteElement.GetState());
        db.insert(TABLE_NAME_NOTES, null, tableRow);

        //Log.d(TAG, "note inserted");
        db.close();
    }

    public synchronized void DeleteANote(@NonNull String number,
                                         @NonNull String accountname) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from " + TABLE_NAME_NOTES + " where number = '" + number +
                "' and accountname = '" + accountname + "'");
        db.execSQL("delete from " + TABLE_NAME_TAGS + " where number = '" + number +
                "' and accountname = '" + accountname + "'");
        db.close();
    }

    public synchronized void UpdateANote(@NonNull String olduid,
                                         @NonNull String newuid,
                                         @NonNull String accountname) {
        /* TODO: use sql template and placeholders instead of string concatenation.
         */

        SQLiteDatabase db = this.getWritableDatabase();

        String req = "update notesTable set number='" + newuid + "' where number='" + olduid + "' and accountname='" + accountname + "'";
        db.execSQL(req);
        db.close();
    }

    public synchronized String GetDate(@NonNull String uid,
                                       @NonNull String accountname) {
       /* Returns a string representing the modification time of the note.
          TODO: use date class.
        */
        SQLiteDatabase db = this.getWritableDatabase();
        String RetValue = "";
        String selectQuery = "select date from notesTable where number = '" + uid + "' and accountname='" + accountname + "'";
        try (Cursor c = db.rawQuery(selectQuery, null)) {
            if (c.moveToFirst()) {
                RetValue = c.getString(0);
            }
        }

        db.close();
        return RetValue;
    }

    public synchronized String GetTempNumber(@NonNull OneNote noteElement) {
        String RetValue = "-1";
        String selectQuery = "select case when cast(max(abs(number)+2) as int) > 0 then cast(max(abs(number)+1) as int)*-1 " +
                "else '-1' end from notesTable where number < '0' and accountname='" + noteElement.GetAccount() + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor c = db.rawQuery(selectQuery, null)) {
            if (c.moveToFirst()) {
                RetValue = c.getString(0);
            }
        }
        // Create DS with TempNumber, so it can not be given two times
        ContentValues tableRow = new ContentValues();
        tableRow.put(COL_TITLE_NOTE, "~" + noteElement.GetTitle());
        tableRow.put(COL_DATE, noteElement.GetDate());
        tableRow.put(COL_NUMBER, RetValue);
        tableRow.put(COL_ACCOUNT_NAME, noteElement.GetAccount());
        tableRow.put(COL_BGCOLOR, "");
        tableRow.put(COL_SAVE_STATE, "");
        db.insert(TABLE_NAME_NOTES, null, tableRow);
        db.close();
        return (RetValue);
    }

    public synchronized void GetStoredNotes(@NonNull ArrayList<OneNote> noteList,
                                            @NonNull String accountName,
                                            @NonNull String sortOrder,
                                            String[] hashFilter) {
        Log.d(TAG, "GetStoredNotes:" + accountName + " " + sortOrder);
        String table = TABLE_NAME_NOTES;
        noteList.clear();
        Date date = null;
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = "1=1";
        String GroupBy = "";
        List<String> selectionArgsList = new ArrayList<String>();

        if (!(accountName.isEmpty())) {
            selection += " AND " + COL_ACCOUNT_NAME + " = ?";
            selectionArgsList.add(accountName);
        }

        if (!(hashFilter == null)) {
            table = VIEW_NAME_TAGS;
            selection += " AND (1=2";
            for (String filter : hashFilter) {
                selection += " OR " + COL_TITLE_TAG + " = ?";
                selectionArgsList.add(filter);
            }
            selection += ")";
            GroupBy = COL_ACCOUNT_NAME + "," + COL_NUMBER;
        }

        String[] selectionArgs = new String[selectionArgsList.size()];
        selectionArgsList.toArray(selectionArgs);

        try (Cursor resultPointer = db.query(table, null, selection,
                selectionArgs, GroupBy, null, sortOrder)) {
            if (resultPointer.moveToFirst()) {
                int titleIndex = resultPointer.getColumnIndex(COL_TITLE_NOTE);
                int AccountNameIndex = resultPointer.getColumnIndex(COL_ACCOUNT_NAME);
                int dateIndex = resultPointer.getColumnIndex(COL_DATE);
                int numberIndex = resultPointer.getColumnIndex(COL_NUMBER);
                int bgColorIndex = resultPointer.getColumnIndex(COL_BGCOLOR);
                int bgSaveStateIndex = resultPointer.getColumnIndex(COL_SAVE_STATE);
                do {
                    //String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
                    //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.ROOT);
                    try {
                        date = Utilities.internalDateFormat.parse(resultPointer.getString(dateIndex));
                    } catch (ParseException e) {
                        Log.d(TAG, "Parsing data from database failed: " + e.getMessage());
                    }
                    //DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(this.ctx);
                    //String sdate = dateFormat.format(date);
                    String sdate;
                    try {
                        sdate = DateFormat.getDateTimeInstance().format(date);
                    } catch (Exception e) {
                        sdate = "?";
                    }
                    if (accountName.isEmpty()) {
                        sdate = sdate + "  (" + resultPointer.getString(AccountNameIndex) + ")";
                    }
                    noteList.add(new OneNote(resultPointer.getString(titleIndex),
                            sdate,
                            resultPointer.getString(numberIndex),
                            resultPointer.getString(AccountNameIndex),
                            resultPointer.getString(bgColorIndex),
                            resultPointer.getString(bgSaveStateIndex)));

                } while (resultPointer.moveToNext());
                // Log.d(TAG,"GetStoredNotes: Anzahl" + resultPointer.getCount());
                resultPointer.close();
            }
        }
        db.close();
    }

    public synchronized void SetSaveState(@NonNull String uid, @NonNull String saveState, @NonNull String accountname) {
        SQLiteDatabase db = this.getWritableDatabase();
        String req = "update notesTable set saveState='" + saveState + "' where number='" + uid + "' and accountname='" + accountname + "'";
        db.execSQL(req);
        db.close();
        // Log.d(TAG,"SetSaveState:" + uid + "-->" + saveState + " Account "+ accountname);
    }

    ;

    public synchronized String GetSaveState(@NonNull String uid, @NonNull String accountname) {
        SQLiteDatabase db = this.getWritableDatabase();
        String RetValue = OneNote.SAVE_STATE_SYNCING;
        String selectQuery = "select saveState from notesTable where number = '" + uid + "' and accountname='" + accountname + "'";
        try (Cursor c = db.rawQuery(selectQuery, null)) {
            if (c.moveToFirst()) {
                RetValue = c.getString(0);
            }
        }
        db.close();
        //Log.d(TAG,"GetSaveState:" + uid + "-->" + RetValue + " Account "+ accountname);
        return RetValue;
    }

    ;


    public synchronized void UpdateTags(@NonNull List<String> tags, @NonNull String uid, @NonNull String accountname) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from " + TABLE_NAME_TAGS + " where accountname = '" + accountname + "' and " + COL_NUMBER + "= '" + uid + "'");
        ContentValues tableRow = new ContentValues();
        tableRow.put(COL_NUMBER, uid);
        tableRow.put(COL_BGCOLOR, "");
        tableRow.put(COL_ACCOUNT_NAME, accountname);
        for (String tag : tags) {
            tableRow.put(COL_TITLE_TAG, tag);
            db.insert(TABLE_NAME_TAGS, null, tableRow);
        }
        db.close();
    }

    public synchronized List<String> GetTags(@NonNull String uid, @NonNull String accountName) {
        List<String> retVal = new ArrayList<String>();

        SQLiteDatabase db = this.getWritableDatabase();
        String selection = "";
        String[] selectionArgs = new String[]{};
        if (!(accountName.isEmpty())) {
            selection = COL_ACCOUNT_NAME + " = ?";
            selectionArgs = new String[]{accountName};
            if (!(uid.isEmpty())) {
                selection = COL_ACCOUNT_NAME + " = ? AND " + COL_NUMBER + " = ?";
                selectionArgs = new String[]{accountName, uid};
            }
        } else if (!(uid.isEmpty())) {
            selection = COL_NUMBER + " = ?";
            selectionArgs = new String[]{uid};
        }

        try (Cursor resultPointer = db.query(TABLE_NAME_TAGS, null, selection,
                selectionArgs, COL_TITLE_TAG, null, "UPPER(" + COL_TITLE_TAG + ") ASC")) {

            if (resultPointer.moveToFirst()) {
                int titleIndex = resultPointer.getColumnIndex(COL_TITLE_TAG);
                do {
                    retVal.add(resultPointer.getString(titleIndex));
                } while (resultPointer.moveToNext());
                resultPointer.close();
            }
            db.close();
            return retVal;
        }
    }


    public synchronized void ClearDb(@NonNull String accountname) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from " + TABLE_NAME_NOTES + " where accountname = '" + accountname + "'");
        db.execSQL("delete from " + TABLE_NAME_TAGS + " where accountname = '" + accountname + "'");
        db.close();
    }
}
