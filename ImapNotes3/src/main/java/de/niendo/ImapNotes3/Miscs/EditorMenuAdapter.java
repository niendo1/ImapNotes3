/*
 * Copyright (C) 2022-2023 - Peter Korf <peter@niendo.de>
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import de.niendo.ImapNotes3.NoteDetailActivity;
import de.niendo.ImapNotes3.R;

public class EditorMenuAdapter extends ArrayAdapter<String> {
    @NonNull
    private final LayoutInflater mInflater;
    private final int mSpinnerResourceId;
    private final Context mContext;
    private final int mTextViewResourceId;
    private final NoteDetailActivity mNoteDetailActivity;


    public EditorMenuAdapter(Context context, int textViewResourceId, String[] objects,
                             int spinnerResourceId, NoteDetailActivity noteDetailActivity) {
        super(context, textViewResourceId, objects);

        mTextViewResourceId = textViewResourceId;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSpinnerResourceId = spinnerResourceId;
        mContext = context;
        mNoteDetailActivity = noteDetailActivity;
// TODO Auto-generated constructor stub
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                @NonNull ViewGroup parent) {
// TODO Auto-generated method stub
        return getCustomView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
// TODO Auto-generated method stub
        return getCustomView(position, convertView, parent);
    }


    public View getCustomView(int position, View convertView, ViewGroup parent) {
// TODO Auto-generated method stub
//return super.getView(position, convertView, parent);
        View row = mInflater.inflate(mTextViewResourceId, parent, false);
        ImageView icon = row.findViewById(R.id.row_icon_dummy);
        NDSpinner spinner;
            /*
            row.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           switch (mSpinnerResourceId) {
                                               case R.id.action_format:
                                                   switch (position) {
                                                       case 0:
                                                           mEditor.setBold();
                                                       case 1:
                                                           mEditor.setItalic();
                                                   }
                                           }
                                       }


                                   });
        //row.setOnLongClickListener(v -> mEditor.setItalic());
*/
        switch (mSpinnerResourceId) {
            case R.id.action_format:
                switch (position) {
                    case 0:
                        row.setId(R.id.action_removeFormat);
                        icon.setImageResource(R.drawable.remove_format);
                        break;
                    case 1:
                        row.setId(R.id.action_bold);
                        icon.setImageResource(R.drawable.bold);
                        break;
                    case 2:
                        row.setId(R.id.action_italic);
                        icon.setImageResource(R.drawable.italic);
                        break;
                    case 3:
                        row.setId(R.id.action_subscript);
                        icon.setImageResource(R.drawable.subscript);
                        break;
                    case 4:
                        row.setId(R.id.action_superscript);
                        icon.setImageResource(R.drawable.superscript);
                        break;
                    case 5:
                        row.setId(R.id.action_strikethrough);
                        icon.setImageResource(R.drawable.strikethrough);
                        break;
                    case 6:
                        row.setId(R.id.action_underline);
                        icon.setImageResource(R.drawable.underline);
                        break;
                    case 7:
                        row.setId(R.id.action_txt_color);
                        icon.setImageResource(R.drawable.txt_color);
                        break;
                    case 8:
                        row.setId(R.id.action_bg_color);
                        icon.setImageResource(R.drawable.bg_color);
                        break;
                    case 9:
                        icon.setImageResource(0);
                        icon.setVisibility(View.INVISIBLE);
                        icon.setPadding(0, 0, 0, 0);
                        spinner = row.findViewById(R.id.row_spinner_dummy);
                        spinner.setId(R.id.action_font_size);
                        spinner.setVisibility(View.VISIBLE);
                        spinner.setAdapter(new EditorMenuAdapter(mContext, R.layout.editor_row, new String[7], R.id.action_font_size, mNoteDetailActivity));
                        spinner.setOnItemSelectedListener(mNoteDetailActivity);
                        break;
                    case 10:
                        icon.setImageResource(0);
                        icon.setVisibility(View.INVISIBLE);
                        icon.setPadding(0, 0, 0, 0);
                        spinner = row.findViewById(R.id.row_spinner_dummy);
                        spinner.setId(R.id.action_font_family);
                        spinner.setVisibility(View.VISIBLE);
                        spinner.setAdapter(new EditorMenuAdapter(mContext, R.layout.editor_row, new String[5], R.id.action_font_family, mNoteDetailActivity));
                        spinner.setOnItemSelectedListener(mNoteDetailActivity);
                        break;
                }
                break;
            case R.id.action_heading:
                switch (position) {
                    case 0:
                        row.setId(R.id.action_insert_bullets);
                        icon.setImageResource(R.drawable.bullets);
                        break;
                    case 1:
                        row.setId(R.id.action_insert_numbers);
                        icon.setImageResource(R.drawable.numbers);
                        break;
                    case 2:
                        row.setId(R.id.action_heading1);
                        icon.setImageResource(R.drawable.h1);
                        break;
                    case 3:
                        row.setId(R.id.action_heading2);
                        icon.setImageResource(R.drawable.h2);
                        break;
                    case 4:
                        row.setId(R.id.action_heading3);
                        icon.setImageResource(R.drawable.h3);
                        break;
                    case 5:
                        row.setId(R.id.action_heading4);
                        icon.setImageResource(R.drawable.h4);
                        break;
                    case 6:
                        row.setId(R.id.action_heading5);
                        icon.setImageResource(R.drawable.h5);
                        break;
                    case 7:
                        row.setId(R.id.action_heading6);
                        icon.setImageResource(R.drawable.h6);
                        break;
                }
                break;
            case R.id.action_font_size:
                switch (position) {
                    case 0:
                        row.setId(R.id.action_font_size_1);
                        icon.setImageResource(R.drawable.font_size_1);
                        break;
                    case 1:
                        row.setId(R.id.action_font_size_2);
                        icon.setImageResource(R.drawable.font_size_2);
                        break;
                    case 2:
                        row.setId(R.id.action_font_size_3);
                        icon.setImageResource(R.drawable.font_size_3);
                        break;
                    case 3:
                        row.setId(R.id.action_font_size_4);
                        icon.setImageResource(R.drawable.font_size_4);
                        break;
                    case 4:
                        row.setId(R.id.action_font_size_5);
                        icon.setImageResource(R.drawable.font_size_5);
                        break;
                    case 5:
                        row.setId(R.id.action_font_size_6);
                        icon.setImageResource(R.drawable.font_size_6);
                        break;
                    case 6:
                        row.setId(R.id.action_font_size_7);
                        icon.setImageResource(R.drawable.font_size_7);
                        break;
                }
                break;
            case R.id.action_font_family:
                switch (position) {
                    case 0:
                        row.setId(R.id.action_font_serif);
                        icon.setImageResource(R.drawable.font_serif);
                        break;
                    case 1:
                        row.setId(R.id.action_font_sansserif);
                        icon.setImageResource(R.drawable.font_sansserif);
                        break;
                    case 2:
                        row.setId(R.id.action_font_cursive);
                        icon.setImageResource(R.drawable.font_cursive);
                        break;
                    case 3:
                        row.setId(R.id.action_font_monospace);
                        icon.setImageResource(R.drawable.font_monospace);
                        break;
                    case 4:
                        row.setId(R.id.action_font_fantasy);
                        icon.setImageResource(R.drawable.font_fantasy);
                        break;
                }
                break;
            case R.id.action_alignment:
                switch (position) {
                    case 0:
                        row.setId(R.id.action_indent);
                        icon.setImageResource(R.drawable.indent);
                        break;
                    case 1:
                        row.setId(R.id.action_outdent);
                        icon.setImageResource(R.drawable.outdent);
                        break;
                    case 2:
                        row.setId(R.id.action_align_left);
                        icon.setImageResource(R.drawable.justify_left);
                        break;
                    case 3:
                        row.setId(R.id.action_align_center);
                        icon.setImageResource(R.drawable.justify_center);
                        break;
                    case 4:
                        row.setId(R.id.action_align_right);
                        icon.setImageResource(R.drawable.justify_right);
                        break;
                    case 5:
                        row.setId(R.id.action_blockquote);
                        icon.setImageResource(R.drawable.blockquote);
                        break;
                    case 6:
                        row.setId(R.id.action_pre);
                        icon.setImageResource(R.drawable.action_pre);
                        break;
                    case 7:
                        row.setId(R.id.action_code_html);
                        icon.setImageResource(R.drawable.code_html);
                        break;
                    case 8:
                        row.setId(R.id.action_code_off_html);
                        icon.setImageResource(R.drawable.code_off_html);
                        break;
                }
                break;
            case R.id.action_insert:
                switch (position) {
                    case 0:
                        row.setId(R.id.action_insert_checkbox);
                        icon.setImageResource(R.drawable.checkbox);
                        break;
                    case 1:
                        row.setId(R.id.action_insert_image);
                        icon.setImageResource(R.drawable.insert_image);
                        break;
                    case 2:
                        row.setId(R.id.action_insert_audio);
                        icon.setImageResource(R.drawable.insert_audio);
                        break;
                    case 3:
                        row.setId(R.id.action_insert_video);
                        icon.setImageResource(R.drawable.insert_video);
                        break;
                    case 4:
                        row.setId(R.id.action_insert_youtube);
                        icon.setImageResource(R.drawable.insert_youtube);
                        break;
                    case 5:
                        row.setId(R.id.action_insert_link);
                        icon.setImageResource(R.drawable.insert_link);
                        break;
                    case 6:
                        row.setId(R.id.action_insert_star);
                        icon.setImageResource(R.drawable.star);
                        break;
                    case 7:
                        row.setId(R.id.action_insert_exclamation);
                        icon.setImageResource(R.drawable.exclamation);
                        break;
                    case 8:
                        row.setId(R.id.action_insert_question);
                        icon.setImageResource(R.drawable.question);
                        break;
                    case 9:
                        row.setId(R.id.action_insert_hline);
                        icon.setImageResource(R.drawable.hline);
                        break;
                    case 10:
                        row.setId(R.id.action_insert_section);
                        icon.setImageResource(R.drawable.insert_section);
                        break;
                    case 11:
                        row.setId(R.id.action_insert_datetime);
                        icon.setImageResource(R.drawable.insert_datetime);
                        break;
                    case 12:
                        row.setId(R.id.action_insert_hashtag);
                        icon.setImageResource(R.drawable.insert_hashtag);
                        break;
                }
                break;
            case R.id.action_table:
                switch (position) {
                    case 0:
                        row.setId(R.id.action_insert_table);
                        icon.setImageResource(R.drawable.insert_table_2x2);
                        break;
                    case 1:
                        row.setId(R.id.action_insert_column);
                        icon.setImageResource(R.drawable.insert_column);
                        break;
                    case 2:
                        row.setId(R.id.action_insert_row);
                        icon.setImageResource(R.drawable.insert_row);
                        break;
                    case 3:
                        row.setId(R.id.action_delete_column);
                        icon.setImageResource(R.drawable.delete_column);
                        break;
                    case 4:
                        row.setId(R.id.action_delete_row);
                        icon.setImageResource(R.drawable.delete_row);
                        break;
                }
                break;
        }
        return row;
    }
}

